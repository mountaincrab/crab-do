package com.mountaincrab.crabdo.ui.boards

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mountaincrab.crabdo.ui.boards.components.KanbanColumn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KanbanBoardScreen(
    boardId: String,
    navController: NavController,
    onBack: (() -> Unit)? = null,
    viewModel: KanbanBoardViewModel = hiltViewModel<KanbanBoardViewModel, KanbanBoardViewModel.Factory>(
        creationCallback = { factory -> factory.create(boardId) }
    )
) {
    val board by viewModel.board.collectAsStateWithLifecycle()
    val columns by viewModel.columns.collectAsStateWithLifecycle()
    val tasksByColumn by viewModel.tasksByColumn.collectAsStateWithLifecycle()
    var showColumnConfig by remember { mutableStateOf(false) }

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
        LazyRow(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(columns, key = { it.id }) { column ->
                KanbanColumn(
                    column = column,
                    tasks = tasksByColumn[column.id] ?: emptyList(),
                    onCardDropped = { taskId, targetColumnId, orderBefore, orderAfter ->
                        viewModel.moveTask(taskId, targetColumnId, orderBefore, orderAfter)
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
                    }
                )
            }
            item {
                AddColumnButton { viewModel.addColumn(it) }
            }
        }
    }

    if (showColumnConfig) {
        ColumnConfigSheet(
            columns = columns,
            onDismiss = { showColumnConfig = false },
            onRename = { col, title -> viewModel.renameColumn(col, title) },
            onDelete = { viewModel.deleteColumn(it) },
            onReorder = { viewModel.reorderColumns(it) }
        )
    }
}

@Composable
private fun AddColumnButton(onAdd: (String) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .width(200.dp)
            .padding(top = 48.dp),
        contentAlignment = Alignment.TopStart
    ) {
        TextButton(
            onClick = { showDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(4.dp))
            Text("Add column")
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false; title = "" },
            title = { Text("New Column") },
            text = {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Column name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (title.isNotBlank()) onAdd(title.trim())
                    showDialog = false
                    title = ""
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false; title = "" }) { Text("Cancel") }
            }
        )
    }
}
