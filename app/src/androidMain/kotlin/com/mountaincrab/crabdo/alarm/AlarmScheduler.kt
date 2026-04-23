package com.mountaincrab.crabdo.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.getSystemService
import com.mountaincrab.crabdo.data.local.entity.TaskEntity

class AlarmScheduler(private val context: Context) {

    fun scheduleReminder(id: String, title: String, triggerMillis: Long, styleName: String) {
        schedule(
            requestCode = reminderRequestCode(id),
            triggerMillis = triggerMillis,
            intent = buildReminderIntent(id, title, styleName)
        )
    }

    fun scheduleTaskReminder(taskId: String, triggerMillis: Long, style: TaskEntity.ReminderStyle) {
        schedule(
            requestCode = taskRequestCode(taskId),
            triggerMillis = triggerMillis,
            intent = buildTaskReminderIntent(taskId, style)
        )
    }

    fun cancelReminder(reminderId: String) {
        cancel(reminderRequestCode(reminderId))
    }

    fun cancelTaskReminder(taskId: String) {
        cancel(taskRequestCode(taskId))
    }

    private fun schedule(requestCode: Int, triggerMillis: Long, intent: Intent) {
        val alarmManager = context.getSystemService<AlarmManager>() ?: return
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val inMs = triggerMillis - System.currentTimeMillis()
        when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> {
                Log.d(TAG, "schedule: exact alarm in ${inMs}ms (pre-S), requestCode=$requestCode")
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
            alarmManager.canScheduleExactAlarms() -> {
                Log.d(TAG, "schedule: exact alarm in ${inMs}ms, requestCode=$requestCode")
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
            else -> {
                Log.d(TAG, "schedule: window alarm in ${inMs}ms (no exact permission), requestCode=$requestCode")
                alarmManager.setWindow(AlarmManager.RTC_WAKEUP, triggerMillis, 600_000L, pendingIntent)
            }
        }
    }

    private fun cancel(requestCode: Int) {
        val alarmManager = context.getSystemService<AlarmManager>() ?: return
        val matchIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_FIRE_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, matchIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        } else {
            val fallback = PendingIntent.getBroadcast(
                context, requestCode, matchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(fallback)
            fallback.cancel()
        }
    }

    private fun buildReminderIntent(id: String, title: String, styleName: String) =
        Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_FIRE_REMINDER
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, id)
            putExtra(ReminderReceiver.EXTRA_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_TYPE, "reminder")
            putExtra(ReminderReceiver.EXTRA_STYLE, styleName)
        }

    private fun buildTaskReminderIntent(taskId: String, style: TaskEntity.ReminderStyle) =
        Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_FIRE_REMINDER
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, taskId)
            putExtra(ReminderReceiver.EXTRA_TYPE, "task")
            putExtra(ReminderReceiver.EXTRA_STYLE, style.name)
        }

    private fun reminderRequestCode(id: String): Int = id.hashCode() and 0x7FFFFFFF
    private fun taskRequestCode(id: String): Int = (id + "_task").hashCode() and 0x7FFFFFFF

    companion object {
        private const val TAG = "AlarmScheduler"
    }
}
