package com.mountaincrab.crabdo.ui.boards

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mountaincrab.crabdo.data.local.entity.TaskEntity
import com.mountaincrab.crabdo.ui.boards.components.SubtaskItem
import com.mountaincrab.crabdo.ui.reminders.TimePickerDialog
import java.text.SimpleDateFormat
import java.util.*
import java.util.TimeZone
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TaskDetailScreen(
    taskId: String,
    navController: NavController,
    viewModel: TaskDetailViewModel = koinViewModel { parametersOf(taskId) }
) {
    val task by viewModel.task.collectAsStateWithLifecycle()
    val subtasks by viewModel.subtasks.collectAsStateWithLifecycle()

    var titleText by remember(task?.title) { mutableStateOf(task?.title ?: "") }
    var descriptionText by remember(task?.description) { mutableStateOf(task?.description ?: "") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showAddSubtaskSheet by remember { mutableStateOf(false) }
    var draggedSubtaskId by remember { mutableStateOf<String?>(null) }
    var subtaskHoverIndex by remember { mutableStateOf<Int?>(null) }
    val visibleSubtasks = remember(subtasks, draggedSubtaskId) {
        subtasks.filter { it.id != draggedSubtaskId }
    }

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
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
            }
            item {
                OutlinedTextField(
                    value = descriptionText,
                    onValueChange = { descriptionText = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Checklist",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { showAddSubtaskSheet = true }) {
                        Icon(Icons.Default.Add, contentDescription = null,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add subtask")
                    }
                }
            }
            itemsIndexed(visibleSubtasks, key = { _, s -> s.id }) { index, subtask ->
                val insertBeforeTarget = remember(subtask.id, index, visibleSubtasks) {
                    object : DragAndDropTarget {
                        override fun onDrop(event: DragAndDropEvent): Boolean {
                            val draggedId = event.toAndroidDragEvent()
                                .clipData?.getItemAt(0)?.text?.toString() ?: return false
                            if (draggedId == subtask.id) return false
                            val prevOrder = visibleSubtasks.getOrNull(index - 1)?.order
                                ?: (subtask.order - 2.0)
                            viewModel.reorderSubtask(draggedId, prevOrder, subtask.order)
                            subtaskHoverIndex = null
                            draggedSubtaskId = null
                            return true
                        }
                        override fun onEntered(event: DragAndDropEvent) { subtaskHoverIndex = index }
                        override fun onEnded(event: DragAndDropEvent) { subtaskHoverIndex = null }
                    }
                }
                Column {
                    SubtaskDropIndicator(visible = subtaskHoverIndex == index)
                    SubtaskItem(
                        subtask = subtask,
                        onToggle = { viewModel.toggleSubtask(subtask.id, it) },
                        onDelete = { viewModel.deleteSubtask(subtask.id) },
                        onRename = { viewModel.renameSubtask(subtask.id, it) },
                        modifier = Modifier
                            .dragAndDropTarget(
                                shouldStartDragAndDrop = { true },
                                target = insertBeforeTarget
                            )
                            .dragAndDropSource {
                                val dragSource = this
                                awaitPointerEventScope {
                                    // Initial pass intercepts before Checkbox/clickable children
                                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                                    val released = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                                        waitForUpOrCancellation()
                                    }
                                    if (released == null) {
                                        draggedSubtaskId = subtask.id
                                        dragSource.startTransfer(
                                            DragAndDropTransferData(
                                                clipData = android.content.ClipData.newPlainText(
                                                    "subtaskId", subtask.id
                                                )
                                            )
                                        )
                                    }
                                }
                            }
                    )
                }
            }
            // Trailing drop target: append after all subtasks
            item {
                val appendTarget = remember(visibleSubtasks) {
                    object : DragAndDropTarget {
                        override fun onDrop(event: DragAndDropEvent): Boolean {
                            val draggedId = event.toAndroidDragEvent()
                                .clipData?.getItemAt(0)?.text?.toString() ?: return false
                            val maxOrder = visibleSubtasks.maxOfOrNull { it.order } ?: 0.0
                            viewModel.reorderSubtask(draggedId, maxOrder, maxOrder + 2.0)
                            subtaskHoverIndex = null
                            draggedSubtaskId = null
                            return true
                        }
                        override fun onEntered(event: DragAndDropEvent) {
                            subtaskHoverIndex = visibleSubtasks.size
                        }
                        override fun onEnded(event: DragAndDropEvent) { subtaskHoverIndex = null }
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .dragAndDropTarget(shouldStartDragAndDrop = { true }, target = appendTarget)
                ) {
                    SubtaskDropIndicator(
                        visible = subtaskHoverIndex == visibleSubtasks.size && draggedSubtaskId != null
                    )
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
            isTimeInputKeyboard = viewModel.isTimeInputKeyboard,
            onToggleTimeInputMode = { viewModel.updateTimeInputKeyboard(!viewModel.isTimeInputKeyboard) },
            onConfirm = { millis, style ->
                viewModel.setReminder(millis, style)
                showReminderDialog = false
            },
            onDismiss = { showReminderDialog = false }
        )
    }

    if (showAddSubtaskSheet) {
        AddSubtaskSheet(
            onAdd = { title -> viewModel.addSubtask(title) },
            onDismiss = { showAddSubtaskSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskReminderDialog(
    currentStyle: TaskEntity.ReminderStyle,
    isTimeInputKeyboard: Boolean,
    onToggleTimeInputMode: () -> Unit,
    onConfirm: (Long, TaskEntity.ReminderStyle) -> Unit,
    onDismiss: () -> Unit
) {
    val defaultMillis = System.currentTimeMillis() + 3_600_000L
    var selectedStyle by remember { mutableStateOf(currentStyle) }
    var selectedMillis by remember { mutableStateOf(defaultMillis) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val initialCal = remember { Calendar.getInstance().apply { timeInMillis = defaultMillis } }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = localDateToUtcMidnight(defaultMillis)
    )
    val timePickerState = rememberTimePickerState(
        initialHour = initialCal.get(Calendar.HOUR_OF_DAY),
        initialMinute = initialCal.get(Calendar.MINUTE),
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Reminder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedCard(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Date", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
                                    .format(Date(selectedMillis)),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    OutlinedCard(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Time", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = SimpleDateFormat("HH:mm", Locale.getDefault())
                                    .format(Date(selectedMillis)),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
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
            TextButton(onClick = { onConfirm(selectedMillis, selectedStyle) }) { Text("Set") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { dateMillis ->
                        val existing = Calendar.getInstance().also { it.timeInMillis = selectedMillis }
                        selectedMillis = Calendar.getInstance().apply {
                            timeInMillis = dateMillis
                            set(Calendar.HOUR_OF_DAY, existing.get(Calendar.HOUR_OF_DAY))
                            set(Calendar.MINUTE, existing.get(Calendar.MINUTE))
                            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        TimePickerDialog(
            state = timePickerState,
            isKeyboardMode = isTimeInputKeyboard,
            onToggleMode = onToggleTimeInputMode,
            onConfirm = {
                selectedMillis = Calendar.getInstance().apply {
                    timeInMillis = selectedMillis
                    set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                    set(Calendar.MINUTE, timePickerState.minute)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSubtaskSheet(
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Single back press closes both the keyboard and the sheet
    BackHandler(enabled = true) {
        keyboardController?.hide()
        onDismiss()
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .imePadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Subtask name") },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, capitalization = KeyboardCapitalization.Sentences),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (text.isNotBlank()) {
                            onAdd(text.trim())
                            text = ""
                        }
                    }
                )
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (text.isNotBlank()) {
                        onAdd(text.trim())
                        text = ""
                    }
                },
                enabled = text.isNotBlank()
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Add subtask",
                    tint = if (text.isNotBlank()) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
        }
    }
}

@Composable
private fun SubtaskDropIndicator(visible: Boolean) {
    if (visible) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 1.dp)
                .height(2.dp)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

private fun localDateToUtcMidnight(localMillis: Long): Long {
    val local = Calendar.getInstance()
    local.timeInMillis = localMillis
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
