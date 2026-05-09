package com.mountaincrab.crabdo.ui.boards

import androidx.compose.ui.geometry.Rect
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
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
    val sourceColumnId: String? = if (draggedTaskId == null) null
        else tasksByColumn.entries.firstOrNull { (_, ts) -> ts.any { it.id == draggedTaskId } }?.key
    val ghostColumnId = remember { mutableStateOf<String?>(null) }
    // Absolute finger Y in root coords during a drag. Source column writes it
    // every onDrag tick; target column observes it to reorder the ghost.
    val dragFingerYAbs = remember { mutableFloatStateOf(0f) }
    // Order bracket for the ghost's current slot in the target column. Written
    // by the target's reorder effect; read by the source's onDragEnd to drop
    // at the right slot instead of appending to the end.
    val ghostOrderBracket = remember { mutableStateOf<Pair<Double, Double>?>(null) }
    val columnBoundsMap = remember { mutableStateMapOf<String, Rect>() }
    LaunchedEffect(draggedTaskId) {
        if (draggedTaskId == null) {
            ghostColumnId.value = null
            ghostOrderBracket.value = null
        }
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
            val pagerState = rememberPagerState(pageCount = { columns.size })

            val edgeScrollState = remember { mutableIntStateOf(0) }
            // While the finger is at a screen edge, keep advancing the pager one page
            // at a time. The 700ms delay between scrolls is what gives the user a
            // chance to release; if they're still at the edge after the page settles,
            // we scroll again.
            LaunchedEffect(edgeScrollState.intValue) {
                while (edgeScrollState.intValue != 0) {
                    delay(700)
                    if (edgeScrollState.intValue == 0) break
                    val current = pagerState.currentPage
                    val target = (current + edgeScrollState.intValue)
                        .coerceIn(0, columns.size - 1)
                    if (target == current) break  // already at edge column
                    pagerState.animateScrollToPage(target)
                }
            }

            // When the pager scrolls *during* a drag, set ghostColumnId to the new
            // centered column. This is the fix for: user drags to right edge, pager
            // scrolls to next column, but the user's finger is still at the screen
            // edge (outside any column's bounds), so the in-onDrag findColumnIdAt
            // lookup returns null and never sets the ghost. Driving the ghost from
            // pager state means the dragged task visibly "follows" into the new column.
            //
            // We snapshot the page at drag start and only react to *changes* from
            // there — otherwise long-pressing a peeking card (sourceColumn != centered)
            // would immediately spawn a ghost in the centered column before the user
            // has dragged anywhere.
            val dragStartPage = remember(draggedTaskId) { pagerState.currentPage }
            LaunchedEffect(pagerState.currentPage, draggedTaskId, sourceColumnId) {
                if (draggedTaskId != null && pagerState.currentPage != dragStartPage) {
                    val centeredColumnId = columns.getOrNull(pagerState.currentPage)?.id
                    if (centeredColumnId != null) {
                        ghostColumnId.value = if (centeredColumnId == sourceColumnId) null
                                              else centeredColumnId
                    }
                }
            }

            // HorizontalPager with beyondViewportPageCount keeps all column composables
            // in composition even when off-screen, so detectDragGesturesAfterLongPress
            // is never cancelled by the edge scroll moving the source column out of view.
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = columns.size,
                modifier = Modifier.fillMaxSize(),
                pageSpacing = 8.dp,
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                key = { index -> columns.getOrNull(index)?.id ?: index },
            ) { pageIndex ->
                val column = columns[pageIndex]
                val columnTasks = tasksByColumn[column.id] ?: emptyList()
                KanbanColumn(
                    column = column,
                    tasks = columnTasks,
                    draggedTaskId = draggedTaskId,
                    foreignDraggedTask = if (columnTasks.any { it.id == draggedTaskId }) null else draggedTask,
                    ghostColumnId = ghostColumnId,
                    dragFingerYAbs = dragFingerYAbs,
                    ghostOrderBracket = ghostOrderBracket,
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
                    modifier = Modifier.fillMaxWidth()
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
