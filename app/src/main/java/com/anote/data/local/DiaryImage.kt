package com.anote.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "diary_images",
    foreignKeys = [
        ForeignKey(
            entity = DiaryEntry::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["entryId"])],
)
data class DiaryImage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entryId: Long,
    val relativePath: String,
    val sortOrder: Int,
)
