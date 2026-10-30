package com.anote.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anote.data.DiaryRepository
import com.anote.data.local.DiaryEntryWithImages
import com.anote.model.Mood
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class DiaryListViewModel(
    private val repository: DiaryRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val mood = MutableStateFlow<Mood?>(null)

    val queryText = query.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val selectedMood = mood.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val entries = combine(query, mood) { rawQuery, selected -> rawQuery to selected }
        .flatMapLatest { (rawQuery, selected) -> repository.observeEntries(rawQuery, selected) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(value: String) {
        query.value = value
    }

    fun setMood(value: Mood?) {
        mood.value = value
    }

    fun delete(item: DiaryEntryWithImages) {
        viewModelScope.launch { repository.delete(item.entry.id) }
    }
}
