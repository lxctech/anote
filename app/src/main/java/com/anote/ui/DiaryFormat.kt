package com.anote.ui

import com.anote.R
import com.anote.data.local.DiaryEntry
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val headerFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA)

fun LocalDate.diaryHeader(today: LocalDate = LocalDate.now()): String {
    val formatted = format(headerFormatter)
    val prefix = when (this) {
        today -> "今天"
        today.minusDays(1) -> "昨天"
        else -> null
    }
    val year = if (year == today.year) "" else "${year}年"
    return when {
        prefix != null -> "$prefix · $year$formatted"
        year.isEmpty() -> formatted
        else -> "$year$formatted"
    }
}

fun DiaryEntry.displayTitle(untitled: String): String {
    val trimmed = title.trim()
    if (trimmed.isNotEmpty()) return trimmed
    val body = content.trim().replace(Regex("\\s+"), " ")
    if (body.isEmpty()) return untitled
    return if (body.length <= 28) body else body.take(28) + "…"
}

fun DiaryEntry.previewText(): String =
    content.trim().replace(Regex("\\s+"), " ")

fun monthCells(month: java.time.YearMonth): List<LocalDate?> {
    val first = month.atDay(1)
    val leading = first.dayOfWeek.value - java.time.DayOfWeek.MONDAY.value
    val cells = MutableList<LocalDate?>(leading) { null }
    for (day in 1..month.lengthOfMonth()) {
        cells += month.atDay(day)
    }
    while (cells.size % 7 != 0) {
        cells += null
    }
    return cells
}

val weekdayLabels = listOf(
    R.string.weekday_mon,
    R.string.weekday_tue,
    R.string.weekday_wed,
    R.string.weekday_thu,
    R.string.weekday_fri,
    R.string.weekday_sat,
    R.string.weekday_sun,
)
