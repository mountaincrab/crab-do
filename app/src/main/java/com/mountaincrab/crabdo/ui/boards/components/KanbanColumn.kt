package com.mountaincrab.crabdo.ui.boards.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.unit.dp
import com.mountaincrab.crabdo.data.local.entity.ColumnEntity
import com.mountaincrab.crabdo.data.local.entity.TaskEntity
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun KanbanColumn(
    column: ColumnEntity,
    tasks: List<TaskEntity>,
    onCardDropped: (taskId: String, targetColumnId: String, orderBefore: Double, orderAfter: Double) -> Unit,
    onCardTapped: (taskId: String) -> Unit,
    onAddCard: (title: String) -> Unit,
    onReorder: (taskId: String, orderBefore: Double, orderAfter: Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDropTarget by remember { mutableStateOf(false) }
    var showAddCard by remember { mutableStateOf(false) }
    var newCardTitle by remember { mutableStateOf("") }

    val dropTarget = remember(column.id, tasks) {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                val taskId = event.toAndroidDragEvent().clipData?.getItemAt(0)?.text?.toString()
                    ?: return false
                val maxOrder = tasks.maxOfOrNull { it.order } ?: 0.0
                onCardDropped(taskId, column.id, maxOrder, maxOrder + 2.0)
                isDropTarget = false
                return true
            }
            override fun onEntered(event: DragAndDropEvent) { isDropTarget = true }
            override fun onExited(event: DragAndDropEvent) { isDropTarget = false }
            override fun onEnded(event: DragAndDropEvent) { isDropTarget = false }
        }
    }

    val lazyListState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val reordered = tasks.toMutableList().apply { add(to.index, removeAt(from.index)) }
        val moved = reordered[to.index]
        val before = reordered.getOrNull(to.index - 1)?.order ?: 0.0
        val after = reordered.getOrNull(to.index + 1)?.order ?: (before + 2.0)
        onReorder(moved.id, before, after)
    }

    Card(
        modifier = modifier
            .width(280.dp)
            .fillMaxHeight(0.85f)
            .dragAndDropTarget(shouldStartDragAndDrop = { true }, target = dropTarget),
        border = if (isDropTarget) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Column header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = column.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${tasks.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider()

            LazyColumn(
                state = lazyListState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    ReorderableItem(reorderState, key = task.id) { isDragging ->
                        TaskCard(
                            task = task,
                            isDragging = isDragging,
                            modifier = Modifier
                                .longPressDraggableHandle()
                                .dragAndDropSource(drawDragDecoration = {}) {
                                    DragAndDropTransferData(
                                        clipData = android.content.ClipData.newPlainText("taskId", task.id)
                                    )
                                },
                            onTap = { onCardTapped(task.id) }
                        )
                    }
                }
                item {
                    if (showAddCard) {
                        OutlinedTextField(
                            value = newCardTitle,
                            onValueChange = { newCardTitle = it },
                            placeholder = { Text("Card title") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            trailingIcon = {
                                Row {
                                    TextButton(onClick = {
                                        if (newCardTitle.isNotBlank()) {
                                            onAddCard(newCardTitle.trim())
                                            newCardTitle = ""
                                        }
                                        showAddCard = false
                                    }) { Text("Add") }
                                    TextButton(onClick = {
                                        showAddCard = false
                                        newCardTitle = ""
                                    }) { Text("Cancel") }
                                }
                            }
                        )
                    } else {
                        TextButton(
                            onClick = { showAddCard = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Add card")
                        }
                    }
                }
            }
        }
    }
}
