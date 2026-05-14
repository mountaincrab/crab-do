package com.mountaincrab.crabdo.alarm

import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.getSystemService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mountaincrab.crabdo.ui.theme.CrabbanTheme
import com.mountaincrab.crabdo.ui.theme.LocalAppPalette
import com.mountaincrab.crabdo.ui.theme.ThemeViewModel
import org.koin.compose.viewmodel.koinViewModel

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
            val themeViewModel: ThemeViewModel = koinViewModel()
            val appTheme by themeViewModel.appTheme.collectAsStateWithLifecycle()
            CrabbanTheme(appTheme = appTheme) {
                AlarmAlertScreen(
                    title = title,
                    onDismiss = {
                        if (notificationId != -1) {
                            getSystemService<NotificationManager>()?.cancel(notificationId)
                        }
                        startService(Intent(this, AlarmRingerService::class.java).apply {
                            action = AlarmRingerService.ACTION_ADVANCE
                        })
                        finish()
                    },
                    onSnooze = {
                        startActivity(Intent(this, SnoozePickerActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminderId)
                            putExtra(ReminderReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                            putExtra(ReminderReceiver.EXTRA_TITLE, title)
                            putExtra(ReminderReceiver.EXTRA_STYLE, "ALARM")
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
    val palette = LocalAppPalette.current
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Warm alarm glow from the top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            palette.alarmTint.copy(alpha = 0.10f),
                            Color.Transparent,
                        )
                    )
                )
        )

        // Centred icon + title
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Alarm icon tile
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(
                        color = palette.alarmTileBg,
                        shape = RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "⏰", fontSize = 44.sp)
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
                color = colors.onSurface,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "alarm reminder",
                style = MaterialTheme.typography.labelSmall,
                color = palette.alarmTint,
                letterSpacing = 1.2.sp,
            )
        }

        // Action buttons pinned to bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 48.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(
                    text = "Dismiss",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            OutlinedButton(
                onClick = onSnooze,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(
                    text = "Snooze…",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
