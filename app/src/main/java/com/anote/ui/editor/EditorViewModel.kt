package com.anote.ui.editor

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anote.data.DiaryRepository
import com.anote.data.ImageStore
import com.anote.data.local.DiaryImage
import com.anote.model.Mood
import com.anote.model.moodOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

enum class EditorError {
    ImportFailed,
    SaveFailed,
    Missing,
    ImageLimit,
}

data class PendingImage(
    val id: String,
    val file: File,
)

data class EditorUiState(
    val loading: Boolean = false,
    val missing: Boolean = false,
    val title: String = "",
    val content: String = "",
    val mood: Mood? = null,
    val diaryDate: LocalDate = LocalDate.now(),
    val existingImages: List<DiaryImage> = emptyList(),
    val removedImageIds: Set<Long> = emptySet(),
    val pending: List<PendingImage> = emptyList(),
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: EditorError? = null,
) {
    val visibleExisting: List<DiaryImage>
        get() = existingImages.filter { it.id !in removedImageIds }

    val canSave: Boolean
        get() = !saving && !loading && !missing && (
            title.isNotBlank() ||
                content.isNotBlank() ||
                visibleExisting.isNotEmpty() ||
                pending.isNotEmpty()
            )
}

private data class Baseline(
    val title: String,
    val content: String,
    val mood: Mood?,
    val diaryDate: LocalDate,
    val imageIds: List<Long>,
)

class EditorViewModel(
    private val repository: DiaryRepository,
    private val imageStore: ImageStore,
    private val entryId: Long?,
    initialDate: LocalDate,
) : ViewModel() {
    private val _state = MutableStateFlow(EditorUiState(loading = entryId != null, diaryDate = initialDate))
    val state: StateFlow<EditorUiState> = _state.asStateFlow()
    private var baseline: Baseline? = null
    private var saved = false

    init {
        if (entryId == null) {
            baseline = Baseline("", "", null, initialDate, emptyList())
        } else {
            viewModelScope.launch {
                val item = repository.getById(entryId)
                if (item == null) {
                    _state.update { it.copy(loading = false, missing = true, error = EditorError.Missing) }
                    return@launch
                }
                val mood = moodOf(item.entry.mood)
                val date = LocalDate.ofEpochDay(item.entry.diaryDate)
                baseline = Baseline(
                    title = item.entry.title,
                    content = item.entry.content,
                    mood = mood,
                    diaryDate = date,
                    imageIds = item.orderedImages.map { it.id },
                )
                _state.update {
                    it.copy(
                        loading = false,
                        title = item.entry.title,
                        content = item.entry.content,
                        mood = mood,
                        diaryDate = date,
                        existingImages = item.orderedImages,
                    )
                }
            }
        }
    }

    fun isDirty(): Boolean {
        val base = baseline ?: return false
        val current = _state.value
        if (current.loading || current.missing) return false
        return current.title != base.title ||
            current.content != base.content ||
            current.mood != base.mood ||
            current.diaryDate != base.diaryDate ||
            current.removedImageIds.isNotEmpty() ||
            current.pending.isNotEmpty()
    }

    fun setTitle(value: String) = _state.update { it.copy(title = value) }
    fun setContent(value: String) = _state.update { it.copy(content = value) }
    fun setMood(value: Mood?) = _state.update { it.copy(mood = value) }
    fun setDate(value: LocalDate) = _state.update { it.copy(diaryDate = value) }
    fun consumeError() = _state.update { it.copy(error = null) }

    fun removeExisting(imageId: Long) {
        _state.update { it.copy(removedImageIds = it.removedImageIds + imageId) }
    }

    fun removePending(id: String) {
        val image = _state.value.pending.find { it.id == id } ?: return
        image.file.delete()
        _state.update { it.copy(pending = it.pending.filter { pending -> pending.id != id }) }
    }

    fun notifyImageLimit() {
        _state.update { it.copy(error = EditorError.ImageLimit) }
    }

    fun addImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val current = _state.value
        val room = MAX_IMAGES - current.visibleExisting.size - current.pending.size
        if (room <= 0) {
            notifyImageLimit()
            return
        }
        val accepted = uris.take(room)
        val truncated = accepted.size < uris.size
        viewModelScope.launch {
            val files = imageStore.importToPending(accepted)
            val error = when {
                files.size < accepted.size -> EditorError.ImportFailed
                truncated -> EditorError.ImageLimit
                else -> null
            }
            val pending = files.map { PendingImage(UUID.randomUUID().toString(), it) }
            _state.update { it.copy(pending = it.pending + pending, error = error ?: it.error) }
        }
    }

    fun save() {
        val snapshot = _state.value
        if (!snapshot.canSave) return
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            try {
                repository.save(
                    entryId = entryId,
                    title = snapshot.title,
                    content = snapshot.content,
                    mood = snapshot.mood,
                    diaryDate = snapshot.diaryDate,
                    removedImageIds = snapshot.removedImageIds,
                    pendingFiles = snapshot.pending.map { it.file },
                )
                saved = true
                _state.update { it.copy(saving = false, saved = true, pending = emptyList()) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                _state.update { it.copy(saving = false, error = EditorError.SaveFailed) }
            }
        }
    }

    override fun onCleared() {
        if (!saved) {
            _state.value.pending.forEach { it.file.delete() }
        }
        super.onCleared()
    }

    private companion object {
        const val MAX_IMAGES = 20
    }
}
