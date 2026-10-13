package com.example.kopilka.utils

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Биометрия реализована как БЫСТРЫЙ АЛЬТЕРНАТИВНЫЙ способ входа — она не
 * заменяет PIN и не защищает его криптографически, а лишь даёт более
 * короткий путь к тому же результату (разблокировке). PIN остаётся
 * основным механизмом и единственным источником правды (см. PinDataStore).
 */
sealed class BiometricAuthResult {
    object Success : BiometricAuthResult()

    /** Пользователь сам отменил ввод (нажал "Ввести PIN" или системную кнопку "Назад"). */
    object Cancelled : BiometricAuthResult()

    /** Системная ошибка — например, аппаратная неполадка сенсора. */
    data class Error(val message: String) : BiometricAuthResult()
}

object BiometricAuthenticator {

    private const val AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_STRONG

    /**
     * true — на устройстве есть биометрический сенсор И уже зарегистрирован
     * хотя бы один отпечаток/лицо. Если сенсора нет или ничего не
     * зарегистрировано — false, и кнопку/тумблер нужно скрывать.
     */
    fun isAvailable(context: Context): Boolean {
        val manager = BiometricManager.from(context)
        return manager.canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Показывает системный диалог биометрии. BiometricPrompt требует именно
     * FragmentActivity (не обычный ComponentActivity) — поэтому MainActivity
     * унаследован от FragmentActivity.
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Вход по отпечатку",
        subtitle: String = "Подтвердите личность, чтобы открыть Копилку",
        negativeButtonText: String = "Ввести PIN",
        onResult: (BiometricAuthResult) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onResult(BiometricAuthResult.Success)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                val userCancelled = errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_CANCELED
                onResult(
                    if (userCancelled) BiometricAuthResult.Cancelled
                    else BiometricAuthResult.Error(errString.toString())
                )
            }

            override fun onAuthenticationFailed() {
                // Отпечаток/лицо не распознаны — системный диалог сам даёт
                // повторить попытку, здесь ничего делать не нужно.
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(AUTHENTICATORS)
            .build()

        prompt.authenticate(promptInfo)
    }
}
