package com.mountaincrab.crabdo.ui.boards

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessAlarm
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.getSelectedText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mountaincrab.crabdo.data.local.entity.TaskEntity
import com.mountaincrab.crabdo.ui.boards.components.SubtaskItem
import com.mountaincrab.crabdo.ui.util.AddLinkDialog
import com.mountaincrab.crabdo.ui.util.insertMarkdownLink
import com.mountaincrab.crabdo.ui.reminders.ReminderTimePickerDialog
import com.mountaincrab.crabdo.ui.theme.Eyebrow
import com.mountaincrab.crabdo.ui.theme.PillButton
import com.mountaincrab.crabdo.ui.theme.PillGroup
import java.text.SimpleDateFormat
import java.util.*
import java.util.TimeZone
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: String,
    navController: NavController,
    viewModel: TaskDetailViewModel = koinViewModel { parametersOf(taskId) }
) {
    val task by viewModel.task.collectAsStateWithLifecycle()
    val subtasks by viewModel.subtasks.collectAsStateWithLifecycle()

    var titleText by remember(task?.title) { mutableStateOf(TextFieldValue(task?.title ?: "")) }
    var descriptionText by remember(task?.description) { mutableStateOf(TextFieldValue(task?.description ?: "")) }
    var showTitleLinkDialog by remember { mutableStateOf(false) }
    var showDescLinkDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showAddSubtaskSheet by remember { mutableStateOf(false) }
    var dragList by remember { mutableStateOf(subtasks) }
    var draggingSubtaskId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var itemHeightPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val subtaskSpacingPx = with(density) { 8.dp.toPx() }
    val haptic = LocalHapticFeedback.current

    // Keep dragList in sync with the ViewModel when not mid-drag.
    LaunchedEffect(subtasks) {
        if (draggingSubtaskId == null) dragList = subtasks
    }

    val onBack = {
        if (titleText.text.isNotBlank()) {
            viewModel.updateTitle(titleText.text.trim())
            viewModel.updateDescription(descriptionText.text.trim())
        }
        navController.popBackStack()
        Unit
    }

    // Save title/description when the user presses the system back gesture,
    // but only when no dialog or sheet is intercepting (those BackHandlers are
    // composed later and therefore take priority over this one).
    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Task") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                Eyebrow("Title")
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    placeholder = { Text("Task title") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { if (!it.isFocused && titleText.text.isNotBlank()) viewModel.updateTitle(titleText.text.trim()) },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { showTitleLinkDialog = true }) {
                            Icon(Icons.Default.Link, contentDescription = "Add link",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Eyebrow("Description")
                    TextButton(onClick = { showDescLinkDialog = true }) {
                        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add link", style = MaterialTheme.typography.labelMedium)
                    }
                }
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = descriptionText,
                    onValueChange = { descriptionText = it },
                    placeholder = { Text("Add details or notes…") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { if (!it.isFocused) viewModel.updateDescription(descriptionText.text.trim()) },
                    minLines = 3,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
            }
            item {
                HorizontalDivider()
                Spacer(Modifier.height(4.dp))
                Eyebrow("Reminder")
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
                    Eyebrow("Checklist", modifier = Modifier.weight(1f))
                    TextButton(onClick = { showAddSubtaskSheet = true }) {
                        Icon(Icons.Default.Add, contentDescription = null,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add subtask")
                    }
                }
            }
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    dragList.forEach { subtask ->
                        key(subtask.id) {
                            val isDragging = subtask.id == draggingSubtaskId
                            SubtaskItem(
                                subtask = subtask,
                                onToggle = { viewModel.toggleSubtask(subtask.id, it) },
                                onDelete = { viewModel.deleteSubtask(subtask.id) },
                                onRename = { viewModel.renameSubtask(subtask.id, it) },
                                modifier = Modifier
                                    .zIndex(if (isDragging) 1f else 0f)
                                    .graphicsLayer {
                                        translationY = if (isDragging) dragOffsetY else 0f
                                        alpha = if (isDragging) 0.85f else 1f
                                    }
                                    .onSizeChanged { size ->
                                        if (size.height > 0) itemHeightPx = size.height.toFloat()
                                    }
                                    .pointerInput(subtask.id) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = { _ ->
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                draggingSubtaskId = subtask.id
                                                dragOffsetY = 0f
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragOffsetY += dragAmount.y
                                                val slotH = if (itemHeightPx > 0f)
                                                    itemHeightPx + subtaskSpacingPx
                                                else with(density) { 56.dp.toPx() }
                                                var curIdx = dragList.indexOfFirst { it.id == subtask.id }
                                                while (dragOffsetY > slotH / 2f && curIdx < dragList.size - 1) {
                                                    val m = dragList.toMutableList()
                                                    m.add(curIdx + 1, m.removeAt(curIdx))
                                                    dragList = m
                                                    dragOffsetY -= slotH
                                                    curIdx++
                                                }
                                                while (dragOffsetY < -slotH / 2f && curIdx > 0) {
                                                    val m = dragList.toMutableList()
                                                    m.add(curIdx - 1, m.removeAt(curIdx))
                                                    dragList = m
                                                    dragOffsetY += slotH
                                                    curIdx--
                                                }
                                            },
                                            onDragEnd = {
                                                val id = draggingSubtaskId
                                                draggingSubtaskId = null
                                                dragOffsetY = 0f
                                                if (id != null) {
                                                    val finalList = dragList
                                                    val finalIdx = finalList.indexOfFirst { it.id == id }
                                                    if (finalIdx >= 0) {
                                                        val prevOrder = finalList.getOrNull(finalIdx - 1)?.order
                                                        val nextOrder = finalList.getOrNull(finalIdx + 1)?.order
                                                        viewModel.reorderSubtask(
                                                            id,
                                                            prevOrder ?: ((nextOrder ?: 1.0) - 2.0),
                                                            nextOrder ?: ((prevOrder ?: 0.0) + 2.0)
                                                        )
                                                    }
                                                }
                                            },
                                            onDragCancel = {
                                                draggingSubtaskId = null
                                                dragOffsetY = 0f
                                                dragList = subtasks
                                            }
                                        )
                                    }
                            )
                        }
                    }
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

    if (showTitleLinkDialog) {
        AddLinkDialog(
            initialLabel = titleText.getSelectedText().text,
            onDismiss = { showTitleLinkDialog = false },
            onConfirm = { label, url ->
                titleText = insertMarkdownLink(titleText, label, url)
                showTitleLinkDialog = false
            },
        )
    }
    if (showDescLinkDialog) {
        AddLinkDialog(
            initialLabel = descriptionText.getSelectedText().text,
            onDismiss = { showDescLinkDialog = false },
            onConfirm = { label, url ->
                descriptionText = insertMarkdownLink(descriptionText, label, url)
                showDescLinkDialog = false
            },
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
                PillGroup {
                    PillButton(
                        selected = selectedStyle == TaskEntity.ReminderStyle.NOTIFICATION,
                        onClick = { selectedStyle = TaskEntity.ReminderStyle.NOTIFICATION },
                        icon = Icons.Default.Notifications,
                        text = "Notification",
                    )
                    PillButton(
                        selected = selectedStyle == TaskEntity.ReminderStyle.ALARM,
                        onClick = { selectedStyle = TaskEntity.ReminderStyle.ALARM },
                        icon = Icons.Default.AccessAlarm,
                        text = "Alarm",
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
        ReminderTimePickerDialog(
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

    // Add the current subtask but keep the sheet + keyboard up so the user can
    // type the next one straight away. Re-requesting focus and re-showing the
    // keyboard counteracts the IME "Done" action (and the icon tap) dismissing
    // the keyboard / stealing focus.
    val addAndContinue = {
        if (text.isNotBlank()) {
            onAdd(text.trim())
            text = ""
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

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
                keyboardActions = KeyboardActions(onDone = { addAndContinue() })
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = addAndContinue,
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

private fun localDateToUtcMidnight(localMillis: Long): Long {
    val local = Calendar.getInstance()
    local.timeInMillis = localMillis
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
