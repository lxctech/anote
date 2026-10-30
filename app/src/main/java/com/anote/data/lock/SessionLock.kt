package com.anote.data.lock

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class LockGateState(
    val ready: Boolean = false,
    val locked: Boolean = false,
    val biometricEnabled: Boolean = false,
)

class SessionLock(
    lockStore: LockStore,
    scope: CoroutineScope,
) {
    private val unlocked = MutableStateFlow(false)

    val state: StateFlow<LockGateState> = combine(lockStore.config, unlocked) { config, isUnlocked ->
        LockGateState(
            ready = true,
            locked = config.enabled && config.hasPin && !isUnlocked,
            biometricEnabled = config.enabled && config.biometricEnabled,
        )
    }.stateIn(
        scope,
        SharingStarted.Eagerly,
        LockGateState(ready = false, locked = true, biometricEnabled = false),
    )

    fun unlock() {
        unlocked.value = true
    }

    fun onHostStop(changingConfigurations: Boolean) {
        if (!changingConfigurations) {
            unlocked.value = false
        }
    }
}
