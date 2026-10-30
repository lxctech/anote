package com.anote.ui.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anote.data.lock.LockStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class LockViewModel(
    private val lockStore: LockStore,
) : ViewModel() {
    private val _error = MutableStateFlow(false)
    val error = _error.asStateFlow()
    private val _checking = MutableStateFlow(false)
    val checking = _checking.asStateFlow()

    fun submit(pin: String, onSuccess: () -> Unit) {
        if (_checking.value || pin.length !in 4..6) return
        viewModelScope.launch {
            _checking.value = true
            try {
                val ok = lockStore.verify(pin)
                _error.value = !ok
                if (ok) onSuccess()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } finally {
                _checking.value = false
            }
        }
    }

    fun clearError() {
        _error.value = false
    }
}
