package com.mountaincrab.crabdo.ui.boards.components

import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.mountaincrab.crabdo.data.local.dao.SubtaskCountAggregate
import com.mountaincrab.crabdo.data.local.entity.ColumnEntity
import com.mountaincrab.crabdo.data.local.entity.TaskEntity
import com.mountaincrab.crabdo.ui.theme.LocalAppPalette
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KanbanColumn(
    column: ColumnEntity,
    tasks: List<TaskEntity>,
    columns: List<ColumnEntity>,
    subtaskCounts: Map<String, SubtaskCountAggregate> = emptyMap(),
    onCardReordered: (taskId: String, orderBefore: Double, orderAfter: Double) -> Unit,
    onCardMovedToColumn: (taskId: String, toColumnId: String) -> Unit,
    onTaskTapped: (taskId: String) -> Unit,
    onAddCard: (title: String, description: String, reminderTimeMillis: Long?, reminderStyle: TaskEntity.ReminderStyle, columnId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddCardSheet by remember { mutableStateOf(false) }

    val displayTasksState = remember { mutableStateOf(tasks) }
    val currentTasksRef = rememberUpdatedState(tasks)
    val onCardReorderedRef = rememberUpdatedState(onCardReordered)
    val onCardMovedToColumnRef = rememberUpdatedState(onCardMovedToColumn)
    val columnsRef = rememberUpdatedState(columns)

    var draggedTaskId by remember { mutableStateOf<String?>(null) }
    var droppedSuccessfully by remember { mutableStateOf(false) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var cardHeightPx by remember { mutableFloatStateOf(0f) }

    // One card can be swiped horizontally at a time.
    var swipingCardId by remember { mutableStateOf<String?>(null) }
    val swipeDxState = remember { mutableFloatStateOf(0f) }

    val density = LocalDensity.current
    val cardSpacingPx = with(density) { 6.dp.toPx() }
    val swipeThresholdPx = with(density) { 70.dp.toPx() }
    val haptic = LocalHapticFeedback.current

    val cardYsState = remember { mutableStateMapOf<String, Float>() }
    val cardHeightsState = remember { mutableStateMapOf<String, Float>() }

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

    val displayTasks = displayTasksState.value
    val lazyListState = rememberLazyListState()

    Column(modifier = modifier) {
        // Column header: uppercase label + count chip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
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

        LazyColumn(
            state = lazyListState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(displayTasks, key = { _, it -> it.id }) { _, task ->
                val isDragging = task.id == draggedTaskId
                val isSwiping = task.id == swipingCardId
                val capturedTaskId = task.id
                val counts = subtaskCounts[task.id]

                // Swipe target column — guard all swipeDxState reads with isSwiping
                // so non-swiping items don't recompose on every pointer event.
                val swipeDir = if (isSwiping && swipeDxState.floatValue > 0) 1 else -1
                val colIdx = columns.indexOfFirst { it.id == column.id }
                val swipeTargetCol = if (isSwiping && colIdx >= 0)
                    columns.getOrNull(colIdx + swipeDir)
                else null
                val isPastThreshold = isSwiping && abs(swipeDxState.floatValue) > swipeThresholdPx

                // Spring back when swiping ends; snap to finger while active.
                val swipeTranslationX by animateFloatAsState(
                    targetValue = if (isSwiping) swipeDxState.floatValue * 0.9f else 0f,
                    animationSpec = if (isSwiping) androidx.compose.animation.core.snap()
                                    else spring(dampingRatio = 0.6f, stiffness = 500f),
                    label = "swipeX"
                )

                val itemModifier = if (isDragging) Modifier else Modifier.animateItem()

                Box(modifier = itemModifier) {
                    // Swipe hint — uses matchParentSize() so it always matches the
                    // TaskCard's height exactly regardless of card content.
                    if (isSwiping && swipeTargetCol != null && abs(swipeDxState.floatValue) > 8f) {
                        val accent = MaterialTheme.colorScheme.primary
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isPastThreshold) accent else accent.copy(alpha = 0.22f)
                                ),
                            contentAlignment = if (swipeDir > 0) Alignment.CenterStart
                                              else Alignment.CenterEnd
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isPastThreshold) "→ ${swipeTargetCol.title}"
                                           else swipeTargetCol.title,
                                    color = if (isPastThreshold) MaterialTheme.colorScheme.onPrimary
                                            else accent,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    letterSpacing = 0.08.sp
                                )
                                if (swipeDir > 0) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = if (isPastThreshold) MaterialTheme.colorScheme.onPrimary
                                               else accent
                                    )
                                }
                            }
                        }
                    }

                    TaskCard(
                        task = task,
                        subtaskCount = counts?.total ?: 0,
                        completedSubtaskCount = counts?.completed ?: 0,
                        isDragging = isDragging,
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(if (isDragging) 1f else 0f)
                            .graphicsLayer {
                                alpha = if (isDragging) 0.5f else 1f
                                translationY = if (isDragging) dragOffsetY else 0f
                                translationX = swipeTranslationX
                            }
                            .onSizeChanged { size ->
                                if (size.height > 0) {
                                    cardHeightPx = size.height.toFloat()
                                    cardHeightsState[task.id] = size.height.toFloat()
                                }
                            }
                            .onGloballyPositioned { coords ->
                                cardYsState[task.id] = coords.boundsInRoot().top
                            }
                            // Horizontal swipe: moves card to adjacent column
                            .pointerInput(capturedTaskId, column.id) {
                                detectHorizontalDragGestures(
                                    onDragStart = {
                                        if (draggedTaskId == null) {
                                            swipingCardId = capturedTaskId
                                            swipeDxState.floatValue = 0f
                                        }
                                    },
                                    onDragEnd = {
                                        val dx = swipeDxState.floatValue
                                        val colsSnapshot = columnsRef.value
                                        val curColIdx = colsSnapshot.indexOfFirst { it.id == column.id }
                                        swipingCardId = null
                                        swipeDxState.floatValue = 0f
                                        if (abs(dx) > swipeThresholdPx && curColIdx >= 0) {
                                            val dir = if (dx > 0) 1 else -1
                                            val neighbor = colsSnapshot.getOrNull(curColIdx + dir)
                                            if (neighbor != null) {
                                                onCardMovedToColumnRef.value(capturedTaskId, neighbor.id)
                                            }
                                        }
                                    },
                                    onDragCancel = {
                                        swipingCardId = null
                                        swipeDxState.floatValue = 0f
                                    },
                                    onHorizontalDrag = { change, dragAmount ->
                                        if (draggedTaskId == null && swipingCardId == capturedTaskId) {
                                            swipeDxState.floatValue += dragAmount
                                            change.consume()
                                        }
                                    }
                                )
                            }
                            // Long-press vertical drag: reorder within column
                            .pointerInput(capturedTaskId) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { _ ->
                                        if (swipingCardId == null) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            draggedTaskId = capturedTaskId
                                            dragOffsetY = 0f
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffsetY += dragAmount.y
                                        val fallbackSlotH = if (cardHeightPx > 0f)
                                            cardHeightPx + cardSpacingPx
                                        else with(density) { 80.dp.toPx() }
                                        var curIdx = displayTasksState.value
                                            .indexOfFirst { it.id == capturedTaskId }
                                        if (curIdx < 0) return@detectDragGesturesAfterLongPress

                                        while (curIdx < displayTasksState.value.size - 1) {
                                            val neighborId = displayTasksState.value[curIdx + 1].id
                                            val neighborSlotH = cardHeightsState[neighborId]
                                                ?.plus(cardSpacingPx) ?: fallbackSlotH
                                            if (dragOffsetY <= neighborSlotH / 2f) break
                                            val m = displayTasksState.value.toMutableList()
                                            m.add(curIdx + 1, m.removeAt(curIdx))
                                            displayTasksState.value = m
                                            dragOffsetY -= neighborSlotH
                                            curIdx++
                                        }
                                        while (curIdx > 0) {
                                            val neighborId = displayTasksState.value[curIdx - 1].id
                                            val neighborSlotH = cardHeightsState[neighborId]
                                                ?.plus(cardSpacingPx) ?: fallbackSlotH
                                            if (dragOffsetY >= -neighborSlotH / 2f) break
                                            val m = displayTasksState.value.toMutableList()
                                            m.add(curIdx - 1, m.removeAt(curIdx))
                                            displayTasksState.value = m
                                            dragOffsetY += neighborSlotH
                                            curIdx--
                                        }
                                    },
                                    onDragEnd = {
                                        val current = displayTasksState.value
                                        val idx = current.indexOfFirst { it.id == capturedTaskId }
                                        if (idx >= 0) {
                                            val prevOrder = current.getOrNull(idx - 1)?.order
                                            val nextOrder = current.getOrNull(idx + 1)?.order
                                            onCardReorderedRef.value(
                                                capturedTaskId,
                                                prevOrder ?: ((nextOrder ?: 1.0) - 2.0),
                                                nextOrder ?: ((prevOrder ?: 0.0) + 2.0)
                                            )
                                        }
                                        droppedSuccessfully = true
                                        dragOffsetY = 0f
                                        draggedTaskId = null
                                    },
                                    onDragCancel = {
                                        dragOffsetY = 0f
                                        draggedTaskId = null
                                        displayTasksState.value = currentTasksRef.value
                                    }
                                )
                            },
                        onTap = { onTaskTapped(capturedTaskId) }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showAddCardSheet = true }
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
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
    }

    if (showAddCardSheet) {
        AddCardDialog(
            columns = columns,
            currentColumnId = column.id,
            onAdd = { title, description, reminderAt, style, columnId ->
                onAddCard(title, description, reminderAt, style, columnId)
                showAddCardSheet = false
            },
            onDismiss = { showAddCardSheet = false }
        )
    }
}
