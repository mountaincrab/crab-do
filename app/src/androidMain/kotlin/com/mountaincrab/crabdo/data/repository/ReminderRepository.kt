package com.mountaincrab.crabdo.data.repository

import android.content.Context
import androidx.work.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.mountaincrab.crabdo.alarm.AlarmScheduler
import com.mountaincrab.crabdo.data.local.dao.OneOffReminderDao
import com.mountaincrab.crabdo.data.local.dao.RecurringReminderDao
import com.mountaincrab.crabdo.data.local.entity.OneOffReminderEntity
import com.mountaincrab.crabdo.data.local.entity.RecurringReminderEntity
import com.mountaincrab.crabdo.data.local.entity.ReminderStyle
import com.mountaincrab.crabdo.data.model.RecurrenceRule
import com.mountaincrab.crabdo.data.model.SyncStatus
import com.mountaincrab.crabdo.data.remote.toOneOffReminderEntity
import com.mountaincrab.crabdo.data.remote.toRecurringReminderEntity
import com.mountaincrab.crabdo.data.remote.SyncWorker
import com.mountaincrab.crabdo.domain.RecurrenceEngine
import com.mountaincrab.crabdo.widget.OneOffRemindersWidget
import com.mountaincrab.crabdo.widget.RecurringRemindersWidget
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

