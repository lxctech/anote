package com.anote.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryDao {
    @Transaction
    @Query(
        """
        SELECT * FROM diary_entries
        WHERE (:moodName IS NULL OR mood = :moodName)
          AND (
            :query = ''
            OR title LIKE '%' || :query || '%'
            OR content LIKE '%' || :query || '%'
          )
        ORDER BY diaryDate DESC, createdAt DESC
        """,
    )
    fun observeEntries(query: String, moodName: String?): Flow<List<DiaryEntryWithImages>>

    @Transaction
    @Query("SELECT * FROM diary_entries WHERE id = :id")
    fun observeById(id: Long): Flow<DiaryEntryWithImages?>

    @Transaction
    @Query("SELECT * FROM diary_entries WHERE id = :id")
    suspend fun getById(id: Long): DiaryEntryWithImages?

    @Transaction
    @Query(
        """
        SELECT * FROM diary_entries
        WHERE diaryDate = :day
        ORDER BY createdAt DESC
        """,
    )
    fun observeByDate(day: Long): Flow<List<DiaryEntryWithImages>>

    @Query(
        """
        SELECT DISTINCT diaryDate FROM diary_entries
        WHERE diaryDate BETWEEN :start AND :end
        """,
    )
    fun observeDates(start: Long, end: Long): Flow<List<Long>>

    @Insert
    suspend fun insert(entry: DiaryEntry): Long

    @Update
    suspend fun update(entry: DiaryEntry)

    @Query("DELETE FROM diary_entries WHERE id = :id")
    suspend fun deleteEntry(id: Long)

    @Insert
    suspend fun insertImage(image: DiaryImage): Long

    @Query("SELECT * FROM diary_images WHERE entryId = :entryId")
    suspend fun imagesForEntry(entryId: Long): List<DiaryImage>

    @Query("SELECT * FROM diary_images WHERE id IN (:ids)")
    suspend fun imagesByIds(ids: List<Long>): List<DiaryImage>

    @Query("DELETE FROM diary_images WHERE id IN (:ids)")
    suspend fun deleteImages(ids: List<Long>)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM diary_images WHERE entryId = :entryId")
    suspend fun maxSortOrder(entryId: Long): Int
}
