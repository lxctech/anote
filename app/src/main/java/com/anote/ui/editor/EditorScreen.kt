package com.anote.ui.editor

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.anote.R
import com.anote.model.Mood
import com.anote.ui.anoteViewModel
import com.anote.ui.components.DiaryPhoto
import com.anote.ui.diaryHeader
import com.anote.ui.theme.anoteTopBarColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    entryId: Long?,
    dateEpoch: Long,
    onDone: () -> Unit,
) {
    val initialDate = remember(dateEpoch) {
        if (dateEpoch >= 0) LocalDate.ofEpochDay(dateEpoch) else LocalDate.now()
    }
    val viewModel = anoteViewModel(key = "editor-${entryId ?: "new"}-$dateEpoch") {
        EditorViewModel(it.repository, it.imageStore, entryId, initialDate)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showDiscard by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val importError = stringResource(R.string.error_import_image)
    val saveError = stringResource(R.string.error_save)
    val missingError = stringResource(R.string.error_missing)
    val limitError = stringResource(R.string.image_limit)

    val multiplePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(10),
    ) { uris -> viewModel.addImages(uris) }
    val singlePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) viewModel.addImages(listOf(uri)) }

    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }
    LaunchedEffect(state.error) {
        val message = when (state.error) {
            EditorError.ImportFailed -> importError
            EditorError.SaveFailed -> saveError
            EditorError.Missing -> missingError
            EditorError.ImageLimit -> limitError
            null -> null
        } ?: return@LaunchedEffect
        snackbar.showSnackbar(message)
        viewModel.consumeError()
    }

    val requestBack = {
        if (viewModel.isDirty()) showDiscard = true else onDone()
    }
    BackHandler(onBack = requestBack)

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                colors = anoteTopBarColors(),
                title = {
                    Text(stringResource(if (entryId == null) R.string.editor_new else R.string.editor_edit))
                },
                navigationIcon = {
                    IconButton(onClick = requestBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::save, enabled = state.canSave) {
                        if (state.saving) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.action_save))
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        if (state.missing) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.detail_missing))
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(onClick = { showDatePicker = true }) {
                Text("${stringResource(R.string.label_date)} · ${state.diaryDate.diaryHeader()}")
            }
            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::setTitle,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.label_title)) },
            )
            Text(stringResource(R.string.label_mood), style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Mood.entries, key = { it.name }) { mood ->
                    FilterChip(
                        selected = state.mood == mood,
                        onClick = { viewModel.setMood(if (state.mood == mood) null else mood) },
                        label = { Text("${mood.symbol} ${stringResource(mood.labelRes)}") },
                    )
                }
            }
            OutlinedTextField(
                value = state.content,
                onValueChange = viewModel::setContent,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp),
                label = { Text(stringResource(R.string.label_content)) },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily.Serif,
                    fontSize = 18.sp,
                    lineHeight = 28.sp,
                ),
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                items(state.visibleExisting, key = { "e-${it.id}" }) { image ->
                    Box {
                        DiaryPhoto(image = image, size = 96.dp)
                        IconButton(
                            onClick = { viewModel.removeExisting(image.id) },
                            modifier = Modifier.align(Alignment.TopEnd),
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_remove_image))
                        }
                    }
                }
                items(state.pending, key = { it.id }) { image ->
                    Box {
                        AsyncImage(
                            model = image.file,
                            contentDescription = stringResource(R.string.cd_diary_image),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(96.dp)
                                .clip(RoundedCornerShape(12.dp)),
                        )
                        IconButton(
                            onClick = { viewModel.removePending(image.id) },
                            modifier = Modifier.align(Alignment.TopEnd),
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_remove_image))
                        }
                    }
                }
                item {
                    OutlinedButton(
                        onClick = {
                            val room = 20 - state.visibleExisting.size - state.pending.size
                            val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            when {
                                room <= 0 -> viewModel.notifyImageLimit()
                                room == 1 -> singlePicker.launch(request)
                                else -> multiplePicker.launch(request)
                            }
                        },
                        modifier = Modifier.height(96.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Filled.AddAPhoto, contentDescription = stringResource(R.string.cd_add_image))
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.diaryDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        viewModel.setDate(
                            Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate(),
                        )
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showDiscard) {
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            title = { Text(stringResource(R.string.discard_title)) },
            text = { Text(stringResource(R.string.discard_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showDiscard = false
                    onDone()
                }) { Text(stringResource(R.string.action_discard)) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscard = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}
