package com.mountaincrab.crabdo.alarm

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.getSystemService
import com.mountaincrab.crabdo.data.local.entity.ReminderStyle
import com.mountaincrab.crabdo.data.repository.ReminderRepository
import com.mountaincrab.crabdo.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class ReminderReceiver : BroadcastReceiver(), KoinComponent {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_FIRE_REMINDER -> handleFire(context, intent)
            ACTION_DISMISS -> handleDismiss(context, intent)
            ACTION_SNOOZE -> handleSnooze(context, intent)
        }
    }

    private fun handleFire(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Reminder"
        val styleStr = intent.getStringExtra(EXTRA_STYLE) ?: "ALARM"
        val style = try { ReminderStyle.valueOf(styleStr) } catch (e: Exception) { ReminderStyle.ALARM }
        val notificationId = reminderId.hashCode() and 0x7FFFFFFF

        Log.d(TAG, "handleFire: reminderId=$reminderId, style=$style, title=$title")

        if (style == ReminderStyle.ALARM) {
            val serviceIntent = Intent(context, AlarmRingerService::class.java).apply {
                action = AlarmRingerService.ACTION_START
                putExtra(EXTRA_REMINDER_ID, reminderId)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } else {
            NotificationHelper.showAlarmNotification(context, reminderId, title, notificationId, style)
        }

        val pendingResult = goAsync()
        val repo: ReminderRepository = get()
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope.launch {
            try {
                repo.clearSnooze(reminderId)
                repo.onReminderFired(reminderId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleDismiss(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        if (notificationId != -1) {
            context.getSystemService<NotificationManager>()?.cancel(notificationId)
        }
        context.startService(Intent(context, AlarmRingerService::class.java).apply {
            action = AlarmRingerService.ACTION_ADVANCE
        })
    }

    private fun handleSnooze(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        if (notificationId != -1) {
            context.getSystemService<NotificationManager>()?.cancel(notificationId)
        }
        val snoozeTime = System.currentTimeMillis() + 10 * 60 * 1000L
        val alarmManager = context.getSystemService<AlarmManager>() ?: return
        val pendingIntent = PendingIntent.getBroadcast(
            context, reminderId.hashCode() and 0x7FFFFFFF,
            Intent(context, ReminderReceiver::class.java).apply {
                action = ACTION_FIRE_REMINDER
                putExtra(EXTRA_REMINDER_ID, reminderId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent)
        context.startService(Intent(context, AlarmRingerService::class.java).apply {
            action = AlarmRingerService.ACTION_ADVANCE
        })
    }

    companion object {
        private const val TAG = "ReminderReceiver"
        const val ACTION_FIRE_REMINDER = "com.mountaincrab.crabdo.ACTION_FIRE_REMINDER"
        const val ACTION_DISMISS = "com.mountaincrab.crabdo.ACTION_DISMISS"
        const val ACTION_SNOOZE = "com.mountaincrab.crabdo.ACTION_SNOOZE"
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_TYPE = "type"
        const val EXTRA_STYLE = "style"
    }
}
