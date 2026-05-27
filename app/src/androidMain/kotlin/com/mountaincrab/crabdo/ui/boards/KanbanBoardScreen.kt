package com.mountaincrab.crabdo.ui.boards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mountaincrab.crabdo.data.local.entity.ColumnEntity
import com.mountaincrab.crabdo.data.local.entity.TaskEntity
import com.mountaincrab.crabdo.ui.boards.components.EditCardDialog
import com.mountaincrab.crabdo.ui.boards.components.KanbanColumn
import kotlinx.coroutines.flow.flowOf
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
    val subtaskCounts by viewModel.subtaskCounts.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    var showColumnConfig by remember { mutableStateOf(false) }
    var editingTaskId by remember { mutableStateOf<String?>(null) }

    var activeColumnId by remember { mutableStateOf("") }

    val editingTask = tasksByColumn.values.flatten().find { it.id == editingTaskId }
    val editingSubtasks by remember(editingTaskId) {
        editingTaskId?.let { viewModel.observeSubtasks(it) } ?: flowOf(emptyList())
    }.collectAsStateWithLifecycle(emptyList())

    LaunchedEffect(columns) {
        if (columns.isEmpty()) return@LaunchedEffect
        if (activeColumnId.isEmpty() || columns.none { it.id == activeColumnId }) {
            activeColumnId = columns.first().id
        }
    }

    val activeColumn = columns.find { it.id == activeColumnId }
    val activeTasks = tasksByColumn[activeColumnId] ?: emptyList()
    val taskCounts = columns.associate { it.id to (tasksByColumn[it.id]?.size ?: 0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(board?.title ?: "") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (onBack != null) onBack() else navController.popBackStack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showColumnConfig = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Configure columns")
                    }
                }
            )
        },
        bottomBar = {
            if (columns.isNotEmpty()) {
                ColumnTabs(
                    columns = columns,
                    counts = taskCounts,
                    activeId = activeColumnId,
                    onSelect = { activeColumnId = it }
                )
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (activeColumn != null) {
                KanbanColumn(
                    column = activeColumn,
                    tasks = activeTasks,
                    columns = columns,
                    subtaskCounts = subtaskCounts,
                    onCardReordered = { taskId, orderBefore, orderAfter ->
                        viewModel.moveTask(taskId, activeColumnId, orderBefore, orderAfter)
                    },
                    onCardMovedToColumn = { taskId, toColumnId ->
                        // Stay on the current column — just move the task in the background
                        val targetTasks = tasksByColumn[toColumnId] ?: emptyList()
                        val minOrder = targetTasks.minOfOrNull { it.order } ?: 1.0
                        viewModel.moveTask(taskId, toColumnId, minOrder - 2.0, minOrder)
                    },
                    onTaskTapped = { taskId -> editingTaskId = taskId },
                    onAddCard = { title, description, reminderAt, style, columnId ->
                        viewModel.createTask(columnId, title, description, reminderAt, style)
                        if (columnId != activeColumnId) activeColumnId = columnId
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    editingTask?.let { task ->
        EditCardDialog(
            task = task,
            columns = columns,
            subtasks = editingSubtasks,
            onSave = { title, description, reminderAt, style, columnId ->
                viewModel.saveTaskChanges(task.id, title, description, reminderAt, style, columnId)
                editingTaskId = null
            },
            onDelete = {
                viewModel.deleteTask(task.id)
                editingTaskId = null
            },
            onAddSubtask = { title -> viewModel.addSubtask(task.id, title) },
            onToggleSubtask = { id, completed -> viewModel.toggleSubtask(id, completed) },
            onDeleteSubtask = { id -> viewModel.deleteSubtask(id) },
            onRenameSubtask = { id, title -> viewModel.renameSubtask(id, title) },
            onReorderSubtask = { id, before, after -> viewModel.reorderSubtask(id, before, after) },
            onDismiss = { editingTaskId = null }
        )
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

@Composable
private fun ColumnTabs(
    columns: List<ColumnEntity>,
    counts: Map<String, Int>,
    activeId: String,
    onSelect: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            columns.forEach { col ->
                val isActive = col.id == activeId
                val count = counts[col.id] ?: 0
                Surface(
                    onClick = { onSelect(col.id) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = if (isActive)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    else
                        Color.Transparent,
                    border = if (isActive)
                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    else
                        null,
                ) {
                    Column(
                        modifier = Modifier.padding(top = 10.dp, start = 8.dp, end = 8.dp, bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = col.title,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            letterSpacing = (-0.2).sp,
                            color = if (isActive) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = count.toString(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.06.sp,
                            color = if (isActive) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                    }
                }
            }
        }
    }
}
