package com.anote.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anote.data.lock.LockConfig
import com.anote.data.lock.LockStore
import com.anote.data.lock.SessionLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SettingsMessage {
    PinChanged,
}

class SettingsViewModel(
    private val lockStore: LockStore,
    private val sessionLock: SessionLock,
) : ViewModel() {
    val config = lockStore.config.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        LockConfig(),
    )

    private val _message = MutableStateFlow<SettingsMessage?>(null)
    val message = _message.asStateFlow()

    fun enableWithPin(pin: String, onDone: () -> Unit) {
        viewModelScope.launch {
            lockStore.setPin(pin)
            sessionLock.unlock()
            onDone()
        }
    }

    fun disable() {
        viewModelScope.launch { lockStore.clear() }
    }

    fun verifyCurrent(pin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(lockStore.verify(pin)) }
    }

    fun changePin(newPin: String, onDone: () -> Unit) {
        viewModelScope.launch {
            lockStore.setPin(newPin)
            sessionLock.unlock()
            _message.value = SettingsMessage.PinChanged
            onDone()
        }
    }

    fun setBiometric(enabled: Boolean) {
        viewModelScope.launch { lockStore.setBiometricEnabled(enabled) }
    }

    fun consumeMessage() {
        _message.value = null
    }
}
