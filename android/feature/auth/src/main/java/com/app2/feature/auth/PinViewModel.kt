package com.app2.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PinViewState {
    data object EnterPin : PinViewState()
    data object ConfirmPin : PinViewState()
    data class Locked(val remainingMs: Long) : PinViewState()
    data object Success : PinViewState()
    data class Error(val message: String) : PinViewState()
}

@HiltViewModel
class PinViewModel @Inject constructor(
    private val pinManager: PinManager
) : ViewModel() {

    private val _state = MutableStateFlow<PinViewState>(PinViewState.EnterPin)
    val state = _state.asStateFlow()

    private var firstPin = ""

    init {
        if (pinManager.isLocked()) {
            startLockoutTimer()
        }
    }

    fun onPinEntered(pin: String) {
        val currentState = _state.value
        when (currentState) {
            is PinViewState.EnterPin -> {
                if (pinManager.isLocked()) {
                    startLockoutTimer()
                    return
                }
                if (pinManager.verifyPin(pin)) {
                    pinManager.resetAttempts()
                    _state.value = PinViewState.Success
                } else {
                    pinManager.registerFailedAttempt()
                    if (pinManager.isLocked()) {
                        startLockoutTimer()
                    } else {
                        val remaining = pinManager.getRemainingAttempts()
                        _state.value = PinViewState.Error("Code incorrect. $remaining tentative(s) restante(s).")
                    }
                }
            }
            is PinViewState.ConfirmPin -> {
                if (pin == firstPin) {
                    pinManager.setPin(pin)
                    _state.value = PinViewState.Success
                } else {
                    _state.value = PinViewState.Error("Les codes ne correspondent pas.")
                }
            }
            else -> {}
        }
    }

    fun setupNewPin(pin: String) {
        if (pin.length < 4) {
            _state.value = PinViewState.Error("Le code doit contenir au moins 4 chiffres.")
            return
        }
        firstPin = pin
        _state.value = PinViewState.ConfirmPin
    }

    fun clearError() {
        val current = _state.value
        if (current is PinViewState.Error) {
            _state.value = when {
                firstPin.isNotEmpty() -> PinViewState.ConfirmPin
                else -> PinViewState.EnterPin
            }
        }
    }

    private fun startLockoutTimer() {
        val remaining = pinManager.getLockoutRemainingMs()
        _state.value = PinViewState.Locked(remaining)
        viewModelScope.launch {
            var ms = remaining
            while (ms > 0) {
                delay(1000)
                ms = pinManager.getLockoutRemainingMs()
                _state.value = PinViewState.Locked(ms)
            }
            if (!pinManager.isLocked()) {
                _state.value = PinViewState.EnterPin
            }
        }
    }
}
