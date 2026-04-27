package com.mountaincrab.crabdo.alarm

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mountaincrab.crabdo.R
import com.mountaincrab.crabdo.notification.NotificationHelper
import java.util.LinkedList

/**
 * Foreground service that keeps the alarm ringing (looping MediaPlayer) until the user
 * explicitly dismisses or snoozes. Started by [ReminderReceiver] when an ALARM-style
 * reminder fires; stopped by dismiss/snooze actions.
 *
 * When multiple alarms fire simultaneously, they are queued. After each dismiss/snooze
 * the next alarm in the queue is presented automatically.
 */
class AlarmRingerService : Service() {

    private data class AlarmItem(val reminderId: String, val title: String, val notificationId: Int)

    private val queue = LinkedList<AlarmItem>()
    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                Log.d(TAG, "ACTION_STOP received — shutting down")
                shutdown()
                return START_NOT_STICKY
            }
            ACTION_ADVANCE -> {
                Log.d(TAG, "ACTION_ADVANCE received — advancing queue")
                advanceQueue()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                val reminderId = intent.getStringExtra(ReminderReceiver.EXTRA_REMINDER_ID) ?: run {
                    Log.w(TAG, "No reminderId — ignoring")
                    if (queue.isEmpty()) stopSelf()
                    return START_NOT_STICKY
                }
                val title = intent.getStringExtra(ReminderReceiver.EXTRA_TITLE) ?: "Reminder"
                val notificationId = reminderId.hashCode() and 0x7FFFFFFF

                if (queue.isEmpty()) {
                    Log.d(TAG, "Starting alarm ringer for '$title' (id=$reminderId)")
                    queue.add(AlarmItem(reminderId, title, notificationId))
                    startForeground(FOREGROUND_NOTIFICATION_ID, buildNotification(reminderId, title, notificationId))
                    startAlarmSound()
                    acquireWakeLock()
                } else {
                    Log.d(TAG, "Queuing alarm '$title' (id=$reminderId), queue size will be ${queue.size + 1}")
                    queue.add(AlarmItem(reminderId, title, notificationId))
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun advanceQueue() {
        queue.poll() // remove the alarm that was just handled
        val next = queue.peek()
        if (next != null) {
            Log.d(TAG, "Advancing to next alarm: '${next.title}' (id=${next.reminderId})")
            val notification = buildNotification(next.reminderId, next.title, next.notificationId)
            startForeground(FOREGROUND_NOTIFICATION_ID, notification)
        } else {
            Log.d(TAG, "Queue empty — shutting down")
            shutdown()
        }
    }

    private fun buildNotification(reminderId: String, title: String, notificationId: Int): Notification {
        val dismissIntent = PendingIntent.getBroadcast(
            this, notificationId + 1000,
            Intent(this, ReminderReceiver::class.java).apply {
                action = ReminderReceiver.ACTION_DISMISS
                putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminderId)
                putExtra(ReminderReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = PendingIntent.getActivity(
            this, notificationId + 2000,
            Intent(this, SnoozePickerActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminderId)
                putExtra(ReminderReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(ReminderReceiver.EXTRA_TITLE, title)
                putExtra(ReminderReceiver.EXTRA_STYLE, "ALARM")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText("Tap to dismiss")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(R.drawable.ic_dismiss, "Dismiss", dismissIntent)
            .addAction(R.drawable.ic_snooze, "Snooze", snoozeIntent)

        val fullScreenIntent = PendingIntent.getActivity(
            this, notificationId + 3000,
            Intent(this, AlarmAlertActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
                putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminderId)
                putExtra(ReminderReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(ReminderReceiver.EXTRA_TITLE, title)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.setFullScreenIntent(fullScreenIntent, true)

        return builder.build()
    }

    private fun startAlarmSound() {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(applicationContext, alarmUri)
                isLooping = true
                prepare()
                start()
            }
            Log.d(TAG, "Alarm sound started (looping)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start alarm sound", e)
        }
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CrabDo::AlarmRinger")
            .apply { acquire(10 * 60 * 1000L) }
    }

    private fun shutdown() {
        queue.clear()
        mediaPlayer?.runCatching { if (isPlaying) stop(); release() }
        mediaPlayer = null
        wakeLock?.release()
        wakeLock = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.runCatching { if (isPlaying) stop(); release() }
        mediaPlayer = null
        wakeLock?.release()
    }

    companion object {
        private const val TAG = "AlarmRingerService"
        const val ACTION_START = "com.mountaincrab.crabdo.ACTION_START_ALARM"
        const val ACTION_STOP = "com.mountaincrab.crabdo.ACTION_STOP_ALARM"
        const val ACTION_ADVANCE = "com.mountaincrab.crabdo.ACTION_ADVANCE_ALARM"
        const val FOREGROUND_NOTIFICATION_ID = 9999
    }
}
