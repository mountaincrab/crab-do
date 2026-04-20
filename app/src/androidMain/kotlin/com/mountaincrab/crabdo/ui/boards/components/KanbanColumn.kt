package com.mountaincrab.crabdo.ui.boards.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mountaincrab.crabdo.data.local.entity.ColumnEntity
import com.mountaincrab.crabdo.data.local.entity.TaskEntity

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KanbanColumn(
    column: ColumnEntity,
    tasks: List<TaskEntity>,
    draggedTaskId: String?,
    onDragStart: (taskId: String) -> Unit,
    onDragEnd: () -> Unit,
    onCardDropped: (taskId: String, targetColumnId: String, orderBefore: Double, orderAfter: Double) -> Unit,
    onCardTapped: (taskId: String) -> Unit,
    onAddCard: (title: String, description: String, reminderTimeMillis: Long?, reminderStyle: TaskEntity.ReminderStyle) -> Unit,
    @Suppress("UNUSED_PARAMETER") onReorder: (taskId: String, orderBefore: Double, orderAfter: Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddCardDialog by remember { mutableStateOf(false) }

    // Hide the card currently being dragged so the column visually collapses around it,
    // and compute the order of the remaining (visible) tasks so insert-indices align
    // with what the user sees.
    val visibleTasks = remember(tasks, draggedTaskId) {
        tasks.filter { it.id != draggedTaskId }
    }

    // null = no hover in this column; otherwise the index (in visibleTasks) *before which*
    // the card would be inserted. visibleTasks.size means "drop at the end".
    var hoverIndex by remember { mutableStateOf<Int?>(null) }

    // Drop at the end of this column (or into an empty column).
    val appendDropTarget = remember(column.id, tasks) {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                val taskId = event.toAndroidDragEvent().clipData?.getItemAt(0)?.text?.toString()
                    ?: return false
                val maxOrder = tasks.filter { it.id != taskId }.maxOfOrNull { it.order } ?: 0.0
                onCardDropped(taskId, column.id, maxOrder, maxOrder + 2.0)
                hoverIndex = null
                onDragEnd()
                return true
            }
            override fun onEntered(event: DragAndDropEvent) { hoverIndex = visibleTasks.size }
            override fun onEnded(event: DragAndDropEvent) { hoverIndex = null }
        }
    }

    val lazyListState = rememberLazyListState()

    Column(
        modifier = modifier
            .fillMaxHeight(0.9f)
            .dragAndDropTarget(shouldStartDragAndDrop = { true }, target = appendDropTarget)
    ) {
        // Todoist-style header: title + count
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = column.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            if (tasks.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = tasks.size.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyColumn(
            state = lazyListState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(visibleTasks, key = { _, it -> it.id }) { index, task ->
                val insertBeforeTarget = remember(column.id, visibleTasks, task.id, index) {
                    object : DragAndDropTarget {
                        override fun onDrop(event: DragAndDropEvent): Boolean {
                            val draggedId = event.toAndroidDragEvent().clipData?.getItemAt(0)?.text?.toString()
                                ?: return false
                            if (draggedId == task.id) return false
                            val prevOrder = visibleTasks.getOrNull(index - 1)?.order ?: (task.order - 2.0)
                            onCardDropped(draggedId, column.id, prevOrder, task.order)
                            hoverIndex = null
                            onDragEnd()
                            return true
                        }
                        override fun onEntered(event: DragAndDropEvent) { hoverIndex = index }
                        override fun onEnded(event: DragAndDropEvent) { hoverIndex = null }
                    }
                }
                Column {
                    // Insert indicator above this card
                    DropIndicator(visible = hoverIndex == index)
                    TaskCard(
                        task = task,
                        isDragging = false,
                        modifier = Modifier
                            .dragAndDropTarget(shouldStartDragAndDrop = { true }, target = insertBeforeTarget)
                            .dragAndDropSource {
                                detectTapGestures(
                                    onTap = { onCardTapped(task.id) },
                                    onLongPress = {
                                        onDragStart(task.id)
                                        startTransfer(
                                            DragAndDropTransferData(
                                                clipData = android.content.ClipData.newPlainText("taskId", task.id)
                                            )
                                        )
                                    }
                                )
                            },
                        onTap = { onCardTapped(task.id) }
                    )
                }
            }
            // Trailing indicator for drop-at-end — explicit target so onEntered fires reliably.
            item {
                val trailingTarget = remember(column.id, tasks, visibleTasks) {
                    object : DragAndDropTarget {
                        override fun onDrop(event: DragAndDropEvent): Boolean {
                            val taskId = event.toAndroidDragEvent().clipData?.getItemAt(0)?.text?.toString()
                                ?: return false
                            val maxOrder = tasks.filter { it.id != taskId }.maxOfOrNull { it.order } ?: 0.0
                            onCardDropped(taskId, column.id, maxOrder, maxOrder + 2.0)
                            hoverIndex = null
                            onDragEnd()
                            return true
                        }
                        override fun onEntered(event: DragAndDropEvent) { hoverIndex = visibleTasks.size }
                        override fun onEnded(event: DragAndDropEvent) { hoverIndex = null }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (draggedTaskId != null) 32.dp else 0.dp)
                        .dragAndDropTarget(shouldStartDragAndDrop = { true }, target = trailingTarget)
                ) {
                    DropIndicator(visible = hoverIndex == visibleTasks.size && draggedTaskId != null)
                }
            }
            item {
                OutlinedButton(
                    onClick = { showAddCardDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Add task")
                }
            }
        }
    }

    if (showAddCardDialog) {
        AddCardDialog(
            onAdd = { title, description, reminderAt, style ->
                onAddCard(title, description, reminderAt, style)
                showAddCardDialog = false
            },
            onDismiss = { showAddCardDialog = false }
        )
    }
}

@Composable
private fun DropIndicator(visible: Boolean) {
    if (visible) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
                .height(3.dp)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}
