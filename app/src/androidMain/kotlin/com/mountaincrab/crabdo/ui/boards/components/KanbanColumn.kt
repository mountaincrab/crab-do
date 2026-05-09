package com.mountaincrab.crabdo.ui.boards.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.runtime.MutableIntState
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
    dragFingerYAbs: MutableFloatState,
    ghostOrderBracket: MutableState<Pair<Double, Double>?>,
    findColumnIdAt: (Float) -> String?,
    onBoundsChanged: (Rect) -> Unit,
    edgeScrollState: MutableIntState,
    onDragStart: (taskId: String) -> Unit,
    onDragEnd: () -> Unit,
    onCardDropped: (taskId: String, targetColumnId: String, orderBefore: Double, orderAfter: Double) -> Unit,
    onCardTapped: (taskId: String) -> Unit,
    onAddCard: (title: String, description: String, reminderTimeMillis: Long?, reminderStyle: TaskEntity.ReminderStyle) -> Unit,
    allTasksByColumn: Map<String, List<TaskEntity>>,
    modifier: Modifier = Modifier
) {
    var showAddCardDialog by remember { mutableStateOf(false) }

    val displayTasksState = remember { mutableStateOf(tasks) }
    val currentTasksRef = rememberUpdatedState(tasks)
    val foreignDraggedTaskRef = rememberUpdatedState(foreignDraggedTask)
    val onCardDroppedRef = rememberUpdatedState(onCardDropped)
    val onDragEndRef = rememberUpdatedState(onDragEnd)
    val allTasksByColumnRef = rememberUpdatedState(allTasksByColumn)
    val findColumnIdAtRef = rememberUpdatedState(findColumnIdAt)

    var droppedSuccessfully by remember { mutableStateOf(false) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var cardHeightPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val cardSpacingPx = with(density) { 8.dp.toPx() }
    val edgeThresholdPx = with(density) { 80.dp.toPx() }
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val edgeScrollStateRef = rememberUpdatedState(edgeScrollState)
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(tasks) {
        if (draggedTaskId == null) displayTasksState.value = tasks
    }

    LaunchedEffect(draggedTaskId) {
        if (draggedTaskId == null) {
            if (!droppedSuccessfully) displayTasksState.value = currentTasksRef.value
            droppedSuccessfully = false
            dragOffsetY = 0f
        }
    }

    // Insert or remove ghost based on which column currently owns it.
    LaunchedEffect(ghostColumnId.value) {
        val ghost = foreignDraggedTaskRef.value ?: return@LaunchedEffect
        if (ghostColumnId.value == column.id) {
            if (displayTasksState.value.none { it.id == ghost.id }) {
                displayTasksState.value = displayTasksState.value + ghost
            }
        } else {
            displayTasksState.value = displayTasksState.value.filter { it.id != ghost.id }
        }
    }

    // Per-card absolute Y (top, in root coords). Used by the target column to
    // figure out where the finger is relative to its own cards, so the ghost
    // can be reordered within the target while the source still owns the
    // gesture. Each TaskCard updates its entry via onGloballyPositioned.
    val cardYsState = remember { mutableStateMapOf<String, Float>() }

    // Reorder the ghost within this column (when it's the active target) based
    // on finger Y. The source column owns the drag gesture, so it writes
    // dragFingerYAbs every tick; we observe it here. We also publish the
    // resulting order bracket so the source's onDragEnd can drop at the right
    // slot rather than always appending to the end.
    LaunchedEffect(ghostColumnId.value, column.id) {
        if (ghostColumnId.value != column.id) return@LaunchedEffect
        snapshotFlow { dragFingerYAbs.floatValue }.collect { fingerY ->
            val ghost = foreignDraggedTaskRef.value ?: return@collect
            val current = displayTasksState.value
            val curGhostIdx = current.indexOfFirst { it.id == ghost.id }
            if (curGhostIdx < 0) return@collect
            val effHeight = if (cardHeightPx > 0f) cardHeightPx
                            else with(density) { 80.dp.toPx() }

            // Find the first non-ghost card whose midpoint is below fingerY —
            // that's where the ghost should be inserted. Default to end.
            var newIdx = current.size
            for ((i, task) in current.withIndex()) {
                if (task.id == ghost.id) continue
                val cardY = cardYsState[task.id] ?: continue
                if (fingerY < cardY + effHeight / 2f) {
                    newIdx = i
                    break
                }
            }

            // If the ghost was before the insertion point, removing it shifts
            // the target index left by one.
            val target = if (curGhostIdx < newIdx) (newIdx - 1) else newIdx
            val targetCoerced = target.coerceIn(0, current.size - 1)
            if (targetCoerced != curGhostIdx) {
                val m = current.toMutableList()
                m.removeAt(curGhostIdx)
                m.add(targetCoerced.coerceIn(0, m.size), ghost)
                displayTasksState.value = m
            }

            val updated = displayTasksState.value
            val gIdx = updated.indexOfFirst { it.id == ghost.id }
            if (gIdx >= 0) {
                val prevOrder = updated.getOrNull(gIdx - 1)?.order
                val nextOrder = updated.getOrNull(gIdx + 1)?.order
                ghostOrderBracket.value = Pair(
                    prevOrder ?: ((nextOrder ?: 1.0) - 2.0),
                    nextOrder ?: ((prevOrder ?: 0.0) + 2.0)
                )
            }
        }
    }

    val displayTasks = displayTasksState.value
    val lazyListState = rememberLazyListState()

    Column(
        modifier = modifier
            .fillMaxHeight(0.9f)
            .onGloballyPositioned { onBoundsChanged(it.boundsInRoot()) }
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
                val cardAbsoluteLeftState = remember { mutableFloatStateOf(0f) }

                // Don't apply animateItem to the dragged card. When the card swaps slots
                // we compensate dragOffsetY by -slotH so the visual position stays under
                // the finger, but animateItem would interpolate from old slot to new slot
                // — at t=0 the card visibly jumps up by slotH and animates back to the
                // finger over ~150ms. Snapping the dragged card's slot makes the
                // compensation exact and the card stays glued to the finger. Other cards
                // still animate via animateItem, so they slide around the dragged card.
                val itemModifier = if (isDragging) Modifier else Modifier.animateItem()

                TaskCard(
                    task = task,
                    isDragging = isDragging,
                    modifier = itemModifier
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer {
                            alpha = if (isDragging) 0.5f else 1f
                            translationY = if (isDragging) dragOffsetY else 0f
                        }
                        .onSizeChanged { size ->
                            if (size.height > 0) cardHeightPx = size.height.toFloat()
                        }
                        .onGloballyPositioned { coords ->
                            val r = coords.boundsInRoot()
                            cardAbsoluteLeftState.floatValue = r.left
                            cardYsState[task.id] = r.top
                        }
                        .pointerInput(capturedTaskId) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { _ ->
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onDragStart(capturedTaskId)
                                    dragOffsetY = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()

                                    // Absolute pointer coords in root space.
                                    // graphicsLayer translation doesn't affect
                                    // layout, so the source card's stored
                                    // top/left reflect its layout slot, not
                                    // its rendered position — so adding
                                    // change.position gives the real finger
                                    // location.
                                    val absX = cardAbsoluteLeftState.floatValue + change.position.x
                                    val absY = (cardYsState[capturedTaskId] ?: 0f) + change.position.y
                                    dragFingerYAbs.floatValue = absY

                                    // Edge scroll detection.
                                    edgeScrollStateRef.value.intValue = when {
                                        absX < edgeThresholdPx -> -1
                                        absX > screenWidthPx - edgeThresholdPx -> 1
                                        else -> 0
                                    }

                                    // Cross-column detection.
                                    val targetColumn = findColumnIdAtRef.value(absX)
                                    when {
                                        targetColumn != null && targetColumn != column.id -> {
                                            if (ghostColumnId.value != targetColumn) {
                                                ghostColumnId.value = targetColumn
                                                dragOffsetY = 0f
                                            }
                                            return@detectDragGesturesAfterLongPress
                                        }
                                        targetColumn == column.id -> {
                                            // Positively back in source column — clear ghost.
                                            if (ghostColumnId.value != null && ghostColumnId.value != column.id) {
                                                ghostColumnId.value = null
                                                dragOffsetY = 0f
                                            }
                                        }
                                        else -> {
                                            // targetColumn == null: pointer is in a gap or off-screen.
                                            // If a ghost is already active in another column, don't
                                            // re-arrange the source column underneath the user's feet —
                                            // they've committed to the target column.
                                            if (ghostColumnId.value != null && ghostColumnId.value != column.id) {
                                                return@detectDragGesturesAfterLongPress
                                            }
                                        }
                                    }

                                    // Within-column: accumulate Y and reorder at midpoint.
                                    dragOffsetY += dragAmount.y
                                    val slotH = if (cardHeightPx > 0f) cardHeightPx + cardSpacingPx
                                                else with(density) { 80.dp.toPx() }
                                    var curIdx = displayTasksState.value.indexOfFirst { it.id == capturedTaskId }
                                    if (curIdx < 0) return@detectDragGesturesAfterLongPress

                                    while (dragOffsetY > slotH / 2f && curIdx < displayTasksState.value.size - 1) {
                                        val m = displayTasksState.value.toMutableList()
                                        m.add(curIdx + 1, m.removeAt(curIdx))
                                        displayTasksState.value = m
                                        dragOffsetY -= slotH
                                        curIdx++
                                    }
                                    while (dragOffsetY < -slotH / 2f && curIdx > 0) {
                                        val m = displayTasksState.value.toMutableList()
                                        m.add(curIdx - 1, m.removeAt(curIdx))
                                        displayTasksState.value = m
                                        dragOffsetY += slotH
                                        curIdx--
                                    }
                                },
                                onDragEnd = {
                                    val currentGhost = ghostColumnId.value
                                    if (currentGhost != null && currentGhost != column.id) {
                                        // Cross-column: drop at the slot the
                                        // target column has positioned us in.
                                        // Falls back to appending if the
                                        // target hasn't yet computed a bracket
                                        // (e.g. instant release after entry).
                                        val bracket = ghostOrderBracket.value
                                        if (bracket != null) {
                                            onCardDroppedRef.value(capturedTaskId, currentGhost, bracket.first, bracket.second)
                                        } else {
                                            val targetTasks = allTasksByColumnRef.value[currentGhost] ?: emptyList()
                                            val maxOrder = targetTasks.maxOfOrNull { it.order } ?: 0.0
                                            onCardDroppedRef.value(capturedTaskId, currentGhost, maxOrder, maxOrder + 2.0)
                                        }
                                    } else {
                                        // Within-column: commit from current display order.
                                        val current = displayTasksState.value
                                        val idx = current.indexOfFirst { it.id == capturedTaskId }
                                        if (idx >= 0) {
                                            val prevOrder = current.getOrNull(idx - 1)?.order
                                            val nextOrder = current.getOrNull(idx + 1)?.order
                                            onCardDroppedRef.value(capturedTaskId, column.id,
                                                prevOrder ?: ((nextOrder ?: 1.0) - 2.0),
                                                nextOrder ?: ((prevOrder ?: 0.0) + 2.0))
                                        }
                                    }
                                    droppedSuccessfully = true
                                    dragOffsetY = 0f
                                    ghostColumnId.value = null
                                    edgeScrollStateRef.value.intValue = 0
                                    onDragEndRef.value()
                                },
                                onDragCancel = {
                                    dragOffsetY = 0f
                                    ghostColumnId.value = null
                                    edgeScrollStateRef.value.intValue = 0
                                    displayTasksState.value = currentTasksRef.value
                                    onDragEndRef.value()
                                }
                            )
                        },
                    onTap = { onCardTapped(capturedTaskId) }
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
