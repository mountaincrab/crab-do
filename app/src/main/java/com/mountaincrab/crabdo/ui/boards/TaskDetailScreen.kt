package com.mountaincrab.crabdo.ui.boards

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mountaincrab.crabdo.data.local.entity.TaskEntity
import com.mountaincrab.crabdo.ui.boards.components.SubtaskItem
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: String,
    navController: NavController,
    viewModel: TaskDetailViewModel = hiltViewModel()
) {
    val task by viewModel.task.collectAsStateWithLifecycle()
    val subtasks by viewModel.subtasks.collectAsStateWithLifecycle()

    var titleText by remember(task?.title) { mutableStateOf(task?.title ?: "") }
    var descriptionText by remember(task?.description) { mutableStateOf(task?.description ?: "") }
    var newSubtaskTitle by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Task") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete task",
                            tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    value = descriptionText,
                    onValueChange = { descriptionText = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = {
                        if (titleText.isNotBlank()) {
                            viewModel.updateTitle(titleText.trim())
                            viewModel.updateDescription(descriptionText.trim())
                        }
                    }) { Text("Save changes") }
                }
            }
            item {
                HorizontalDivider()
                Spacer(Modifier.height(4.dp))
                Text("Reminder", style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                task?.reminderTimeMillis?.let { millis ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault())
                                .format(Date(millis)),
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.clearReminder() }) {
                            Icon(Icons.Default.NotificationsOff, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Clear")
                        }
                    }
                } ?: run {
                    TextButton(onClick = { showReminderDialog = true }) {
                        Icon(Icons.Default.Notifications, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Set reminder")
                    }
                }
            }
            item {
                HorizontalDivider()
                Spacer(Modifier.height(4.dp))
                Text("Checklist", style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(subtasks, key = { it.id }) { subtask ->
                SubtaskItem(
                    subtask = subtask,
                    onToggle = { viewModel.toggleSubtask(subtask.id, it) },
                    onDelete = { viewModel.deleteSubtask(subtask.id) }
                )
            }
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newSubtaskTitle,
                        onValueChange = { newSubtaskTitle = it },
                        placeholder = { Text("Add item") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            if (newSubtaskTitle.isNotBlank()) {
                                viewModel.addSubtask(newSubtaskTitle.trim())
                                newSubtaskTitle = ""
                            }
                        }
                    ) { Text("Add") }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Task") },
            text = { Text("Are you sure you want to delete this task?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTask { navController.popBackStack() }
                    showDeleteConfirm = false
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showReminderDialog) {
        TaskReminderDialog(
            currentStyle = task?.reminderStyle ?: TaskEntity.ReminderStyle.ALARM,
            onConfirm = { millis, style ->
                viewModel.setReminder(millis, style)
                showReminderDialog = false
            },
            onDismiss = { showReminderDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskReminderDialog(
    currentStyle: TaskEntity.ReminderStyle,
    onConfirm: (Long, TaskEntity.ReminderStyle) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedStyle by remember { mutableStateOf(currentStyle) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis() + 3_600_000L
    )
    val timePickerState = rememberTimePickerState(
        initialHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
        initialMinute = Calendar.getInstance().get(Calendar.MINUTE)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Reminder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DatePicker(state = datePickerState)
                TimePicker(state = timePickerState)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedStyle == TaskEntity.ReminderStyle.ALARM,
                        onClick = { selectedStyle = TaskEntity.ReminderStyle.ALARM },
                        label = { Text("🔔 Alarm") }
                    )
                    FilterChip(
                        selected = selectedStyle == TaskEntity.ReminderStyle.NOTIFICATION,
                        onClick = { selectedStyle = TaskEntity.ReminderStyle.NOTIFICATION },
                        label = { Text("📳 Notification") }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val dateMillis = datePickerState.selectedDateMillis ?: return@TextButton
                val cal = Calendar.getInstance().apply {
                    timeInMillis = dateMillis
                    set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                    set(Calendar.MINUTE, timePickerState.minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                onConfirm(cal.timeInMillis, selectedStyle)
            }) { Text("Set") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
