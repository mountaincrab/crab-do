package com.mountaincrab.crabdo.ui.boards.components

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
    onCardDropped: (taskId: String, targetColumnId: String, orderBefore: Double, orderAfter: Double) -> Unit,
    onCardTapped: (taskId: String) -> Unit,
    onAddCard: (title: String, description: String, reminderTimeMillis: Long?, reminderStyle: TaskEntity.ReminderStyle) -> Unit,
    @Suppress("UNUSED_PARAMETER") onReorder: (taskId: String, orderBefore: Double, orderAfter: Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDropTarget by remember { mutableStateOf(false) }
    var showAddCardDialog by remember { mutableStateOf(false) }

    // Drop at the end of this column (or into an empty column).
    val appendDropTarget = remember(column.id, tasks) {
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

    Column(
        modifier = modifier
            .width(280.dp)
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
            itemsIndexed(tasks, key = { _, it -> it.id }) { index, task ->
                val insertBeforeTarget = remember(column.id, tasks, task.id) {
                    object : DragAndDropTarget {
                        override fun onDrop(event: DragAndDropEvent): Boolean {
                            val draggedId = event.toAndroidDragEvent().clipData?.getItemAt(0)?.text?.toString()
                                ?: return false
                            if (draggedId == task.id) return false
                            val prevOrder = tasks.getOrNull(index - 1)?.order ?: (task.order - 2.0)
                            onCardDropped(draggedId, column.id, prevOrder, task.order)
                            return true
                        }
                    }
                }
                TaskCard(
                    task = task,
                    isDragging = false,
                    modifier = Modifier
                        .dragAndDropTarget(shouldStartDragAndDrop = { true }, target = insertBeforeTarget)
                        .dragAndDropSource {
                            detectTapGestures(
                                onTap = { onCardTapped(task.id) },
                                onLongPress = {
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
            item {
                TextButton(
                    onClick = { showAddCardDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Add card")
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
