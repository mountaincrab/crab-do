package com.mountaincrab.crabdo.ui.boards

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.ui.geometry.Rect
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mountaincrab.crabdo.data.local.entity.TaskEntity
import com.mountaincrab.crabdo.ui.boards.components.KanbanColumn
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KanbanBoardScreen(
    boardId: String,
    navController: NavController,
    onBack: (() -> Unit)? = null,
    viewModel: KanbanBoardViewModel = koinViewModel { parametersOf(boardId) }
) {
    val board by viewModel.board.collectAsStateWithLifecycle()
    val columns by viewModel.columns.collectAsStateWithLifecycle()
    val tasksByColumn by viewModel.tasksByColumn.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    var showColumnConfig by remember { mutableStateOf(false) }
    var draggedTaskId by remember { mutableStateOf<String?>(null) }
    val draggedTask: TaskEntity? = if (draggedTaskId == null) null
        else tasksByColumn.values.flatten().firstOrNull { it.id == draggedTaskId }
    val ghostColumnId = remember { mutableStateOf<String?>(null) }
    val columnBoundsMap = remember { mutableStateMapOf<String, Rect>() }
    LaunchedEffect(draggedTaskId) {
        if (draggedTaskId == null) ghostColumnId.value = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(board?.title ?: "") },
                navigationIcon = {
                    IconButton(onClick = { if (onBack != null) onBack() else navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showColumnConfig = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Configure columns")
                    }
                }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val lazyRowState = rememberLazyListState()
            val snapBehavior = rememberSnapFlingBehavior(lazyRowState)

            val edgeScrollState = remember { mutableIntStateOf(0) }
            LaunchedEffect(edgeScrollState.intValue) {
                if (edgeScrollState.intValue != 0) {
                    delay(700)
                    if (edgeScrollState.intValue != 0) {
                        val target = (lazyRowState.firstVisibleItemIndex + edgeScrollState.intValue).coerceAtLeast(0)
                        lazyRowState.animateScrollToItem(target)
                    }
                }
            }

            LazyRow(
                state = lazyRowState,
                flingBehavior = snapBehavior,
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
            ) {
                items(columns, key = { it.id }) { column ->
                    val columnTasks = tasksByColumn[column.id] ?: emptyList()
                    KanbanColumn(
                        column = column,
                        tasks = columnTasks,
                        draggedTaskId = draggedTaskId,
                        foreignDraggedTask = if (columnTasks.any { it.id == draggedTaskId }) null else draggedTask,
                        ghostColumnId = ghostColumnId,
                        findColumnIdAt = { x ->
                            columnBoundsMap.entries.firstOrNull { (_, r) -> x >= r.left && x <= r.right }?.key
                        },
                        onBoundsChanged = { rect -> columnBoundsMap[column.id] = rect },
                        edgeScrollState = edgeScrollState,
                        onDragStart = { draggedTaskId = it },
                        onDragEnd = { draggedTaskId = null },
                        onCardDropped = { taskId, targetColumnId, orderBefore, orderAfter ->
                            viewModel.moveTask(taskId, targetColumnId, orderBefore, orderAfter)
                            draggedTaskId = null
                        },
                        onCardTapped = { taskId ->
                            navController.navigate(
                                com.mountaincrab.crabdo.ui.navigation.Screen.TaskDetail.createRoute(taskId)
                            )
                        },
                        onAddCard = { title, description, reminderAt, style ->
                            viewModel.createTask(column.id, title, description, reminderAt, style)
                        },
                        allTasksByColumn = tasksByColumn,
                        modifier = Modifier.fillParentMaxWidth(0.92f)
                    )
                }
            }
        }
    }

    if (showColumnConfig) {
        ColumnConfigSheet(
            columns = columns,
            onDismiss = { showColumnConfig = false },
            onRename = { col, title -> viewModel.renameColumn(col, title) },
            onDelete = { viewModel.deleteColumn(it) },
            onReorder = { viewModel.reorderColumns(it) },
            onAdd = { viewModel.addColumn(it) }
        )
    }
}
