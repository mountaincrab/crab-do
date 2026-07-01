package com.mountaincrab.crabdo.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.mountaincrab.crabdo.MainActivity
import com.mountaincrab.crabdo.R
import com.mountaincrab.crabdo.alarm.AlarmAlertActivity
import com.mountaincrab.crabdo.alarm.ReminderReceiver
import com.mountaincrab.crabdo.alarm.SnoozePickerActivity
import com.mountaincrab.crabdo.data.local.entity.ReminderStyle
import com.mountaincrab.crabdo.ui.navigation.ReminderTarget

object NotificationHelper {
    const val CHANNEL_ALARM = "channel_alarm_v2"
    const val CHANNEL_NOTIFICATION = "channel_notification"

    /** Where tapping the notification body should take the user. */
    enum class TapTarget { TASK, ONE_OFF, RECURRING }

    fun createChannels(context: Context) {
        val manager = context.getSystemService<NotificationManager>() ?: return

        manager.deleteNotificationChannel("channel_alarm")

        val alarmChannel = NotificationChannel(
            CHANNEL_ALARM, "Alarms", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Persistent reminders that require dismissal"
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }

        val notifChannel = NotificationChannel(
            CHANNEL_NOTIFICATION, "Reminders", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Standard reminder notifications"
            enableVibration(true)
        }

        manager.createNotificationChannel(alarmChannel)
        manager.createNotificationChannel(notifChannel)
    }

    fun showAlarmNotification(
        context: Context,
        id: String,
        title: String,
        notificationId: Int,
        style: ReminderStyle,
        tapTarget: TapTarget = TapTarget.ONE_OFF
    ) {
        val manager = context.getSystemService<NotificationManager>() ?: return

        // Tapping the notification body opens the relevant editor: the task detail
        // screen for a task reminder, or the reminder editor for a standalone reminder.
        val contentIntent = PendingIntent.getActivity(
            context, notificationId + 4000,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                when (tapTarget) {
                    TapTarget.TASK -> putExtra("open_task_id", id)
                    TapTarget.ONE_OFF -> {
                        putExtra("open_reminder_id", id)
                        putExtra("reminder_type", ReminderTarget.ONE_OFF.name)
                    }
                    TapTarget.RECURRING -> {
                        putExtra("open_reminder_id", id)
                        putExtra("reminder_type", ReminderTarget.RECURRING.name)
                    }
                }
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = PendingIntent.getBroadcast(
            context, notificationId + 1000,
            Intent(context, ReminderReceiver::class.java).apply {
                action = ReminderReceiver.ACTION_DISMISS
                putExtra(ReminderReceiver.EXTRA_REMINDER_ID, id)
                putExtra(ReminderReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = PendingIntent.getActivity(
            context, notificationId + 2000,
            Intent(context, SnoozePickerActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(ReminderReceiver.EXTRA_REMINDER_ID, id)
                putExtra(ReminderReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(ReminderReceiver.EXTRA_TITLE, title)
                putExtra(ReminderReceiver.EXTRA_STYLE, style.name)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channel = if (style == ReminderStyle.ALARM) CHANNEL_ALARM else CHANNEL_NOTIFICATION

        val isAlarm = style == ReminderStyle.ALARM

        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(if (isAlarm) "Tap to dismiss" else "Tap to open")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(contentIntent)
            // Standalone notifications clear themselves when tapped; alarms stay until dismissed.
            .setAutoCancel(!isAlarm)
            .addAction(R.drawable.ic_dismiss, "Dismiss", dismissIntent)
            .addAction(R.drawable.ic_snooze, "Snooze", snoozeIntent)

        if (isAlarm) {
            builder.setOngoing(true)

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val fullScreenIntent = PendingIntent.getActivity(
                    context, notificationId + 3000,
                    Intent(context, AlarmAlertActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
                        putExtra(ReminderReceiver.EXTRA_REMINDER_ID, id)
                        putExtra(ReminderReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                        putExtra(ReminderReceiver.EXTRA_TITLE, title)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.setFullScreenIntent(fullScreenIntent, true)
            }
        }

        manager.notify(notificationId, builder.build())
    }

    /**
     * Posted when an unattended ALARM is auto-dismissed after the user-configured timeout,
     * so the reminder isn't silently lost.
     */
    fun showAutoDismissedNotification(
        context: Context,
        id: String,
        title: String
    ) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        // Distinct id so it doesn't collide with (or get cancelled alongside) the alarm notification.
        val notificationId = ((id.hashCode() and 0x7FFFFFFF) xor 0x55555555) and 0x7FFFFFFF

        val builder = NotificationCompat.Builder(context, CHANNEL_NOTIFICATION)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText("Alarm auto-dismissed because it wasn't acknowledged")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)

        manager.notify(notificationId, builder.build())
    }
}
