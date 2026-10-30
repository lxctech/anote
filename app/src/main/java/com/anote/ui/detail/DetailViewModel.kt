package com.anote.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anote.data.DiaryRepository
import com.anote.data.local.DiaryEntryWithImages
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DetailState {
    data object Loading : DetailState
    data object Missing : DetailState
    data class Ready(val item: DiaryEntryWithImages) : DetailState
}

class DetailViewModel(
    private val repository: DiaryRepository,
    private val entryId: Long,
) : ViewModel() {
    val state: StateFlow<DetailState> = repository.observeById(entryId)
        .map { item -> if (item == null) DetailState.Missing else DetailState.Ready(item) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailState.Loading)

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.delete(entryId)
            onDone()
        }
    }
}
