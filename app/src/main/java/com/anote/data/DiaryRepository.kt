package com.anote.data

import com.anote.data.local.DiaryDao
import com.anote.data.local.DiaryEntry
import com.anote.data.local.DiaryEntryWithImages
import com.anote.data.local.DiaryImage
import com.anote.model.Mood
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

class DiaryRepository(
    private val dao: DiaryDao,
    private val imageStore: ImageStore,
) {
    fun observeEntries(rawQuery: String, mood: Mood?): Flow<List<DiaryEntryWithImages>> =
        dao.observeEntries(sanitizeLike(rawQuery.trim()), mood?.name)

    fun observeById(id: Long): Flow<DiaryEntryWithImages?> = dao.observeById(id)

    fun observeByDate(date: LocalDate): Flow<List<DiaryEntryWithImages>> =
        dao.observeByDate(date.toEpochDay())

    fun observeDates(start: LocalDate, end: LocalDate): Flow<List<Long>> =
        dao.observeDates(start.toEpochDay(), end.toEpochDay())

    suspend fun getById(id: Long): DiaryEntryWithImages? = dao.getById(id)

    suspend fun save(
        entryId: Long?,
        title: String,
        content: String,
        mood: Mood?,
        diaryDate: LocalDate,
        removedImageIds: Set<Long>,
        pendingFiles: List<File>,
    ): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val id = if (entryId == null) {
            dao.insert(
                DiaryEntry(
                    title = title.trim(),
                    content = content.trim(),
                    mood = mood?.name,
                    diaryDate = diaryDate.toEpochDay(),
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        } else {
            val existing = dao.getById(entryId) ?: error("日记不存在")
            dao.update(
                existing.entry.copy(
                    title = title.trim(),
                    content = content.trim(),
                    mood = mood?.name,
                    diaryDate = diaryDate.toEpochDay(),
                    updatedAt = now,
                ),
            )
            entryId
        }

        if (removedImageIds.isNotEmpty()) {
            val removed = dao.imagesByIds(removedImageIds.toList())
            dao.deleteImages(removedImageIds.toList())
            removed.forEach { imageStore.deleteRelative(it.relativePath) }
        }

        val stored = pendingFiles.map { file -> imageStore.persist(file, id) }
        val startOrder = dao.maxSortOrder(id) + 1
        try {
            stored.forEachIndexed { index, relativePath ->
                dao.insertImage(
                    DiaryImage(
                        entryId = id,
                        relativePath = relativePath,
                        sortOrder = startOrder + index,
                    ),
                )
            }
        } catch (error: Throwable) {
            stored.forEach { imageStore.deleteRelative(it) }
            throw error
        }
        id
    }

    suspend fun delete(entryId: Long) = withContext(Dispatchers.IO) {
        val images = dao.imagesForEntry(entryId)
        dao.deleteEntry(entryId)
        images.forEach { imageStore.deleteRelative(it.relativePath) }
    }

    private fun sanitizeLike(raw: String): String =
        raw.replace("\\", "").replace("%", "").replace("_", "")
}
