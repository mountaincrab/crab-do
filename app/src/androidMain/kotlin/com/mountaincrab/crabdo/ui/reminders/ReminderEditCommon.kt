package com.mountaincrab.crabdo.ui.reminders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReminderTimePickerDialog(
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
            if (isKeyboardMode) KeyboardTimeInput(state = state)
            else TimePicker(state = state)
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
    val hourFocusRequester = remember { FocusRequester() }
    val minuteFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        hourFocusRequester.requestFocus()
        keyboardController?.show()
    }

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
                if (digits.length == 2) minuteFocusRequester.requestFocus()
            },
            label = { Text("HH") },
            modifier = Modifier.weight(1f).focusRequester(hourFocusRequester)
                .onFocusChanged { if (it.isFocused) hourText = "" },
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
            modifier = Modifier.weight(1f).focusRequester(minuteFocusRequester)
                .onFocusChanged { if (it.isFocused) minuteText = "" },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineMedium.copy(textAlign = TextAlign.Center)
        )
    }
}

internal fun localDateToUtcMidnight(localMillis: Long): Long {
    val local = Calendar.getInstance()
    local.timeInMillis = localMillis
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
