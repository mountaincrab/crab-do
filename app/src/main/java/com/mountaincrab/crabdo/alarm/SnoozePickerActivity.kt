package com.mountaincrab.crabdo.alarm

import android.app.NotificationManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.mountaincrab.crabdo.data.local.entity.ReminderEntity
import com.mountaincrab.crabdo.data.repository.ReminderRepository
import com.mountaincrab.crabdo.ui.theme.CrabbanTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SnoozePickerActivity : ComponentActivity() {

    @Inject lateinit var alarmScheduler: AlarmScheduler
    @Inject lateinit var reminderRepository: ReminderRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val reminderId = intent.getStringExtra(ReminderReceiver.EXTRA_REMINDER_ID) ?: run { finish(); return }
        val notificationId = intent.getIntExtra(ReminderReceiver.EXTRA_NOTIFICATION_ID, -1)
        val title = intent.getStringExtra(ReminderReceiver.EXTRA_TITLE) ?: "Reminder"
        val styleStr = intent.getStringExtra(ReminderReceiver.EXTRA_STYLE) ?: "ALARM"
        val style = try { ReminderEntity.ReminderStyle.valueOf(styleStr) }
                    catch (e: Exception) { ReminderEntity.ReminderStyle.ALARM }

        if (notificationId != -1) {
            getSystemService(NotificationManager::class.java)?.cancel(notificationId)
        }
        // Stop the looping ringer — user is now choosing snooze duration or dismissing
        stopService(Intent(this, AlarmRingerService::class.java))

        setContent {
            CrabbanTheme {
                SnoozePickerDialog(
                    onSnooze = { minutes ->
                        val snoozeMillis = System.currentTimeMillis() + minutes * 60_000L
                        alarmScheduler.scheduleSnooze(reminderId, title, style, snoozeMillis)
                        lifecycleScope.launch {
                            reminderRepository.setSnoozeUntil(reminderId, snoozeMillis)
                        }
                        finish()
                    },
                    onDismiss = { finish() }
                )
            }
        }
    }
}

@Composable
private fun SnoozePickerDialog(
    onSnooze: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var showCustom by remember { mutableStateOf(false) }
    var customText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Snooze for…") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 10, 15, 20, 30).forEach { minutes ->
                    FilledTonalButton(
                        onClick = { onSnooze(minutes) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("$minutes minutes")
                    }
                }
                OutlinedButton(
                    onClick = { showCustom = !showCustom },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Custom…")
                }
                if (showCustom) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customText,
                            onValueChange = { customText = it.filter { c -> c.isDigit() } },
                            label = { Text("Minutes") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val m = customText.toIntOrNull() ?: return@Button
                                if (m > 0) onSnooze(m)
                            },
                            enabled = customText.toIntOrNull()?.let { it > 0 } == true
                        ) {
                            Text("OK")
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Dismiss alarm") }
        }
    )
}
