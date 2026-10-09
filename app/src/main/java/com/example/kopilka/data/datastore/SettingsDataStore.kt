// Путь в проекте: app/src/main/java/com/example/kopilka/data/datastore/SettingsDataStore.kt

package com.example.kopilka.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import androidx.datastore.preferences.core.stringSetPreferencesKey

private val Context.settingsDataStore by preferencesDataStore(name = "app_settings")
enum class DefaultSortSetting { DEADLINE, PROGRESS, AMOUNT }
enum class ThemeModeSetting { LIGHT, DARK, SYSTEM }

/**
 * Настройки приложения (экран SettingsScreen): сортировка по умолчанию,
 * валюта, уведомления, биометрическая разблокировка. Тема пока не хранится
 * здесь — единственная доступная тема тёмная (см. Kopilka_Figma_Design_Prompt.md).
 */
@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        val DEADLINE_ALERTS = booleanPreferencesKey("deadline_alerts_enabled")
        val WEEKLY_REPORT = booleanPreferencesKey("weekly_report_enabled")
        val BIOMETRIC = booleanPreferencesKey("biometric_enabled")
        val DEFAULT_SORT = stringPreferencesKey("default_sort")
        val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        val SENT_DEADLINE = stringSetPreferencesKey("sent_deadline_notifications")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val notificationsEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { it[Keys.NOTIFICATIONS] ?: true }

    val deadlineAlertsEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { it[Keys.DEADLINE_ALERTS] ?: true }

    val weeklyReportEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { it[Keys.WEEKLY_REPORT] ?: false }

    val biometricEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { it[Keys.BIOMETRIC] ?: false }

    val defaultSort: Flow<DefaultSortSetting> = context.settingsDataStore.data.map { prefs ->
        val raw = prefs[Keys.DEFAULT_SORT]
        runCatching { DefaultSortSetting.valueOf(raw ?: DefaultSortSetting.DEADLINE.name) }
            .getOrDefault(DefaultSortSetting.DEADLINE)
    }

    val currencySymbol: Flow<String> =
        context.settingsDataStore.data.map { it[Keys.CURRENCY_SYMBOL] ?: "₽" }

    val sentDeadlineNotifications: Flow<Set<String>> =
        context.settingsDataStore.data.map { it[Keys.SENT_DEADLINE] ?: emptySet() }

    val themeMode: Flow<ThemeModeSetting> = context.settingsDataStore.data.map { prefs ->
        val raw = prefs[Keys.THEME_MODE]
        runCatching { ThemeModeSetting.valueOf(raw ?: ThemeModeSetting.DARK.name) }
            .getOrDefault(ThemeModeSetting.DARK)
    }
    suspend fun setNotificationsEnabled(value: Boolean) {
        context.settingsDataStore.edit { it[Keys.NOTIFICATIONS] = value }
    }

    suspend fun setDeadlineAlertsEnabled(value: Boolean) {
        context.settingsDataStore.edit { it[Keys.DEADLINE_ALERTS] = value }
    }

    suspend fun setWeeklyReportEnabled(value: Boolean) {
        context.settingsDataStore.edit { it[Keys.WEEKLY_REPORT] = value }
    }

    suspend fun setBiometricEnabled(value: Boolean) {
        context.settingsDataStore.edit { it[Keys.BIOMETRIC] = value }
    }

    suspend fun setDefaultSort(value: DefaultSortSetting) {
        context.settingsDataStore.edit { it[Keys.DEFAULT_SORT] = value.name }
    }

    suspend fun setCurrencySymbol(value: String) {
        context.settingsDataStore.edit { it[Keys.CURRENCY_SYMBOL] = value }
    }

    suspend fun setSentDeadlineNotifications(value: Set<String>) {
        context.settingsDataStore.edit { it[Keys.SENT_DEADLINE] = value }
    }

    suspend fun setThemeMode(value: ThemeModeSetting) {
        context.settingsDataStore.edit { it[Keys.THEME_MODE] = value.name }
    }
}
