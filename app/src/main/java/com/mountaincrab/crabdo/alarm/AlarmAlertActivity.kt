package com.mountaincrab.crabdo.alarm

import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AlarmAlertActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        val title = intent.getStringExtra(ReminderReceiver.EXTRA_TITLE) ?: "Reminder"
        val notificationId = intent.getIntExtra(ReminderReceiver.EXTRA_NOTIFICATION_ID, -1)
        val reminderId = intent.getStringExtra(ReminderReceiver.EXTRA_REMINDER_ID) ?: ""

        setContent {
            MaterialTheme {
                AlarmAlertScreen(
                    title = title,
                    onDismiss = {
                        if (notificationId != -1) {
                            getSystemService<NotificationManager>()?.cancel(notificationId)
                        }
                        finish()
                    },
                    onSnooze = {
                        sendBroadcast(Intent(this, ReminderReceiver::class.java).apply {
                            action = ReminderReceiver.ACTION_SNOOZE
                            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminderId)
                            putExtra(ReminderReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                        })
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
private fun AlarmAlertScreen(
    title: String,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Dismiss")
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onSnooze,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Snooze 10 min")
            }
        }
    }
}
