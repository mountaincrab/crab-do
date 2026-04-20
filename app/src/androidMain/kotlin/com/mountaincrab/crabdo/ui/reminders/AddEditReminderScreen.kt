package com.mountaincrab.crabdo.ui.reminders

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mountaincrab.crabdo.data.local.entity.ReminderEntity
import com.mountaincrab.crabdo.ui.reminders.components.RecurrencePicker
import java.text.SimpleDateFormat
import java.util.*
import java.util.TimeZone
import kotlin.math.abs
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditReminderScreen(
    reminderId: String?,
    fromWidget: Boolean = false,
    navController: NavController,
    viewModel: AddEditReminderViewModel = koinViewModel { parametersOf(reminderId) }
) {
    val activity = LocalContext.current as? Activity
    val isEditing = reminderId != null
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val titleFocusRequester = remember { FocusRequester() }

    // Auto-focus title field when creating a new reminder
    LaunchedEffect(Unit) {
        if (!isEditing) {
            titleFocusRequester.requestFocus()
        }
    }

    val initialCal = remember(viewModel.selectedDateTime) {
        Calendar.getInstance().apply { timeInMillis = viewModel.selectedDateTime }
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = localDateToUtcMidnight(viewModel.selectedDateTime)
    )
    val timePickerState = rememberTimePickerState(
        initialHour = initialCal.get(Calendar.HOUR_OF_DAY),
        initialMinute = initialCal.get(Calendar.MINUTE),
        is24Hour = true
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Reminder" else "New Reminder") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (fromWidget) activity?.finish() else navController.popBackStack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete reminder",
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = viewModel.title,
                onValueChange = { viewModel.title = it },
                label = { Text("Title") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(titleFocusRequester),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, capitalization = KeyboardCapitalization.Sentences)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedCard(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(
                            "Date",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault())
                                .format(Date(viewModel.selectedDateTime)),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                OutlinedCard(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(
                            "Time",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = SimpleDateFormat("HH:mm", Locale.getDefault())
                                .format(Date(viewModel.selectedDateTime)),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Text("Reminder style", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val chipSelectedColors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                )
                FilterChip(
                    selected = viewModel.selectedStyle == ReminderEntity.ReminderStyle.ALARM,
                    onClick = { viewModel.selectedStyle = ReminderEntity.ReminderStyle.ALARM },
                    label = { Text("🔔 Alarm") },
                    colors = chipSelectedColors
                )
                FilterChip(
                    selected = viewModel.selectedStyle == ReminderEntity.ReminderStyle.NOTIFICATION,
                    onClick = { viewModel.selectedStyle = ReminderEntity.ReminderStyle.NOTIFICATION },
                    label = { Text("📳 Notification") },
                    colors = chipSelectedColors
                )
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Repeat", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = viewModel.isRecurring,
                    onCheckedChange = { viewModel.isRecurring = it }
                )
            }

            if (viewModel.isRecurring) {
                RecurrencePicker(
                    rule = viewModel.recurrenceRule,
                    onRuleChanged = { viewModel.recurrenceRule = it }
                )
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = {
                    keyboardController?.hide()
                    viewModel.save {
                        val diffMs = viewModel.selectedDateTime - System.currentTimeMillis()
                        val totalMins = abs(diffMs) / 60_000
                        val hours = totalMins / 60
                        val mins = totalMins % 60
                        val msg = buildString {
                            append("\"${viewModel.title}\" set for ")
                            if (hours > 0) append("${hours}h ")
                            append("${mins}m from now")
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        if (fromWidget) {
                            activity?.finish()
                        } else {
                            navController.popBackStack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                enabled = viewModel.title.isNotBlank()
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Reminder") },
            text = { Text("Are you sure you want to delete this reminder?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete {
                        if (fromWidget) activity?.finish() else navController.popBackStack()
                    }
                    showDeleteConfirm = false
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { dateMillis ->
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = dateMillis
                            val existing = Calendar.getInstance().also { it.timeInMillis = viewModel.selectedDateTime }
                            set(Calendar.HOUR_OF_DAY, existing.get(Calendar.HOUR_OF_DAY))
                            set(Calendar.MINUTE, existing.get(Calendar.MINUTE))
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        viewModel.selectedDateTime = cal.timeInMillis
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            state = timePickerState,
            isKeyboardMode = viewModel.isTimeInputKeyboard,
            onToggleMode = { viewModel.updateTimeInputKeyboard(!viewModel.isTimeInputKeyboard) },
            onConfirm = {
                val cal = Calendar.getInstance().apply {
                    timeInMillis = viewModel.selectedDateTime
                    set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                    set(Calendar.MINUTE, timePickerState.minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                viewModel.selectedDateTime = cal.timeInMillis
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimePickerDialog(
    state: TimePickerState,
    isKeyboardMode: Boolean,
    onToggleMode: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Set time")
                IconButton(onClick = onToggleMode) {
                    Icon(
                        imageVector = if (isKeyboardMode) Icons.Default.Schedule else Icons.Default.Keyboard,
                        contentDescription = if (isKeyboardMode) "Switch to clock" else "Switch to keyboard"
                    )
                }
            }
        },
        text = {
            if (isKeyboardMode) {
                KeyboardTimeInput(state = state)
            } else {
                TimePicker(state = state)
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KeyboardTimeInput(state: TimePickerState) {
    var hourText by remember { mutableStateOf(state.hour.toString().padStart(2, '0')) }
    var minuteText by remember { mutableStateOf(state.minute.toString().padStart(2, '0')) }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 8.dp)
    ) {
        OutlinedTextField(
            value = hourText,
            onValueChange = { v ->
                val digits = v.filter { it.isDigit() }.take(2)
                hourText = digits
                digits.toIntOrNull()?.takeIf { it in 0..23 }?.let { state.hour = it }
            },
            label = { Text("HH") },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineMedium.copy(textAlign = TextAlign.Center)
        )
        Text(":", style = MaterialTheme.typography.headlineLarge)
        OutlinedTextField(
            value = minuteText,
            onValueChange = { v ->
                val digits = v.filter { it.isDigit() }.take(2)
                minuteText = digits
                digits.toIntOrNull()?.takeIf { it in 0..59 }?.let { state.minute = it }
            },
            label = { Text("MM") },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineMedium.copy(textAlign = TextAlign.Center)
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
