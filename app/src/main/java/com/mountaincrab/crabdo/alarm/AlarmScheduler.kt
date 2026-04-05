package com.mountaincrab.crabdo.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import com.mountaincrab.crabdo.data.local.entity.ReminderEntity
import com.mountaincrab.crabdo.data.local.entity.TaskEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun scheduleReminder(reminder: ReminderEntity) {
        schedule(
            requestCode = reminderRequestCode(reminder.id),
            triggerMillis = reminder.nextTriggerMillis,
            intent = buildReminderIntent(reminder.id, reminder.title, reminder.reminderStyle)
        )
    }

    fun scheduleSnooze(reminderId: String, title: String, style: ReminderEntity.ReminderStyle, triggerMillis: Long) {
        schedule(
            requestCode = reminderRequestCode(reminderId),
            triggerMillis = triggerMillis,
            intent = buildReminderIntent(reminderId, title, style)
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
        when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
            alarmManager.canScheduleExactAlarms() -> {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
            else -> {
                alarmManager.setWindow(AlarmManager.RTC_WAKEUP, triggerMillis, 600_000L, pendingIntent)
            }
        }
    }

    private fun cancel(requestCode: Int) {
        val alarmManager = context.getSystemService<AlarmManager>() ?: return
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode,
            Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        alarmManager.cancel(pendingIntent)
    }

    private fun buildReminderIntent(id: String, title: String, style: ReminderEntity.ReminderStyle) =
        Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_FIRE_REMINDER
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, id)
            putExtra(ReminderReceiver.EXTRA_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_TYPE, "reminder")
            putExtra(ReminderReceiver.EXTRA_STYLE, style.name)
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
}
