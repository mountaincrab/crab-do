package com.mountaincrab.crabdo.ui.boards.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
    foreignDraggedTask: TaskEntity?,
    ghostColumnId: MutableState<String?>,
    onDragStart: (taskId: String) -> Unit,
    onDragEnd: () -> Unit,
    onCardDropped: (taskId: String, targetColumnId: String, orderBefore: Double, orderAfter: Double) -> Unit,
    onCardTapped: (taskId: String) -> Unit,
    onAddCard: (title: String, description: String, reminderTimeMillis: Long?, reminderStyle: TaskEntity.ReminderStyle) -> Unit,
    @Suppress("UNUSED_PARAMETER") onReorder: (taskId: String, orderBefore: Double, orderAfter: Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddCardDialog by remember { mutableStateOf(false) }

    val displayTasksState = remember { mutableStateOf(tasks) }
    val currentTasksRef = rememberUpdatedState(tasks)
    val draggedTaskIdRef = rememberUpdatedState(draggedTaskId)
    val foreignDraggedTaskRef = rememberUpdatedState(foreignDraggedTask)
    val onCardDroppedRef = rememberUpdatedState(onCardDropped)
    val onDragEndRef = rememberUpdatedState(onDragEnd)

    var droppedSuccessfully by remember { mutableStateOf(false) }
    // Set true when this column receives a cross-column drop; prevents premature ghost removal
    // while waiting for Room to push the moved task into this column's task list.
    var ghostDropped by remember { mutableStateOf(false) }

    LaunchedEffect(tasks) {
        if (draggedTaskId == null) {
            displayTasksState.value = tasks
            ghostDropped = false
        }
    }
    LaunchedEffect(draggedTaskId) {
        if (draggedTaskId == null && !droppedSuccessfully) {
            displayTasksState.value = currentTasksRef.value
        }
        droppedSuccessfully = false
    }

    // Remove ghost when another column takes over as ghost host.
    LaunchedEffect(ghostColumnId.value) {
        if (ghostColumnId.value != column.id && !ghostDropped) {
            val ghost = foreignDraggedTaskRef.value ?: return@LaunchedEffect
            displayTasksState.value = displayTasksState.value.filter { it.id != ghost.id }
        }
    }

    val displayTasks = displayTasksState.value

    // Draw colors captured here so drawDragDecoration can close over them.
    val cardBgColor = MaterialTheme.colorScheme.surfaceVariant
    val cardBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)

    fun commitDrop(taskId: String): Boolean {
        val current = displayTasksState.value
        val finalIdx = current.indexOfFirst { it.id == taskId }
        if (finalIdx >= 0) {
            val prevOrder = current.getOrNull(finalIdx - 1)?.order
            val nextOrder = current.getOrNull(finalIdx + 1)?.order
            onCardDroppedRef.value(taskId, column.id,
                prevOrder ?: ((nextOrder ?: 1.0) - 2.0),
                nextOrder ?: ((prevOrder ?: 0.0) + 2.0))
        } else {
            val maxOrder = current.maxOfOrNull { it.order } ?: 0.0
            onCardDroppedRef.value(taskId, column.id, maxOrder, maxOrder + 2.0)
        }
        // If we hosted a ghost, mark it so Room can catch up before we clear the display.
        if (foreignDraggedTaskRef.value != null) ghostDropped = true
        droppedSuccessfully = true
        onDragEndRef.value()
        return true
    }

    // Column-level drop target: handles drops in gaps between cards, empty columns, and
    // cross-column entry for inserting the ghost when there are no card targets to hover.
    val appendDropTarget = remember(column.id) {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                val taskId = event.toAndroidDragEvent().clipData?.getItemAt(0)?.text?.toString()
                    ?: return false
                return commitDrop(taskId)
            }
            override fun onEntered(event: DragAndDropEvent) {
                // Insert ghost into an empty column (card targets handle non-empty columns).
                val ghost = foreignDraggedTaskRef.value ?: return
                val current = displayTasksState.value
                if (current.any { it.id == ghost.id }) return
                ghostColumnId.value = column.id
                displayTasksState.value = current + ghost
            }
            override fun onEnded(event: DragAndDropEvent) {
                if (!droppedSuccessfully) displayTasksState.value = currentTasksRef.value
            }
        }
    }

    val lazyListState = rememberLazyListState()

    Column(
        modifier = modifier
            .fillMaxHeight(0.9f)
            .dragAndDropTarget(shouldStartDragAndDrop = { true }, target = appendDropTarget)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
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
            itemsIndexed(displayTasks, key = { _, it -> it.id }) { _, task ->
                val isDragging = task.id == draggedTaskId
                val capturedTaskId = task.id

                val insertBeforeTarget = remember(capturedTaskId) {
                    object : DragAndDropTarget {
                        override fun onDrop(event: DragAndDropEvent): Boolean {
                            val taskId = event.toAndroidDragEvent().clipData?.getItemAt(0)?.text?.toString()
                                ?: return false
                            return commitDrop(taskId)
                        }
                        override fun onEntered(event: DragAndDropEvent) {
                            val dragId = draggedTaskIdRef.value ?: return
                            val current = displayTasksState.value
                            val fromIdx = current.indexOfFirst { it.id == dragId }
                            val tIdx = current.indexOfFirst { it.id == capturedTaskId }
                            if (tIdx < 0) return

                            if (fromIdx < 0) {
                                // Cross-column: insert or reorder the ghost.
                                val ghost = foreignDraggedTaskRef.value ?: return
                                ghostColumnId.value = column.id
                                val ghostIdx = current.indexOfFirst { it.id == ghost.id }
                                if (ghostIdx < 0) {
                                    val mutable = current.toMutableList()
                                    mutable.add(tIdx, ghost)
                                    displayTasksState.value = mutable
                                } else {
                                    if (ghostIdx == tIdx) return
                                    val mutable = current.toMutableList()
                                    val item = mutable.removeAt(ghostIdx)
                                    mutable.add(if (tIdx > ghostIdx) tIdx - 1 else tIdx, item)
                                    displayTasksState.value = mutable
                                }
                                return
                            }

                            // Same-column reorder.
                            if (tIdx == fromIdx) return
                            val mutable = current.toMutableList()
                            val item = mutable.removeAt(fromIdx)
                            mutable.add(if (tIdx > fromIdx) tIdx - 1 else tIdx, item)
                            displayTasksState.value = mutable
                        }
                        override fun onEnded(event: DragAndDropEvent) {
                            if (!droppedSuccessfully) displayTasksState.value = currentTasksRef.value
                        }
                    }
                }

                TaskCard(
                    task = task,
                    isDragging = isDragging,
                    modifier = Modifier
                        .animateItem()
                        .graphicsLayer { alpha = if (isDragging) 0.5f else 1f }
                        .dragAndDropTarget(shouldStartDragAndDrop = { true }, target = insertBeforeTarget)
                        .dragAndDropSource(drawDragDecoration = {
                            val cr = CornerRadius(10.dp.toPx())
                            drawRoundRect(color = cardBgColor, cornerRadius = cr)
                            drawRoundRect(color = cardBorderColor, cornerRadius = cr,
                                style = Stroke(width = 1.dp.toPx()))
                        }) {
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

            // Trailing drop zone: moves dragged item to end of column on hover.
            item {
                val trailingTarget = remember(column.id) {
                    object : DragAndDropTarget {
                        override fun onDrop(event: DragAndDropEvent): Boolean {
                            val taskId = event.toAndroidDragEvent().clipData?.getItemAt(0)?.text?.toString()
                                ?: return false
                            return commitDrop(taskId)
                        }
                        override fun onEntered(event: DragAndDropEvent) {
                            val dragId = draggedTaskIdRef.value ?: return
                            val current = displayTasksState.value
                            val fromIdx = current.indexOfFirst { it.id == dragId }

                            if (fromIdx < 0) {
                                val ghost = foreignDraggedTaskRef.value ?: return
                                ghostColumnId.value = column.id
                                val ghostIdx = current.indexOfFirst { it.id == ghost.id }
                                if (ghostIdx < 0) {
                                    displayTasksState.value = current + ghost
                                } else {
                                    if (ghostIdx == current.size - 1) return
                                    val mutable = current.toMutableList()
                                    mutable.add(mutable.removeAt(ghostIdx))
                                    displayTasksState.value = mutable
                                }
                                return
                            }

                            if (fromIdx == current.size - 1) return
                            val mutable = current.toMutableList()
                            mutable.add(mutable.removeAt(fromIdx))
                            displayTasksState.value = mutable
                        }
                        override fun onEnded(event: DragAndDropEvent) {
                            if (!droppedSuccessfully) displayTasksState.value = currentTasksRef.value
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (draggedTaskId != null) 32.dp else 0.dp)
                        .dragAndDropTarget(shouldStartDragAndDrop = { true }, target = trailingTarget)
                )
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
                    Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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
