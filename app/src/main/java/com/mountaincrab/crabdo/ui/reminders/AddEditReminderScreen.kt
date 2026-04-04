package com.mountaincrab.crabdo.ui.reminders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.mountaincrab.crabdo.data.local.entity.ReminderEntity
import com.mountaincrab.crabdo.ui.reminders.components.RecurrencePicker
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditReminderScreen(
    reminderId: String?,
    navController: NavController,
    viewModel: AddEditReminderViewModel = hiltViewModel()
) {
    val isEditing = reminderId != null
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = viewModel.selectedDateTime
    )
    val timePickerState = rememberTimePickerState(
        initialHour = Calendar.getInstance().apply {
            timeInMillis = viewModel.selectedDateTime
        }.get(Calendar.HOUR_OF_DAY),
        initialMinute = Calendar.getInstance().apply {
            timeInMillis = viewModel.selectedDateTime
        }.get(Calendar.MINUTE)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Reminder" else "New Reminder") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            OutlinedTextField(
                value = viewModel.title,
                onValueChange = { viewModel.title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Date & Time
            OutlinedCard(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Date & Time", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = SimpleDateFormat("EEE, d MMM yyyy 'at' HH:mm", Locale.getDefault())
                            .format(Date(viewModel.selectedDateTime)),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            // Style toggle
            Text("Reminder style", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = viewModel.selectedStyle == ReminderEntity.ReminderStyle.ALARM,
                    onClick = { viewModel.selectedStyle = ReminderEntity.ReminderStyle.ALARM },
                    label = { Text("🔔 Alarm") }
                )
                FilterChip(
                    selected = viewModel.selectedStyle == ReminderEntity.ReminderStyle.NOTIFICATION,
                    onClick = { viewModel.selectedStyle = ReminderEntity.ReminderStyle.NOTIFICATION },
                    label = { Text("📳 Notification") }
                )
            }

            // Recurrence toggle
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Repeat", style = MaterialTheme.typography.bodyLarge)
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

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    viewModel.save { navController.popBackStack() }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = viewModel.title.isNotBlank()
            ) {
                Text("Save")
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { dateMillis ->
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = dateMillis
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        viewModel.selectedDateTime = cal.timeInMillis
                    }
                    showDatePicker = false
                    showTimePicker = true
                }) { Text("Next") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Pick a time") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = viewModel.selectedDateTime
                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(Calendar.MINUTE, timePickerState.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    viewModel.selectedDateTime = cal.timeInMillis
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }
}