class ReminderRepository(
    private val oneOffDao: OneOffReminderDao,
    private val recurringDao: RecurringReminderDao,
    private val alarmScheduler: AlarmScheduler,
    private val workManager: WorkManager,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val context: Context
) {
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var oneOffListener: ListenerRegistration? = null
    private var recurringListener: ListenerRegistration? = null

    // ── Observe ───────────────────────────────────────────────────────────────

    fun observeOneOffs(userId: String) = oneOffDao.observeActive(userId)
    fun observeCompletedOneOffs(userId: String) = oneOffDao.observeCompleted(userId)
    fun observeRecurring(userId: String) = recurringDao.observeActive(userId)

    suspend fun getOneOffById(id: String): OneOffReminderEntity? = oneOffDao.getById(id)
    suspend fun getRecurringById(id: String): RecurringReminderEntity? = recurringDao.getById(id)

    // ── One-off CRUD ──────────────────────────────────────────────────────────

    suspend fun createOneOff(
        userId: String,
        title: String,
        scheduledAt: Long,
        style: ReminderStyle
    ): OneOffReminderEntity {
        val entity = OneOffReminderEntity(
            userId = userId,
            title = title,
            scheduledAt = scheduledAt,
            reminderStyle = style
        )
        oneOffDao.upsert(entity)
        alarmScheduler.scheduleReminder(entity.id, entity.title, entity.scheduledAt, entity.reminderStyle.name)
        enqueueSyncWork()
        notifyWidgets()
        return entity
    }

    suspend fun updateOneOff(entity: OneOffReminderEntity) {
        oneOffDao.upsert(entity.copy(updatedAt = System.currentTimeMillis(), syncStatus = SyncStatus.PENDING))
        alarmScheduler.cancelReminder(entity.id)
        if (entity.isEnabled) alarmScheduler.scheduleReminder(entity.id, entity.title, entity.scheduledAt, entity.reminderStyle.name)
        enqueueSyncWork()
        notifyWidgets()
    }

    suspend fun deleteOneOff(id: String) {
        alarmScheduler.cancelReminder(id)
        oneOffDao.softDelete(id)
        enqueueSyncWork()
        notifyWidgets()
    }

    // ── Recurring CRUD ────────────────────────────────────────────────────────

    suspend fun createRecurring(
        userId: String,
        title: String,
        rule: RecurrenceRule,
        startDate: Long,
        reminderTime: String,
        style: ReminderStyle
    ): RecurringReminderEntity {
        val (hour, minute) = parseReminderTime(reminderTime)
        val nextFire = RecurrenceEngine.nextTriggerAfter(rule, System.currentTimeMillis(), hour, minute)
            ?: (System.currentTimeMillis() + 86_400_000L)
        val entity = RecurringReminderEntity(
            userId = userId,
            title = title,
            recurrenceRuleJson = rule.toJson(),
            startDate = startDate,
            reminderTime = reminderTime,
            nextFireAt = nextFire,
            reminderStyle = style
        )
        recurringDao.upsert(entity)
        alarmScheduler.scheduleReminder(entity.id, entity.title, entity.nextFireAt, entity.reminderStyle.name)
        enqueueSyncWork()
        notifyWidgets()
        return entity
    }

    suspend fun updateRecurring(entity: RecurringReminderEntity) {
        val (hour, minute) = parseReminderTime(entity.reminderTime)
        val nextFire = RecurrenceEngine.nextTriggerAfter(
            RecurrenceRule.fromJson(entity.recurrenceRuleJson),
            System.currentTimeMillis(), hour, minute
        ) ?: entity.nextFireAt
        val updated = entity.copy(
            nextFireAt = nextFire,
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        )
        recurringDao.upsert(updated)
        alarmScheduler.cancelReminder(entity.id)
        if (entity.isEnabled) alarmScheduler.scheduleReminder(updated.id, updated.title, updated.nextFireAt, updated.reminderStyle.name)
        enqueueSyncWork()
        notifyWidgets()
    }

    suspend fun deleteRecurring(id: String) {
        alarmScheduler.cancelReminder(id)
        recurringDao.softDelete(id)
        enqueueSyncWork()
        notifyWidgets()
    }

    // ── Snooze ────────────────────────────────────────────────────────────────

    suspend fun setSnoozeUntil(reminderId: String, millis: Long) {
        oneOffDao.snoozeAndReactivate(reminderId, millis)
        recurringDao.updateSnooze(reminderId, millis)
        enqueueSyncWork()
        notifyWidgets()
    }

    suspend fun clearSnooze(reminderId: String) {
        oneOffDao.updateSnooze(reminderId, null)
        recurringDao.updateSnooze(reminderId, null)
    }

    // ── Alarm firing ──────────────────────────────────────────────────────────

    suspend fun onReminderFired(reminderId: String) {
        val oneOff = oneOffDao.getById(reminderId)
        if (oneOff != null && !oneOff.isDeleted) {
            oneOffDao.markCompleted(reminderId)
            enqueueSyncWork()
            notifyWidgets()
            return
        }

        val recurring = recurringDao.getById(reminderId)
        if (recurring != null && !recurring.isDeleted) {
            val (hour, minute) = parseReminderTime(recurring.reminderTime)
            val rule = RecurrenceRule.fromJson(recurring.recurrenceRuleJson)
            val nextFire = RecurrenceEngine.nextTriggerAfter(rule, System.currentTimeMillis(), hour, minute)
            if (nextFire != null) {
                recurringDao.advanceToNext(reminderId, nextFire)
                if (recurring.isEnabled) {
                    alarmScheduler.scheduleReminder(reminderId, recurring.title, nextFire, recurring.reminderStyle.name)
                }
            }
            enqueueSyncWork()
            notifyWidgets()
        }
    }

    suspend fun rescheduleAllReminders() {
        val userId = firebaseAuth.currentUser?.uid ?: return
        val now = System.currentTimeMillis()

        oneOffDao.getAllActive(userId).forEach { reminder ->
            if (reminder.scheduledAt > now) {
                alarmScheduler.scheduleReminder(reminder.id, reminder.title, reminder.scheduledAt, reminder.reminderStyle.name)
            }
        }

        recurringDao.getAllActive(userId).forEach { reminder ->
            val (hour, minute) = parseReminderTime(reminder.reminderTime)
            val fireAt = if (reminder.nextFireAt > now) {
                reminder.nextFireAt
            } else {
                val rule = RecurrenceRule.fromJson(reminder.recurrenceRuleJson)
                val next = RecurrenceEngine.nextTriggerAfter(rule, now, hour, minute) ?: return@forEach
                recurringDao.advanceToNext(reminder.id, next)
                next
            }
            alarmScheduler.scheduleReminder(reminder.id, reminder.title, fireAt, reminder.reminderStyle.name)
        }
    }

    // ── Firestore listener ────────────────────────────────────────────────────

    fun startFirestoreListener(userId: String) {
        stopFirestoreListener()
        val userRef = firestore.collection("users").document(userId)

        oneOffListener = userRef.collection("reminders")
            .whereEqualTo("isDeleted", false)
            .addSnapshotListener { snap, error ->
                if (error != null || snap == null) return@addSnapshotListener
                repositoryScope.launch {
                    for (change in snap.documentChanges) {
                        when (change.type) {
                            DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                val entity = change.document.toOneOffReminderEntity(userId)
                                    .copy(syncStatus = SyncStatus.SYNCED)
                                val existing = oneOffDao.getById(entity.id)
                                if (existing != null && existing.syncStatus == SyncStatus.PENDING) continue
                                oneOffDao.upsert(entity)
                                if (entity.isEnabled && !entity.isCompleted && entity.scheduledAt > System.currentTimeMillis()) {
                                    alarmScheduler.scheduleReminder(entity.id, entity.title, entity.scheduledAt, entity.reminderStyle.name)
                                } else {
                                    alarmScheduler.cancelReminder(entity.id)
                                }
                            }
                            DocumentChange.Type.REMOVED -> {
                                alarmScheduler.cancelReminder(change.document.id)
                                oneOffDao.softDelete(change.document.id)
                            }
                        }
                    }
                    notifyWidgets()
                }
            }

        recurringListener = userRef.collection("recurringReminders")
            .whereEqualTo("isDeleted", false)
            .addSnapshotListener { snap, error ->
                if (error != null || snap == null) return@addSnapshotListener
                repositoryScope.launch {
                    for (change in snap.documentChanges) {
                        when (change.type) {
                            DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                val entity = change.document.toRecurringReminderEntity(userId)
                                    .copy(syncStatus = SyncStatus.SYNCED)
                                val existing = recurringDao.getById(entity.id)
                                if (existing != null && existing.syncStatus == SyncStatus.PENDING) continue
                                recurringDao.upsert(entity)
                                if (entity.isEnabled && entity.nextFireAt > System.currentTimeMillis()) {
                                    alarmScheduler.scheduleReminder(entity.id, entity.title, entity.nextFireAt, entity.reminderStyle.name)
                                } else {
                                    alarmScheduler.cancelReminder(entity.id)
                                }
                            }
                            DocumentChange.Type.REMOVED -> {
                                alarmScheduler.cancelReminder(change.document.id)
                                recurringDao.softDelete(change.document.id)
                            }
                        }
                    }
                    notifyWidgets()
                }
            }
    }

    fun stopFirestoreListener() {
        oneOffListener?.remove(); oneOffListener = null
        recurringListener?.remove(); recurringListener = null
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    suspend fun notifyWidgets() {
        try { OneOffRemindersWidget().updateAll(context) } catch (_: Exception) {}
        try { RecurringRemindersWidget().updateAll(context) } catch (_: Exception) {}
    }

    fun triggerSync() = enqueueSyncWork()

    private fun enqueueSyncWork() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork("sync", ExistingWorkPolicy.REPLACE, request)
    }

    private fun parseReminderTime(reminderTime: String): Pair<Int, Int> {
        val parts = reminderTime.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 9
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return hour to minute
    }
}

fun midnightOf(millis: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
