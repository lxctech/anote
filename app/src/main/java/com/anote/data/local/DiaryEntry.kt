package com.anote.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "diary_entries",
    indices = [Index(value = ["diaryDate"])],
)
data class DiaryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val mood: String?,
    val diaryDate: Long,
    val createdAt: Long,
    val updatedAt: Long,
)
