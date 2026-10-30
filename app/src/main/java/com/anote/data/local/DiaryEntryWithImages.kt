package com.anote.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class DiaryEntryWithImages(
    @Embedded val entry: DiaryEntry,
    @Relation(parentColumn = "id", entityColumn = "entryId")
    val images: List<DiaryImage>,
) {
    val orderedImages: List<DiaryImage>
        get() = images.sortedBy { it.sortOrder }
}
