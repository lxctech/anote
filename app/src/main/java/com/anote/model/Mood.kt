package com.anote.model

import androidx.annotation.StringRes
import com.anote.R

enum class Mood(
    @param:StringRes val labelRes: Int,
    val symbol: String,
) {
    HAPPY(R.string.mood_happy, "\uD83D\uDE0A"),
    CALM(R.string.mood_calm, "\uD83D\uDE0C"),
    SAD(R.string.mood_sad, "\uD83D\uDE22"),
    ANGRY(R.string.mood_angry, "\uD83D\uDE20"),
    ANXIOUS(R.string.mood_anxious, "\uD83D\uDE30"),
    EXCITED(R.string.mood_excited, "\uD83E\uDD29"),
    TIRED(R.string.mood_tired, "\uD83D\uDE34"),
    GRATEFUL(R.string.mood_grateful, "\uD83D\uDE4F"),
}

fun moodOf(name: String?): Mood? =
    name?.let { value -> runCatching { Mood.valueOf(value) }.getOrNull() }
