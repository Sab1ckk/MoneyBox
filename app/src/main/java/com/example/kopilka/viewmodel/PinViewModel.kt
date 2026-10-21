package com.example.kopilka.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kopilka.data.datastore.PinDataStore
import com.example.kopilka.data.datastore.SettingsDataStore
import com.example.kopilka.utils.BiometricAuthenticator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PinPhase { LOADING, CREATE, REENTER, PHRASE_CREATE, UNLOCK, RECOVER }

data class PinUiState(
    val phase: PinPhase = PinPhase.LOADING,
    val phrase: String = "",
    val pin: String = "",
    val error: String? = null,
    val shake: Boolean = false,
    val isLockedOut: Boolean = false,
    val lockRemainingSeconds: Int = 0,
    val isUnlocked: Boolean = false,
    val biometricAvailable: Boolean = false
)

@HiltViewModel
class PinViewModel @Inject constructor(
    private val pinDataStore: PinDataStore,
    private val settingsDataStore: SettingsDataStore,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(PinUiState())
    val uiState: StateFlow<PinUiState> = _uiState.asStateFlow()

    // Временно хранит первый ввод при создании PIN, до подтверждения повтором.
    // В памяти, никуда не пишется, пока re-enter не совпадёт.
    private var pendingNewPin: String = ""
    private var lockTickerJob: Job? = null
    private val MIN_PHRASE_LENGTH = 4

    init {
        viewModelScope.launch {
            val pinAlreadyExists = pinDataStore.isPinSet.first()
            _uiState.value = _uiState.value.copy(
                phase = if (pinAlreadyExists) PinPhase.UNLOCK else PinPhase.CREATE
            )
            resumeLockIfNeeded()
        }

        viewModelScope.launch {
            settingsDataStore.biometricEnabled.collect { enabled ->
                val available = enabled && BiometricAuthenticator.isAvailable(appContext)
                _uiState.value = _uiState.value.copy(biometricAvailable = available)
            }

        }
    }

    fun onDigit(digit: String) {
        val state = _uiState.value
        if (state.phase != PinPhase.CREATE && state.phase != PinPhase.REENTER && state.phase != PinPhase.UNLOCK) return
        if (state.isLockedOut || state.pin.length >= 4) return

        val next = state.pin + digit
        _uiState.value = state.copy(pin = next, error = null)

        if (next.length == 4) {
            viewModelScope.launch { submit(next) }
        }
    }

    fun onBackspace() {
        val state = _uiState.value
        if (state.isLockedOut) return
        _uiState.value = state.copy(pin = state.pin.dropLast(1), error = null)
    }

    /** Вызывается UI после проигрывания анимации тряски. */
    fun consumeShake() {
        _uiState.value = _uiState.value.copy(shake = false)
    }

    /** Вызывается после успешного системного диалога BiometricPrompt. */
    fun onBiometricSuccess() {
        // Биометрия — это только альтернативный вход на экране разблокировки.
        // На CREATE/REENTER (первичная настройка PIN) кнопка и так скрыта,
        // но проверка фазы здесь — дополнительная защита от гонок состояний.
        if (_uiState.value.phase == PinPhase.UNLOCK && !_uiState.value.isLockedOut) {
            _uiState.value = _uiState.value.copy(isUnlocked = true)
        }
    }

    /** Системная ошибка биометрии (не "не распознано" — именно сбой). */
    fun onBiometricError(message: String) {
        _uiState.value = _uiState.value.copy(error = message)
    }

    fun onForgotPin() {
        _uiState.value = _uiState.value.copy(phase = PinPhase.RECOVER, pin = "", phrase = "", error = null)
    }

    fun onBackToUnlock() {
        _uiState.value = _uiState.value.copy(phase = PinPhase.UNLOCK, phrase = "", error = null)
    }

    fun onPhraseChange(value: String) {
        _uiState.value = _uiState.value.copy(phrase = value, error = null)
    }

    fun onPhraseSubmit() {
        val state = _uiState.value
        if (state.isLockedOut) return
        val phrase = state.phrase.trim()

        viewModelScope.launch {
            when (state.phase) {
                PinPhase.PHRASE_CREATE -> {
                    if (phrase.length < MIN_PHRASE_LENGTH) {
                        _uiState.value = state.copy(
                            error = "Минимум $MIN_PHRASE_LENGTH символа",
                            shake = true
                        )
                        return@launch
                    }
                    pinDataStore.savePinWithPhrase(pendingNewPin, phrase)
                    pendingNewPin = ""
                    _uiState.value = _uiState.value.copy(phrase = "", isUnlocked = true)
                }

                PinPhase.RECOVER -> {
                    if (pinDataStore.verifyPhrase(phrase)) {
                        pinDataStore.clearPin()
                        pendingNewPin = ""
                        lockTickerJob?.cancel()
                        // Дальше пользователь создаёт новый PIN и новую фразу
                        _uiState.value = PinUiState(
                            phase = PinPhase.CREATE,
                            biometricAvailable = state.biometricAvailable
                        )
                    } else {
                        val lockedUntil = pinDataStore.lockedUntil.first()
                        if (lockedUntil > System.currentTimeMillis()) {
                            _uiState.value = _uiState.value.copy(shake = true)
                            startLockCountdown(lockedUntil)
                        } else {
                            _uiState.value = _uiState.value.copy(
                                error = "Неверная контрольная фраза",
                                shake = true
                            )
                        }
                    }
                }

                else -> Unit
            }
        }
    }


    private suspend fun submit(enteredPin: String) {
        when (_uiState.value.phase) {
            PinPhase.CREATE -> {
                pendingNewPin = enteredPin
                _uiState.value = _uiState.value.copy(pin = "", phase = PinPhase.REENTER)
            }

            PinPhase.REENTER -> {
                if (enteredPin == pendingNewPin) {
                    _uiState.value = _uiState.value.copy(pin = "", phrase = "", phase = PinPhase.PHRASE_CREATE)
                } else {
                    pendingNewPin = ""
                    _uiState.value = _uiState.value.copy(
                        pin = "",
                        phase = PinPhase.CREATE,
                        error = "PIN-коды не совпадают — попробуйте снова",
                        shake = true
                    )
                }
            }

            PinPhase.UNLOCK -> {
                val isValid = pinDataStore.verifyPin(enteredPin)
                if (isValid) {
                    _uiState.value = _uiState.value.copy(isUnlocked = true)
                } else {
                    val lockedUntil = pinDataStore.lockedUntil.first()
                    if (lockedUntil > System.currentTimeMillis()) {
                        _uiState.value = _uiState.value.copy(pin = "", shake = true)
                        startLockCountdown(lockedUntil)
                    } else {
                        _uiState.value = _uiState.value.copy(
                            pin = "",
                            error = "Неверный PIN",
                            shake = true
                        )
                    }
                }
            }

            PinPhase.PHRASE_CREATE, PinPhase.RECOVER, PinPhase.LOADING -> Unit
        }
    }

    private suspend fun resumeLockIfNeeded() {
        val lockedUntil = pinDataStore.lockedUntil.first()
        if (lockedUntil > System.currentTimeMillis()) {
            startLockCountdown(lockedUntil)
        }
    }

    private fun startLockCountdown(lockedUntil: Long) {
        lockTickerJob?.cancel()
        lockTickerJob = viewModelScope.launch {
            while (true) {
                val remainingMs = lockedUntil - System.currentTimeMillis()
                if (remainingMs <= 0) {
                    _uiState.value = _uiState.value.copy(
                        isLockedOut = false,
                        lockRemainingSeconds = 0,
                        error = null
                    )
                    break
                }
                val remainingSec = (remainingMs / 1000).toInt() + 1
                _uiState.value = _uiState.value.copy(
                    isLockedOut = true,
                    lockRemainingSeconds = remainingSec,
                    error = "Слишком много попыток. Повторите через ${remainingSec}с"
                )
                delay(1000)
            }
        }
    }

    override fun onCleared() {
        lockTickerJob?.cancel()
        super.onCleared()
    }
}
