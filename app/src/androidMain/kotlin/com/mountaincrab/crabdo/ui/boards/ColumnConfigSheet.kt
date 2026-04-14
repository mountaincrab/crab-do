package com.mountaincrab.crabdo.ui.boards

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mountaincrab.crabdo.data.local.entity.ColumnEntity
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColumnConfigSheet(
    columns: List<ColumnEntity>,
    onDismiss: () -> Unit,
    onRename: (ColumnEntity, String) -> Unit,
    onDelete: (String) -> Unit,
    onReorder: (List<String>) -> Unit
) {
    var orderedColumns by remember(columns) { mutableStateOf(columns.toList()) }
    var showDeleteConfirm by remember { mutableStateOf<ColumnEntity?>(null) }

    val lazyListState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        orderedColumns = orderedColumns.toMutableList().apply { add(to.index, removeAt(from.index)) }
        onReorder(orderedColumns.map { it.id })
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                text = "Configure Columns",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            HorizontalDivider()
            LazyColumn(state = lazyListState) {
                items(orderedColumns, key = { it.id }) { column ->
                    ReorderableItem(reorderState, key = column.id) {
                        ColumnConfigRow(
                            column = column,
                            dragHandleModifier = Modifier.longPressDraggableHandle(),
                            onRename = { onRename(column, it) },
                            onDelete = { showDeleteConfirm = column }
                        )
                    }
                }
            }
        }
    }

    showDeleteConfirm?.let { col ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Delete Column") },
            text = { Text("Delete \"${col.title}\"? Tasks in this column will be lost.") },
            confirmButton = {
                TextButton(onClick = { onDelete(col.id); showDeleteConfirm = null }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ColumnConfigRow(
    column: ColumnEntity,
    dragHandleModifier: Modifier,
    onRename: (String) -> Unit,
    onDelete: () -> Unit
) {
    var showRename by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf(column.title) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.DragHandle,
            contentDescription = "Drag to reorder",
            modifier = dragHandleModifier,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = column.title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = { renameText = column.title; showRename = true }) {
            Text("Rename")
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete column",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }

    if (showRename) {
        AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text("Rename Column") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (renameText.isNotBlank()) onRename(renameText.trim())
                    showRename = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRename = false }) { Text("Cancel") }
            }
        )
    }
}
