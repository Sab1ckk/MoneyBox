package com.example.kopilka.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kopilka.data.datastore.PinDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PinPhase { LOADING, CREATE, REENTER, UNLOCK }

data class PinUiState(
    val phase: PinPhase = PinPhase.LOADING,
    val pin: String = "",
    val error: String? = null,
    val shake: Boolean = false,
    val isLockedOut: Boolean = false,
    val lockRemainingSeconds: Int = 0,
    val isUnlocked: Boolean = false
)

@HiltViewModel
class PinViewModel @Inject constructor(
    private val pinDataStore: PinDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(PinUiState())
    val uiState: StateFlow<PinUiState> = _uiState.asStateFlow()

    // Временно хранит первый ввод при создании PIN, до подтверждения повтором.
    // В памяти, никуда не пишется, пока re-enter не совпадёт.
    private var pendingNewPin: String = ""
    private var lockTickerJob: Job? = null

    init {
        viewModelScope.launch {
            val pinAlreadyExists = pinDataStore.isPinSet.first()
            _uiState.value = _uiState.value.copy(
                phase = if (pinAlreadyExists) PinPhase.UNLOCK else PinPhase.CREATE
            )
            resumeLockIfNeeded()
        }
    }

    fun onDigit(digit: String) {
        val state = _uiState.value
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

    fun onForgotPin() {
        viewModelScope.launch {
            pinDataStore.clearPin()
            pendingNewPin = ""
            lockTickerJob?.cancel()
            _uiState.value = PinUiState(phase = PinPhase.CREATE)
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
                    pinDataStore.savePin(enteredPin)
                    _uiState.value = _uiState.value.copy(isUnlocked = true)
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

            PinPhase.LOADING -> Unit
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
                    error = "Слишком много попыток. Повторите через ${remainingSec} с"
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
