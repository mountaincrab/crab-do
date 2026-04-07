package com.mountaincrab.crabdo.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.mountaincrab.crabdo.R
import com.mountaincrab.crabdo.alarm.AlarmAlertActivity
import com.mountaincrab.crabdo.alarm.ReminderReceiver
import com.mountaincrab.crabdo.alarm.SnoozePickerActivity
import com.mountaincrab.crabdo.data.local.entity.ReminderEntity

object NotificationHelper {
    const val CHANNEL_ALARM = "channel_alarm"
    const val CHANNEL_NOTIFICATION = "channel_notification"

    fun createChannels(context: Context) {
        val manager = context.getSystemService<NotificationManager>() ?: return

        // Channel sound can't be changed after creation, so drop the old alarm channel
        // (which had a ringtone attached) before re-registering it as silent.
        manager.deleteNotificationChannel(CHANNEL_ALARM)

        val alarmChannel = NotificationChannel(
            CHANNEL_ALARM, "Alarms", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Persistent reminders that require dismissal"
            // Silent channel — AlarmRingerService plays the sound itself via a looping MediaPlayer,
            // so we don't want the channel to play a second overlapping ringtone.
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
        style: ReminderEntity.ReminderStyle
    ) {
        val manager = context.getSystemService<NotificationManager>() ?: return

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

        val channel = if (style == ReminderEntity.ReminderStyle.ALARM) CHANNEL_ALARM else CHANNEL_NOTIFICATION

        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText("Tap to dismiss")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .addAction(R.drawable.ic_dismiss, "Dismiss", dismissIntent)
            .addAction(R.drawable.ic_snooze, "Snooze", snoozeIntent)

        if (style == ReminderEntity.ReminderStyle.ALARM) {
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
}
