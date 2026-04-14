package com.mountaincrab.crabdo.data.repository

import android.content.Context
import androidx.work.*
import com.google.firebase.auth.FirebaseAuth
import com.mountaincrab.crabdo.alarm.AlarmScheduler
import com.mountaincrab.crabdo.data.local.dao.ReminderDao
import com.mountaincrab.crabdo.data.local.entity.ReminderEntity
import com.mountaincrab.crabdo.data.model.RecurrenceRule
import com.mountaincrab.crabdo.data.model.SyncStatus
import com.mountaincrab.crabdo.data.remote.SyncWorker
import com.mountaincrab.crabdo.domain.RecurrenceEngine
import com.mountaincrab.crabdo.widget.RemindersWidget
import androidx.glance.appwidget.updateAll
import java.util.concurrent.TimeUnit

class ReminderRepository(
    private val reminderDao: ReminderDao,
    private val alarmScheduler: AlarmScheduler,
    private val workManager: WorkManager,
    private val firebaseAuth: FirebaseAuth,
    private val context: Context
) {
    fun observeReminders(userId: String) = reminderDao.observeReminders(userId)
    fun observeCompletedReminders(userId: String) = reminderDao.observeCompletedReminders(userId)

    suspend fun getReminderById(id: String): ReminderEntity? = reminderDao.getReminderById(id)

    suspend fun setSnoozeUntil(reminderId: String, millis: Long) {
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
        if (reminder.isDeleted) return
        val ruleJson = reminder.recurrenceRuleJson
        if (ruleJson != null) {
            val rule = RecurrenceRule.fromJson(ruleJson)
            val nextTrigger = RecurrenceEngine.nextTriggerAfter(rule, System.currentTimeMillis())
            if (nextTrigger != null) {
                reminderDao.updateNextTrigger(reminderId, nextTrigger)
                alarmScheduler.scheduleReminder(reminder.copy(nextTriggerMillis = nextTrigger))
            } else {
                reminderDao.markCompleted(reminderId)
            }
        } else {
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
