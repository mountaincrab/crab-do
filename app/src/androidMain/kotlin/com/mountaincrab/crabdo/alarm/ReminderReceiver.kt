package com.mountaincrab.crabdo.alarm

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.getSystemService
import com.mountaincrab.crabdo.data.local.entity.ReminderStyle
import com.mountaincrab.crabdo.data.repository.ReminderRepository
import com.mountaincrab.crabdo.data.repository.TaskRepository
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
        val type = intent.getStringExtra(EXTRA_TYPE) ?: TYPE_REMINDER
        val notificationId = reminderId.hashCode() and 0x7FFFFFFF

        Log.d(TAG, "handleFire: reminderId=$reminderId, style=$style, type=$type, title=$title")

        val isAlarm = style == ReminderStyle.ALARM
        val pendingResult = goAsync()
        val repo: ReminderRepository = get()
        val taskRepo: TaskRepository = get()
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope.launch {
            try {
                // The lookup also recognises snoozes created by older app versions,
                // which omitted EXTRA_TYPE and used the standalone alarm slot.
                val isTask = type == TYPE_TASK || taskRepo.getTask(reminderId) != null
                // Validate and consume the task's matching scheduled time before ringing.
                // A deleted task or an old alarm replaced by a newer time must stay silent.
                if (isTask) {
                    val trigger = if (intent.hasExtra(EXTRA_TRIGGER_MILLIS))
                        intent.getLongExtra(EXTRA_TRIGGER_MILLIS, 0L) else null
                    if (taskRepo.onTaskReminderFired(reminderId, trigger) == null) return@launch
                }
                if (isAlarm) {
                    val serviceIntent = Intent(context, AlarmRingerService::class.java).apply {
                        action = AlarmRingerService.ACTION_START
                        putExtra(EXTRA_REMINDER_ID, reminderId)
                        putExtra(EXTRA_TITLE, title)
                        putExtra(EXTRA_TYPE, if (isTask) TYPE_TASK else TYPE_REMINDER)
                        putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                }
                // For notification-style reminders, resolve where a tap should navigate
                // before firing (which may mark a one-off completed / advance a recurring),
                // then post the notification with a matching content intent.
                if (!isAlarm) {
                    val tapTarget = when {
                        isTask -> NotificationHelper.TapTarget.TASK
                        repo.getOneOffById(reminderId) != null -> NotificationHelper.TapTarget.ONE_OFF
                        repo.getRecurringById(reminderId) != null -> NotificationHelper.TapTarget.RECURRING
                        else -> NotificationHelper.TapTarget.ONE_OFF
                    }
                    NotificationHelper.showAlarmNotification(
                        context, reminderId, title, notificationId, style, tapTarget
                    )
                }
                // A task reminder lives on the task, not in the reminders tables —
                // onReminderFired would find nothing and leave it armed forever.
                if (!isTask) {
                    repo.clearSnooze(reminderId)
                    repo.onReminderFired(reminderId)
                }
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
        val pendingResult = goAsync()
        val taskRepo: TaskRepository = get()
        val repo: ReminderRepository = get()
        val scheduler: AlarmScheduler = get()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val task = taskRepo.getTask(reminderId)
                if (intent.getStringExtra(EXTRA_TYPE) == TYPE_TASK || task != null) {
                    taskRepo.snoozeTaskReminder(reminderId, snoozeTime)
                } else {
                    repo.setSnoozeUntil(reminderId, snoozeTime)
                    scheduler.scheduleReminder(
                        reminderId, intent.getStringExtra(EXTRA_TITLE) ?: "Reminder", snoozeTime,
                        intent.getStringExtra(EXTRA_STYLE) ?: "ALARM"
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
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
        const val EXTRA_TRIGGER_MILLIS = "trigger_millis"
        const val EXTRA_TYPE = "type"
        const val EXTRA_STYLE = "style"
        // EXTRA_TYPE values: distinguishes a task reminder from a standalone reminder.
        const val TYPE_TASK = "task"
        const val TYPE_REMINDER = "reminder"
    }
}
