package com.mountaincrab.crabdo.alarm

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.app.NotificationManager
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.mountaincrab.crabdo.R
import com.mountaincrab.crabdo.notification.NotificationHelper
import com.mountaincrab.crabdo.preferences.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.LinkedList

/**
 * Foreground service that keeps the alarm ringing (looping MediaPlayer) until the user
 * explicitly dismisses or snoozes. Started by [ReminderReceiver] when an ALARM-style
 * reminder fires; stopped by dismiss/snooze actions.
 *
 * When multiple alarms fire simultaneously, they are queued. After each dismiss/snooze
 * the next alarm in the queue is presented automatically.
 */
class AlarmRingerService : Service(), KoinComponent {

    private data class AlarmItem(val reminderId: String, val title: String, val notificationId: Int)

    private val prefs: UserPreferencesRepository by inject()

    private val queue = LinkedList<AlarmItem>()
    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var autoDismissJob: Job? = null

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
                    val item = AlarmItem(reminderId, title, notificationId)
                    queue.add(item)
                    startForeground(FOREGROUND_NOTIFICATION_ID, buildNotification(reminderId, title, notificationId))
                    startAlarmSound()
                    acquireWakeLock()
                    scheduleAutoDismiss(item)
                } else {
                    Log.d(TAG, "Queuing alarm '$title' (id=$reminderId), queue size will be ${queue.size + 1}")
                    queue.add(AlarmItem(reminderId, title, notificationId))
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun advanceQueue() {
        autoDismissJob?.cancel()
        queue.poll() // remove the alarm that was just handled
        val next = queue.peek()
        if (next != null) {
            Log.d(TAG, "Advancing to next alarm: '${next.title}' (id=${next.reminderId})")
            val notification = buildNotification(next.reminderId, next.title, next.notificationId)
            startForeground(FOREGROUND_NOTIFICATION_ID, notification)
            scheduleAutoDismiss(next)
        } else {
            Log.d(TAG, "Queue empty — shutting down")
            shutdown()
        }
    }

    /**
     * Starts (or restarts) the auto-dismiss countdown for the currently-ringing alarm.
     * After the user-configured timeout the alarm is silenced as if dismissed, and a
     * follow-up notification is posted so the reminder isn't lost. A value of 0 disables it.
     */
    private fun scheduleAutoDismiss(item: AlarmItem) {
        autoDismissJob?.cancel()
        autoDismissJob = serviceScope.launch {
            val minutes = prefs.getAutoDismissMinutes()
            if (minutes <= 0) return@launch
            // Keep the CPU awake for the whole countdown (plus a small buffer) so the
            // sound keeps playing right up to the auto-dismiss point.
            acquireWakeLock((minutes + 1) * 60_000L)
            delay(minutes * 60_000L)
            // Only act if this item is still the one ringing.
            if (queue.peek()?.reminderId != item.reminderId) return@launch
            Log.d(TAG, "Auto-dismissing alarm '${item.title}' (id=${item.reminderId}) after $minutes min")
            getSystemService<NotificationManager>()?.cancel(item.notificationId)
            NotificationHelper.showAutoDismissedNotification(applicationContext, item.reminderId, item.title)
            advanceQueue()
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

    private fun acquireWakeLock(timeoutMs: Long = 10 * 60 * 1000L) {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock?.runCatching { if (isHeld) release() }
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CrabDo::AlarmRinger")
            .apply { acquire(timeoutMs) }
    }

    private fun shutdown() {
        autoDismissJob?.cancel()
        queue.clear()
        mediaPlayer?.runCatching { if (isPlaying) stop(); release() }
        mediaPlayer = null
        wakeLock?.runCatching { if (isHeld) release() }
        wakeLock = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        mediaPlayer?.runCatching { if (isPlaying) stop(); release() }
        mediaPlayer = null
        wakeLock?.runCatching { if (isHeld) release() }
    }

    companion object {
        private const val TAG = "AlarmRingerService"
        const val ACTION_START = "com.mountaincrab.crabdo.ACTION_START_ALARM"
        const val ACTION_STOP = "com.mountaincrab.crabdo.ACTION_STOP_ALARM"
        const val ACTION_ADVANCE = "com.mountaincrab.crabdo.ACTION_ADVANCE_ALARM"
        const val FOREGROUND_NOTIFICATION_ID = 9999
    }
}
