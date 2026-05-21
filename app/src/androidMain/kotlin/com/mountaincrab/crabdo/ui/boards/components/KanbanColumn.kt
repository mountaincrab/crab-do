package com.mountaincrab.crabdo.ui.boards.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.runtime.MutableIntState
import com.mountaincrab.crabdo.data.local.dao.SubtaskCountAggregate
import com.mountaincrab.crabdo.data.local.entity.ColumnEntity
import com.mountaincrab.crabdo.data.local.entity.TaskEntity
import com.mountaincrab.crabdo.ui.theme.LocalAppPalette

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
    subtaskCounts: Map<String, SubtaskCountAggregate> = emptyMap(),
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
    val cardSpacingPx = with(density) { 6.dp.toPx() }
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

    // Per-card absolute Y (top, in root coords). Used by the target column to
    // figure out where the finger is relative to its own cards, so the ghost
    // can be reordered within the target while the source still owns the
    // gesture. Each TaskCard updates its entry via onGloballyPositioned.
    val cardYsState = remember { mutableStateMapOf<String, Float>() }
    // Per-card measured height. Needed so the within-column swap can compensate
    // dragOffsetY by the *neighbor's* height (not the dragged card's), keeping
    // the source card glued to the finger when neighbors have mixed heights.
    val cardHeightsState = remember { mutableStateMapOf<String, Float>() }
    // Top of the LazyColumn's parent Box in root coords. The foreign-ghost
    // overlay anchors its translationY to this (stable across LazyColumn
    // reorders), so the ghost tracks the finger smoothly. Previously the
    // ghost lived inside the LazyColumn with translationY = fingerY -
    // cardY - h/2, but cardY was a one-frame-stale read from
    // onGloballyPositioned — every reorder produced a 1-slot visual jump
    // that corrected on the next frame.
    val boxTopInRootState = remember { mutableFloatStateOf(0f) }

    // Insert/remove the ghost and track its vertical position within this column.
    // Insertion and the snapshotFlow collection are combined in one effect so the
    // ghost is guaranteed to be in displayTasksState before the first fingerY
    // emission — previously the two were separate effects and the flow fired
    // before insertion, leaving the ghost stranded at the bottom until the next
    // drag event.
    LaunchedEffect(ghostColumnId.value) {
        val ghost = foreignDraggedTaskRef.value ?: return@LaunchedEffect
        if (ghostColumnId.value == column.id) {
            if (displayTasksState.value.none { it.id == ghost.id }) {
                displayTasksState.value = displayTasksState.value + ghost
            }
            // Now start tracking — ghost is in the list so the first emission
            // can position it immediately rather than bailing with curGhostIdx=-1.
            snapshotFlow { dragFingerYAbs.floatValue }.collect { fingerY ->
                val current = displayTasksState.value
                val curGhostIdx = current.indexOfFirst { it.id == ghost.id }
                if (curGhostIdx < 0) return@collect
                val effHeight = if (cardHeightPx > 0f) cardHeightPx
                                else with(density) { 80.dp.toPx() }

                var newIdx = current.size
                for ((i, task) in current.withIndex()) {
                    if (task.id == ghost.id) continue
                    val cardY = cardYsState[task.id] ?: continue
                    if (fingerY < cardY + effHeight / 2f) {
                        newIdx = i
                        break
                    }
                }

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
        } else {
            displayTasksState.value = displayTasksState.value.filter { it.id != ghost.id }
        }
    }

    val displayTasks = displayTasksState.value
    val lazyListState = rememberLazyListState()

    Column(
        modifier = modifier
            .fillMaxHeight(0.9f)
            .onGloballyPositioned { onBoundsChanged(it.boundsInRoot()) }
    ) {
        // Uppercase tracked column label + count chip — matches the design
        // system's .col-head pattern (eyebrow-style header, count in a
        // surface-high pill).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = column.title.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (tasks.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(LocalAppPalette.current.surfaceHigh)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = tasks.size.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .onGloballyPositioned { boxTopInRootState.floatValue = it.boundsInRoot().top }
        ) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
            itemsIndexed(displayTasks, key = { _, it -> it.id }) { _, task ->
                val isDragging = task.id == draggedTaskId
                val capturedTaskId = task.id
                val cardAbsoluteLeftState = remember { mutableFloatStateOf(0f) }
                val counts = subtaskCounts[task.id]

                // Don't apply animateItem to the dragged card. When the card swaps slots
                // we compensate dragOffsetY by -slotH so the visual position stays under
                // the finger, but animateItem would interpolate from old slot to new slot
                // — at t=0 the card visibly jumps up by slotH and animates back to the
                // finger over ~150ms. Snapping the dragged card's slot makes the
                // compensation exact and the card stays glued to the finger. Other cards
                // still animate via animateItem, so they slide around the dragged card.
                val itemModifier = if (isDragging) Modifier else Modifier.animateItem()

                // A foreign ghost is the dragged task inserted into a *target*
                // column. The source column still owns the gesture and updates
                // dragFingerYAbs; the target's dragOffsetY is never set, so we
                // can't translate by it. Instead, glue the ghost to the finger
                // by translating from its current layout slot to the finger Y.
                val isForeignGhost = isDragging && foreignDraggedTask != null

                TaskCard(
                    task = task,
                    subtaskCount = counts?.total ?: 0,
                    completedSubtaskCount = counts?.completed ?: 0,
                    isDragging = isDragging,
                    modifier = itemModifier
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer {
                            // Foreign ghost: invisible inside the LazyColumn
                            // (just provides the drop-zone gap via its natural
                            // size). The visible copy is rendered as an overlay
                            // outside the LazyColumn so its translationY can be
                            // anchored to a stable reference.
                            alpha = when {
                                isForeignGhost -> 0f
                                isDragging -> 0.5f
                                else -> 1f
                            }
                            translationY = when {
                                isForeignGhost -> 0f
                                isDragging -> dragOffsetY
                                else -> 0f
                            }
                        }
                        .onSizeChanged { size ->
                            if (size.height > 0) {
                                cardHeightPx = size.height.toFloat()
                                cardHeightsState[task.id] = size.height.toFloat()
                            }
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
                                    // The swap threshold and compensation must use the
                                    // *neighbor's* height + spacing, not the dragged
                                    // card's. After a swap the dragged card's layout
                                    // slot shifts by the neighbor's height + spacing —
                                    // subtracting anything else from dragOffsetY leaves
                                    // a residual that visibly bumps the card off the
                                    // finger every time it passes a card of a different
                                    // height.
                                    dragOffsetY += dragAmount.y
                                    val fallbackSlotH = if (cardHeightPx > 0f) cardHeightPx + cardSpacingPx
                                                        else with(density) { 80.dp.toPx() }
                                    var curIdx = displayTasksState.value.indexOfFirst { it.id == capturedTaskId }
                                    if (curIdx < 0) return@detectDragGesturesAfterLongPress

                                    while (curIdx < displayTasksState.value.size - 1) {
                                        val neighborId = displayTasksState.value[curIdx + 1].id
                                        val neighborSlotH = cardHeightsState[neighborId]?.plus(cardSpacingPx)
                                            ?: fallbackSlotH
                                        if (dragOffsetY <= neighborSlotH / 2f) break
                                        val m = displayTasksState.value.toMutableList()
                                        m.add(curIdx + 1, m.removeAt(curIdx))
                                        displayTasksState.value = m
                                        dragOffsetY -= neighborSlotH
                                        curIdx++
                                    }
                                    while (curIdx > 0) {
                                        val neighborId = displayTasksState.value[curIdx - 1].id
                                        val neighborSlotH = cardHeightsState[neighborId]?.plus(cardSpacingPx)
                                            ?: fallbackSlotH
                                        if (dragOffsetY >= -neighborSlotH / 2f) break
                                        val m = displayTasksState.value.toMutableList()
                                        m.add(curIdx - 1, m.removeAt(curIdx))
                                        displayTasksState.value = m
                                        dragOffsetY += neighborSlotH
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
                // Flat left-aligned "+ Add task" — matches .add-task in the
                // design system (no outlined surface, just muted text).
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showAddCardDialog = true }
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = "+ Add task",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.sp,
                            fontSize = 13.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            }

            // Floating ghost overlay — anchored to the column's Box top in
            // root coords (a stable reference across LazyColumn reorders), so
            // translationY tracks the finger smoothly. The dragged task entry
            // is still inserted into the LazyColumn's displayTasksState (so
            // ghostOrderBracket gets computed for the drop) but rendered with
            // alpha=0 — its natural size provides the drop-zone gap.
            if (foreignDraggedTask != null && ghostColumnId.value == column.id) {
                val ghostTask = foreignDraggedTask
                val ghostCounts = subtaskCounts[ghostTask.id]
                TaskCard(
                    task = ghostTask,
                    subtaskCount = ghostCounts?.total ?: 0,
                    completedSubtaskCount = ghostCounts?.completed ?: 0,
                    isDragging = true,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .graphicsLayer {
                            alpha = 0.5f
                            val h = cardHeightsState[ghostTask.id] ?: cardHeightPx
                            translationY = dragFingerYAbs.floatValue - boxTopInRootState.floatValue - h / 2f
                        }
                        .onSizeChanged { size ->
                            if (size.height > 0) {
                                cardHeightsState[ghostTask.id] = size.height.toFloat()
                            }
                        },
                    onTap = {}
                )
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
