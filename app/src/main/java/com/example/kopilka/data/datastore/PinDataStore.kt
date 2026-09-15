// Путь в проекте: app/src/main/java/com/example/kopilka/data/datastore/PinDataStore.kt
//
// Требуемая зависимость (build.gradle.kts, модуль app):
// implementation("androidx.datastore:datastore-preferences:1.1.1")

package com.example.kopilka.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

private val Context.pinPrefsDataStore by preferencesDataStore(name = "pin_prefs")

/**
 * Хранит PIN не в открытом виде, а как SHA-256 хэш.
 * Также хранит счётчик неверных попыток и время окончания блокировки —
 * это реализует требование п.14 инструкции («после нескольких неверных
 * попыток — задержка»).
 */
@Singleton
class PinDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private object Keys {
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val FAILED_ATTEMPTS = intPreferencesKey("failed_attempts")
        val LOCKED_UNTIL = longPreferencesKey("locked_until")
    }

    /** true — PIN уже создан ранее, нужно показывать экран разблокировки. */
    val isPinSet: Flow<Boolean> = context.pinPrefsDataStore.data.map { prefs ->
        !prefs[Keys.PIN_HASH].isNullOrEmpty()
    }

    /** Временная метка (millis), до которой ввод PIN заблокирован. 0 — блокировки нет. */
    val lockedUntil: Flow<Long> = context.pinPrefsDataStore.data.map { prefs ->
        prefs[Keys.LOCKED_UNTIL] ?: 0L
    }

    suspend fun savePin(pin: String) {
        context.pinPrefsDataStore.edit { prefs ->
            prefs[Keys.PIN_HASH] = hash(pin)
            prefs[Keys.FAILED_ATTEMPTS] = 0
            prefs[Keys.LOCKED_UNTIL] = 0L
        }
    }

    /**
     * Проверяет PIN. При ошибке увеличивает счётчик неверных попыток
     * и, начиная с [LOCK_THRESHOLD] попыток, выставляет время блокировки.
     */
    suspend fun verifyPin(pin: String): Boolean {
        var isValid = false
        context.pinPrefsDataStore.edit { prefs ->
            val storedHash = prefs[Keys.PIN_HASH]
            isValid = storedHash != null && storedHash == hash(pin)

            if (isValid) {
                prefs[Keys.FAILED_ATTEMPTS] = 0
                prefs[Keys.LOCKED_UNTIL] = 0L
            } else {
                val attempts = (prefs[Keys.FAILED_ATTEMPTS] ?: 0) + 1
                prefs[Keys.FAILED_ATTEMPTS] = attempts
                if (attempts >= LOCK_THRESHOLD) {
                    val cycles = ((attempts - LOCK_THRESHOLD) / LOCK_THRESHOLD) + 1
                    prefs[Keys.LOCKED_UNTIL] = System.currentTimeMillis() + BASE_LOCK_MS * cycles
                }
            }
        }
        return isValid
    }

    /** Используется, например, кнопкой «Забыли PIN? Сбросить». */
    suspend fun clearPin() {
        context.pinPrefsDataStore.edit { it.clear() }
    }

    private fun hash(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val LOCK_THRESHOLD = 5
        const val BASE_LOCK_MS = 30_000L // 30 секунд за цикл превышения
    }
}
