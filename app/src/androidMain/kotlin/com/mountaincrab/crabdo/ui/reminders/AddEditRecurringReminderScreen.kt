package com.mountaincrab.crabdo.ui.reminders

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mountaincrab.crabdo.data.local.entity.ReminderStyle
import com.mountaincrab.crabdo.ui.reminders.components.RecurrencePicker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRecurringReminderScreen(
    reminderId: String?,
    fromWidget: Boolean = false,
    navController: NavController,
    viewModel: AddEditRecurringReminderViewModel = koinViewModel { parametersOf(reminderId) }
) {
    val activity = LocalContext.current as? Activity
    val isEditing = reminderId != null
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val titleFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (!isEditing) titleFocusRequester.requestFocus()
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
                title = { Text(if (isEditing) "Edit Recurring Reminder" else "New Recurring Reminder") },
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
                modifier = Modifier.fillMaxWidth().focusRequester(titleFocusRequester),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, capitalization = KeyboardCapitalization.Sentences)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedCard(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(
                            "Starting from",
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
                OutlinedCard(onClick = { showTimePicker = true }, modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text("Time", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    selected = viewModel.selectedStyle == ReminderStyle.ALARM,
                    onClick = { viewModel.selectedStyle = ReminderStyle.ALARM },
                    label = { Text("🔔 Alarm") },
                    colors = chipSelectedColors
                )
                FilterChip(
                    selected = viewModel.selectedStyle == ReminderStyle.NOTIFICATION,
                    onClick = { viewModel.selectedStyle = ReminderStyle.NOTIFICATION },
                    label = { Text("📳 Notification") },
                    colors = chipSelectedColors
                )
            }

            RecurrencePicker(
                rule = viewModel.recurrenceRule,
                onRuleChanged = { viewModel.recurrenceRule = it }
            )

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = {
                    keyboardController?.hide()
                    viewModel.save {
                        val msg = "\"${viewModel.title}\" recurring reminder saved"
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        if (fromWidget) activity?.finish() else navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                enabled = viewModel.title.isNotBlank() && viewModel.recurrenceRule != null
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
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        ReminderTimePickerDialog(
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
