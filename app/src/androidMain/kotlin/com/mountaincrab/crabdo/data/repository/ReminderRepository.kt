package com.mountaincrab.crabdo.data.repository

import android.content.Context
import androidx.work.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.mountaincrab.crabdo.alarm.AlarmScheduler
import com.mountaincrab.crabdo.data.local.dao.ReminderDao
import com.mountaincrab.crabdo.data.local.entity.ReminderEntity
import com.mountaincrab.crabdo.data.model.RecurrenceRule
import com.mountaincrab.crabdo.data.model.SyncStatus
import com.mountaincrab.crabdo.data.remote.toReminderEntity
import com.mountaincrab.crabdo.data.remote.SyncWorker
import com.mountaincrab.crabdo.domain.RecurrenceEngine
import com.mountaincrab.crabdo.widget.RemindersWidget
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class ReminderRepository(
    private val reminderDao: ReminderDao,
    private val alarmScheduler: AlarmScheduler,
    private val workManager: WorkManager,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val context: Context
) {
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var firestoreListener: ListenerRegistration? = null

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

    /**
     * Start a real-time Firestore listener for this user's reminders.
     * Any reminder added or modified from another device (e.g. the web app) will be
     * upserted into Room and have its alarm scheduled immediately.
     * Call [stopFirestoreListener] when the user signs out.
     */
    fun startFirestoreListener(userId: String) {
        stopFirestoreListener()
        firestoreListener = firestore
            .collection("users").document(userId)
            .collection("reminders")
            .whereEqualTo("isDeleted", false)
            .addSnapshotListener { snap, error ->
                if (error != null || snap == null) return@addSnapshotListener
                repositoryScope.launch {
                    for (change in snap.documentChanges) {
                        when (change.type) {
                            DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                val reminder = change.document.toReminderEntity(userId)
                                    .copy(syncStatus = SyncStatus.SYNCED)
                                reminderDao.upsert(reminder)
                                if (reminder.isEnabled && !reminder.isCompleted &&
                                    reminder.nextTriggerMillis > System.currentTimeMillis()
                                ) {
                                    alarmScheduler.scheduleReminder(reminder)
                                } else {
                                    alarmScheduler.cancelReminder(reminder.id)
                                }
                            }
                            DocumentChange.Type.REMOVED -> {
                                // Soft-delete means we won't see true removals, but cancel the alarm
                                alarmScheduler.cancelReminder(change.document.id)
                            }
                        }
                    }
                    notifyWidgets()
                }
            }
    }

    fun stopFirestoreListener() {
        firestoreListener?.remove()
        firestoreListener = null
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
