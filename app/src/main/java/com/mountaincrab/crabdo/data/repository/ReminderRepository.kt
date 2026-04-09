package com.mountaincrab.crabdo.data.repository

import android.content.Context
import androidx.work.*
import com.mountaincrab.crabdo.widget.RemindersWidget
import androidx.glance.appwidget.updateAll
import com.google.firebase.auth.FirebaseAuth
import com.mountaincrab.crabdo.alarm.AlarmScheduler
import com.mountaincrab.crabdo.data.local.dao.ReminderDao
import com.mountaincrab.crabdo.data.local.entity.ReminderEntity
import com.mountaincrab.crabdo.data.model.RecurrenceRule
import com.mountaincrab.crabdo.data.model.SyncStatus
import com.mountaincrab.crabdo.data.remote.SyncWorker
import com.mountaincrab.crabdo.domain.RecurrenceEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepository @Inject constructor(
    private val reminderDao: ReminderDao,
    private val alarmScheduler: AlarmScheduler,
    private val workManager: WorkManager,
    private val firebaseAuth: FirebaseAuth,
    @ApplicationContext private val context: Context
) {
    fun observeReminders(userId: String) = reminderDao.observeReminders(userId)
    fun observeCompletedReminders(userId: String) = reminderDao.observeCompletedReminders(userId)

    suspend fun getReminderById(id: String): ReminderEntity? = reminderDao.getReminderById(id)

    suspend fun setSnoozeUntil(reminderId: String, millis: Long) {
        // snoozeAndReactivate also clears isCompleted, so a reminder that was auto-marked
        // completed when it fired gets put back into the active list when the user snoozes.
        reminderDao.snoozeAndReactivate(reminderId, millis)
        enqueueSyncWork()
        notifyWidgets()
    }

    suspend fun clearSnooze(reminderId: String) {
        reminderDao.updateSnoozeUntil(reminderId, null)
    }

    suspend fun createReminder(
        userId: String,
        title: String,
        triggerMillis: Long,
        style: ReminderEntity.ReminderStyle,
        recurrenceRule: RecurrenceRule? = null
    ): ReminderEntity {
        val reminder = ReminderEntity(
            userId = userId,
            title = title,
            nextTriggerMillis = triggerMillis,
            reminderStyle = style,
            recurrenceRuleJson = recurrenceRule?.toJson()
        )
        reminderDao.upsert(reminder)
        alarmScheduler.scheduleReminder(reminder)
        enqueueSyncWork()
        notifyWidgets()
        return reminder
    }

    suspend fun updateReminder(reminder: ReminderEntity) {
        reminderDao.upsert(reminder.copy(
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        ))
        alarmScheduler.cancelReminder(reminder.id)
        if (reminder.isEnabled) alarmScheduler.scheduleReminder(reminder)
        enqueueSyncWork()
        notifyWidgets()
    }

    suspend fun deleteReminder(reminderId: String) {
        alarmScheduler.cancelReminder(reminderId)
        reminderDao.softDelete(reminderId)
        enqueueSyncWork()
        notifyWidgets()
    }

    suspend fun onReminderFired(reminderId: String) {
        val reminder = reminderDao.getReminderById(reminderId) ?: return
        // Guard against a race where a delete lands just before the alarm fires.
        if (reminder.isDeleted) return
        val ruleJson = reminder.recurrenceRuleJson
        if (ruleJson != null) {
            val rule = RecurrenceRule.fromJson(ruleJson)
            val nextTrigger = RecurrenceEngine.nextTriggerAfter(rule, System.currentTimeMillis())
            if (nextTrigger != null) {
                reminderDao.updateNextTrigger(reminderId, nextTrigger)
                alarmScheduler.scheduleReminder(reminder.copy(nextTriggerMillis = nextTrigger))
            } else {
                // Recurrence has ended (e.g. UNTIL date passed) — treat as completed.
                reminderDao.markCompleted(reminderId)
            }
        } else {
            // One-shot reminder has fired — mark it completed so it drops out of the active list.
            reminderDao.markCompleted(reminderId)
        }
        enqueueSyncWork()
        notifyWidgets()
    }

    suspend fun rescheduleAllReminders() {
        val userId = firebaseAuth.currentUser?.uid ?: return
        reminderDao.getAllActiveReminders(userId).forEach { reminder ->
            if (reminder.nextTriggerMillis > System.currentTimeMillis()) {
                alarmScheduler.scheduleReminder(reminder)
            } else {
                val ruleJson = reminder.recurrenceRuleJson
                if (ruleJson != null) {
                    val rule = RecurrenceRule.fromJson(ruleJson)
                    val nextTrigger = RecurrenceEngine.nextTriggerAfter(rule, System.currentTimeMillis())
                    if (nextTrigger != null) {
                        reminderDao.updateNextTrigger(reminder.id, nextTrigger)
                        alarmScheduler.scheduleReminder(reminder.copy(nextTriggerMillis = nextTrigger))
                    }
                }
            }
        }
    }

    suspend fun notifyWidgets() {
        // Call updateAll directly instead of round-tripping through a broadcast +
        // fire-and-forget coroutine in the receiver, which was racy and intermittently
        // dropped updates when the receiver process exited before MainScope ran.
        try {
            RemindersWidget().updateAll(context)
        } catch (_: Exception) {}
    }

    private fun enqueueSyncWork() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork("sync", ExistingWorkPolicy.REPLACE, request)
    }
}
