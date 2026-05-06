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
    onDragStart: (taskId: String) -> Unit,
    onDragEnd: () -> Unit,
    onCardDropped: (taskId: String, targetColumnId: String, orderBefore: Double, orderAfter: Double) -> Unit,
    onCardTapped: (taskId: String) -> Unit,
    onAddCard: (title: String, description: String, reminderTimeMillis: Long?, reminderStyle: TaskEntity.ReminderStyle) -> Unit,
    @Suppress("UNUSED_PARAMETER") onReorder: (taskId: String, orderBefore: Double, orderAfter: Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddCardDialog by remember { mutableStateOf(false) }

    // Live display order — mutated by DnD hover events so items shift as you drag.
    // The dragged card stays in the list (dimmed) rather than disappearing.
    val displayTasksState = remember { mutableStateOf(tasks) }
    val currentTasksRef = rememberUpdatedState(tasks)
    val draggedTaskIdRef = rememberUpdatedState(draggedTaskId)
    val onCardDroppedRef = rememberUpdatedState(onCardDropped)
    val onDragEndRef = rememberUpdatedState(onDragEnd)

    // Track whether a drop was committed so we don't clobber the display on session end.
    var droppedSuccessfully by remember { mutableStateOf(false) }

    // Sync display when tasks list changes from outside (Firestore) while not dragging.
    LaunchedEffect(tasks) {
        if (draggedTaskId == null) displayTasksState.value = tasks
    }
    // Reset display when drag session ends. Skip reset on successful drop — the display
    // already shows the correct order, and Room will update tasks shortly after.
    LaunchedEffect(draggedTaskId) {
        if (draggedTaskId == null && !droppedSuccessfully) {
            displayTasksState.value = currentTasksRef.value
        }
        droppedSuccessfully = false
    }

    val displayTasks = displayTasksState.value

    // Shared drop logic used by all targets in this column.
    // Same-column: use current display position (reflects drag reordering).
    // Cross-column: append to end.
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
        droppedSuccessfully = true
        onDragEndRef.value()
        return true
    }

    // Catch drops in dead zones (gaps between cards, empty column). No onEntered —
    // parent fires before children and causes indicator flicker (see prior fix).
    val appendDropTarget = remember(column.id) {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                val taskId = event.toAndroidDragEvent().clipData?.getItemAt(0)?.text?.toString()
                    ?: return false
                return commitDrop(taskId)
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
                            if (fromIdx < 0) return // cross-column drag — skip shifting
                            val tIdx = current.indexOfFirst { it.id == capturedTaskId }
                            if (tIdx < 0 || tIdx == fromIdx) return
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
                        .dragAndDropSource(drawDragDecoration = {}) {
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

            // Trailing drop zone — moves dragged item to end of column on hover.
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
                            if (fromIdx < 0) return // cross-column
                            if (fromIdx == current.size - 1) return // already last
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
