package com.mountaincrab.crabdo.ui.boards

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mountaincrab.crabdo.ui.boards.components.KanbanColumn
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
            val leftEdgeTarget = remember {
                object : DragAndDropTarget {
                    override fun onDrop(event: DragAndDropEvent) = false
                    override fun onEntered(event: DragAndDropEvent) { edgeScrollState.intValue = -1 }
                    override fun onExited(event: DragAndDropEvent) { edgeScrollState.intValue = 0 }
                    override fun onEnded(event: DragAndDropEvent) { edgeScrollState.intValue = 0 }
                }
            }
            val rightEdgeTarget = remember {
                object : DragAndDropTarget {
                    override fun onDrop(event: DragAndDropEvent) = false
                    override fun onEntered(event: DragAndDropEvent) { edgeScrollState.intValue = 1 }
                    override fun onExited(event: DragAndDropEvent) { edgeScrollState.intValue = 0 }
                    override fun onEnded(event: DragAndDropEvent) { edgeScrollState.intValue = 0 }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                LazyRow(
                    state = lazyRowState,
                    flingBehavior = snapBehavior,
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    items(columns, key = { it.id }) { column ->
                        KanbanColumn(
                            column = column,
                            tasks = tasksByColumn[column.id] ?: emptyList(),
                            draggedTaskId = draggedTaskId,
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
                            onReorder = { taskId, orderBefore, orderAfter ->
                                viewModel.moveTask(taskId, column.id, orderBefore, orderAfter)
                            },
                            modifier = Modifier.fillParentMaxWidth(0.92f)
                        )
                    }
                }
                // Edge scroll zones — always present so they're in the view hierarchy at drag start
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(56.dp)
                        .align(Alignment.CenterStart)
                        .dragAndDropTarget(shouldStartDragAndDrop = { true }, target = leftEdgeTarget)
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(56.dp)
                        .align(Alignment.CenterEnd)
                        .dragAndDropTarget(shouldStartDragAndDrop = { true }, target = rightEdgeTarget)
                )
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
