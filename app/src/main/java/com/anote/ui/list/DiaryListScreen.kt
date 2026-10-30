package com.anote.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anote.R
import com.anote.data.local.DiaryEntryWithImages
import com.anote.model.Mood
import com.anote.model.moodOf
import com.anote.ui.anoteViewModel
import com.anote.ui.components.PhotoStrip
import com.anote.ui.diaryHeader
import com.anote.ui.displayTitle
import com.anote.ui.previewText
import com.anote.ui.theme.anoteTopBarColors
import java.time.LocalDate

private sealed interface DiaryRow {
    data class Header(val date: LocalDate) : DiaryRow
    data class Entry(val item: DiaryEntryWithImages) : DiaryRow
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryListScreen(
    onOpen: (Long) -> Unit,
    onCreate: () -> Unit,
) {
    val viewModel = anoteViewModel { DiaryListViewModel(it.repository) }
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val query by viewModel.queryText.collectAsStateWithLifecycle()
    val mood by viewModel.selectedMood.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<DiaryEntryWithImages?>(null) }
    val untitled = stringResource(R.string.untitled)
    val rows = remember(entries) {
        buildList {
            entries.groupBy { LocalDate.ofEpochDay(it.entry.diaryDate) }.forEach { (date, items) ->
                add(DiaryRow.Header(date))
                items.forEach { add(DiaryRow.Entry(it)) }
            }
        }
    }
    val filtering = query.isNotBlank() || mood != null

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_diary)) },
                colors = anoteTopBarColors(),
                windowInsets = WindowInsets(0),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreate) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.cd_new_entry))
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                singleLine = true,
                label = { Text(stringResource(R.string.label_search)) },
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = mood == null,
                        onClick = { viewModel.setMood(null) },
                        label = { Text(stringResource(R.string.mood_all)) },
                    )
                }
                items(Mood.entries, key = { it.name }) { item ->
                    FilterChip(
                        selected = mood == item,
                        onClick = { viewModel.setMood(if (mood == item) null else item) },
                        label = { Text("${item.symbol} ${stringResource(item.labelRes)}") },
                    )
                }
            }
            if (rows.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(if (filtering) R.string.empty_search else R.string.empty_diary),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (!filtering) {
                            Text(
                                text = stringResource(R.string.empty_diary_hint),
                                modifier = Modifier.padding(top = 8.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(rows, key = { row ->
                        when (row) {
                            is DiaryRow.Header -> "h-${row.date.toEpochDay()}"
                            is DiaryRow.Entry -> "e-${row.item.entry.id}"
                        }
                    }) { row ->
                        when (row) {
                            is DiaryRow.Header -> Text(
                                text = row.date.diaryHeader(),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                            is DiaryRow.Entry -> DiaryCard(
                                item = row.item,
                                title = row.item.entry.displayTitle(untitled),
                                onClick = { onOpen(row.item.entry.id) },
                                onDelete = { pendingDelete = row.item },
                            )
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete_title)) },
            text = { Text(stringResource(R.string.delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(item)
                    pendingDelete = null
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun DiaryCard(
    item: DiaryEntryWithImages,
    title: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val mood = moodOf(item.entry.mood)
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.cd_delete))
                }
            }
            if (mood != null) {
                Text(
                    text = "${mood.symbol} ${stringResource(mood.labelRes)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val preview = item.entry.previewText()
            if (preview.isNotEmpty() && preview != title) {
                Text(
                    text = preview,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            PhotoStrip(item.orderedImages)
        }
    }
}
