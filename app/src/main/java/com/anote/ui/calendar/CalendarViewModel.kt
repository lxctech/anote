package com.anote.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anote.data.DiaryRepository
import com.anote.data.local.DiaryEntryWithImages
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth

data class CalendarUiState(
    val month: YearMonth = YearMonth.now(),
    val selected: LocalDate = LocalDate.now(),
    val marked: Set<Long> = emptySet(),
    val entries: List<DiaryEntryWithImages> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(
    repository: DiaryRepository,
) : ViewModel() {
    private val month = MutableStateFlow(YearMonth.now())
    private val selected = MutableStateFlow(LocalDate.now())

    private val marked = month.flatMapLatest { current ->
        repository.observeDates(current.atDay(1), current.atEndOfMonth())
    }
    private val entries = selected.flatMapLatest { repository.observeByDate(it) }

    val uiState = combine(month, selected, marked, entries) { currentMonth, day, dates, dayEntries ->
        CalendarUiState(
            month = currentMonth,
            selected = day,
            marked = dates.toSet(),
            entries = dayEntries,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState())

    fun shiftMonth(delta: Int) {
        val next = month.value.plusMonths(delta.toLong())
        month.value = next
        val day = selected.value.dayOfMonth.coerceAtMost(next.lengthOfMonth())
        selected.value = next.atDay(day)
    }

    fun select(date: LocalDate) {
        selected.value = date
        month.value = YearMonth.from(date)
    }
}
