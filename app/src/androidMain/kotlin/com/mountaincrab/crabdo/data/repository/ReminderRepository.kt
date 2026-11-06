package com.mountaincrab.crabdo.data.repository

import android.content.Context
import androidx.work.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.Query
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await
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
    private val repositoryScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var oneOffListener: ListenerRegistration? = null
    private var recurringListener: ListenerRegistration? = null
    private val _historyPageStates = MutableStateFlow<Map<String, PageLoadState>>(emptyMap())
    val historyPageStates = _historyPageStates.asStateFlow()
    private val historyListeners = mutableMapOf<String, ListenerRegistration>()

    // ── Observe ───────────────────────────────────────────────────────────────

    fun observeOneOffs(userId: String) = oneOffDao.observeActive(userId)
    fun observeCompletedOneOffs(userId: String, limit: Int) = oneOffDao.observeCompleted(userId, limit)
    fun observeDeletedOneOffs(userId: String, limit: Int) = oneOffDao.observeDeleted(userId, limit)
    fun observeRecurring(userId: String) = recurringDao.observeActive(userId)
    fun observeDeletedRecurring(userId: String, limit: Int) = recurringDao.observeDeleted(userId, limit)

    fun observeCompletedCount(userId: String) = oneOffDao.observeCompletedCount(userId)
    fun observeDeletedCount(userId: String) = oneOffDao.observeDeletedCount(userId)
    fun observeDeletedRecurringCount(userId: String) = recurringDao.observeDeletedCount(userId)

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
        val now = System.currentTimeMillis()
        val fireAt = entity.snoozedUntilMillis?.takeIf { it > now } ?: entity.scheduledAt
        alarmScheduler.cancelReminder(entity.id)
        if (fireAt > now) alarmScheduler.scheduleReminder(entity.id, entity.title, fireAt, entity.reminderStyle.name)
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
        val nextFire = RecurrenceEngine.nextTriggerFrom(rule, startDate, System.currentTimeMillis(), hour, minute)
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
        val nextFire = RecurrenceEngine.nextTriggerFrom(
            RecurrenceRule.fromJson(entity.recurrenceRuleJson),
            entity.startDate, System.currentTimeMillis(), hour, minute
        ) ?: entity.nextFireAt
        val updated = entity.copy(
            nextFireAt = nextFire,
            snoozedUntilMillis = null,
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

    suspend fun dismissNextRecurring(id: String) {
        val reminder = recurringDao.getById(id) ?: return
        if (reminder.isDeleted) return
        val rule = RecurrenceRule.fromJson(reminder.recurrenceRuleJson)
        val (hour, minute) = parseReminderTime(reminder.reminderTime)
        val now = System.currentTimeMillis()

        // If there's an active snooze, onReminderFired already advanced nextFireAt to the
        // next occurrence when the alarm originally fired. Dismissing the snooze just means
        // cancelling it — nextFireAt is already correct, so don't advance again.
        val newNext: Long = if (reminder.snoozedUntilMillis != null && reminder.snoozedUntilMillis > now) {
            reminder.nextFireAt
        } else {
            // No active snooze — advance past the current occurrence.
            // If nextFireAt is stale (past), catch up first so we don't skip an extra occurrence.
            val currentNext = if (reminder.nextFireAt > now) reminder.nextFireAt
                else RecurrenceEngine.nextTriggerFrom(rule, reminder.startDate, now, hour, minute) ?: return
            RecurrenceEngine.nextTriggerAfter(rule, currentNext, hour, minute) ?: return
        }
        recurringDao.advanceToNext(id, newNext)
        alarmScheduler.cancelReminder(id)
        if (reminder.isEnabled) {
            alarmScheduler.scheduleReminder(id, reminder.title, newNext, reminder.reminderStyle.name)
        }
        enqueueSyncWork()
        notifyWidgets()
    }

    suspend fun restoreOneOff(id: String) {
        oneOffDao.restore(id)
        val entity = oneOffDao.getById(id) ?: return
        val now = System.currentTimeMillis()
        val fireAt = entity.snoozedUntilMillis?.takeIf { it > now } ?: entity.scheduledAt
        if (!entity.isCompleted && fireAt > now) {
            alarmScheduler.scheduleReminder(entity.id, entity.title, fireAt, entity.reminderStyle.name)
        }
        enqueueSyncWork()
        notifyWidgets()
    }

    suspend fun restoreRecurring(id: String) {
        val existing = recurringDao.getById(id) ?: return
        val (hour, minute) = parseReminderTime(existing.reminderTime)
        val rule = RecurrenceRule.fromJson(existing.recurrenceRuleJson)
        val nextFire = RecurrenceEngine.nextTriggerFrom(rule, existing.startDate, System.currentTimeMillis(), hour, minute)
            ?: (System.currentTimeMillis() + 86_400_000L)
        val restored = existing.copy(
            isDeleted = false,
            nextFireAt = nextFire,
            snoozedUntilMillis = null,
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        )
        recurringDao.upsert(restored)
        if (restored.isEnabled) {
            alarmScheduler.scheduleReminder(restored.id, restored.title, restored.nextFireAt, restored.reminderStyle.name)
        }
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
            val nextFire = RecurrenceEngine.nextTriggerFrom(rule, recurring.startDate, System.currentTimeMillis(), hour, minute)
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
            val snooze = reminder.snoozedUntilMillis?.takeIf { it > now }
            if (snooze == null && reminder.snoozedUntilMillis != null) {
                oneOffDao.updateSnooze(reminder.id, null)
                enqueueSyncWork()
            }
            val fireAt = snooze ?: reminder.scheduledAt
            if (fireAt > now) {
                alarmScheduler.scheduleReminder(reminder.id, reminder.title, fireAt, reminder.reminderStyle.name)
            }
        }

        recurringDao.getAllActive(userId).forEach { reminder ->
            val snooze = reminder.snoozedUntilMillis?.takeIf { it > now }
            if (snooze == null && reminder.snoozedUntilMillis != null) {
                recurringDao.updateSnooze(reminder.id, null)
                enqueueSyncWork()
            }
            val (hour, minute) = parseReminderTime(reminder.reminderTime)
            val fireAt = snooze ?: if (reminder.nextFireAt > now) {
                reminder.nextFireAt
            } else {
                val rule = RecurrenceRule.fromJson(reminder.recurrenceRuleJson)
                val next = RecurrenceEngine.nextTriggerFrom(rule, reminder.startDate, now, hour, minute) ?: return@forEach
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
            .whereEqualTo("isCompleted", false)
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
                                val now = System.currentTimeMillis()
                                val fireAt = entity.snoozedUntilMillis?.takeIf { it > now }
                                    ?: entity.scheduledAt
                                if (!entity.isCompleted && fireAt > now) {
                                    alarmScheduler.scheduleReminder(entity.id, entity.title, fireAt, entity.reminderStyle.name)
                                } else {
                                    alarmScheduler.cancelReminder(entity.id)
                                }
                            }
                            DocumentChange.Type.REMOVED -> {
                                // Leaving the active query can mean completion, not deletion.
                                val id = change.document.id
                                if (oneOffDao.getById(id)?.syncStatus == SyncStatus.PENDING) continue
                                val doc = try { change.document.reference.get().await() } catch (_: Exception) { continue }
                                if (oneOffDao.getById(id)?.syncStatus == SyncStatus.PENDING) continue
                                val entity = if (doc.exists()) doc.toOneOffReminderEntity(userId).copy(syncStatus = SyncStatus.SYNCED) else null
                                if (entity != null) oneOffDao.upsert(entity)
                                else oneOffDao.getById(id)?.let { oneOffDao.upsert(it.copy(isDeleted = true, syncStatus = SyncStatus.SYNCED)) }
                                val fireAt = entity?.let { it.snoozedUntilMillis ?: it.scheduledAt }
                                if (entity != null && !entity.isDeleted && !entity.isCompleted && fireAt != null && fireAt > System.currentTimeMillis()) {
                                    alarmScheduler.scheduleReminder(id, entity.title, fireAt, entity.reminderStyle.name)
                                } else alarmScheduler.cancelReminder(id)
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
                                val now = System.currentTimeMillis()
                                val snooze = entity.snoozedUntilMillis?.takeIf { it > now }
                                val fireAt: Long? = when {
                                    snooze != null -> snooze
                                    entity.nextFireAt > now -> entity.nextFireAt
                                    entity.isEnabled -> {
                                        val rule = RecurrenceRule.fromJson(entity.recurrenceRuleJson)
                                        val (hour, minute) = parseReminderTime(entity.reminderTime)
                                        RecurrenceEngine.nextTriggerFrom(rule, entity.startDate, now, hour, minute)?.also { next ->
                                            recurringDao.advanceToNext(entity.id, next)
                                            enqueueSyncWork()
                                        }
                                    }
                                    else -> null
                                }
                                if (entity.isEnabled && fireAt != null) {
                                    alarmScheduler.scheduleReminder(entity.id, entity.title, fireAt, entity.reminderStyle.name)
                                } else {
                                    alarmScheduler.cancelReminder(entity.id)
                                }
                            }
                            DocumentChange.Type.REMOVED -> {
                                val id = change.document.id
                                if (recurringDao.getById(id)?.syncStatus == SyncStatus.PENDING) continue
                                val doc = try { change.document.reference.get().await() } catch (_: Exception) { continue }
                                if (recurringDao.getById(id)?.syncStatus == SyncStatus.PENDING) continue
                                val entity = if (doc.exists()) doc.toRecurringReminderEntity(userId).copy(syncStatus = SyncStatus.SYNCED) else null
                                if (entity != null) recurringDao.upsert(entity)
                                else recurringDao.getById(id)?.let { recurringDao.upsert(it.copy(isDeleted = true, syncStatus = SyncStatus.SYNCED)) }
                                val fireAt = entity?.let { it.snoozedUntilMillis ?: it.nextFireAt }
                                if (entity != null && !entity.isDeleted && entity.isEnabled && fireAt != null && fireAt > System.currentTimeMillis()) {
                                    alarmScheduler.scheduleReminder(id, entity.title, fireAt, entity.reminderStyle.name)
                                } else alarmScheduler.cancelReminder(id)
                            }
                        }
                    }
                    notifyWidgets()
                }
            }
    }

    fun setHistoryWindow(userId: String, kind: String, limit: Int) {
        historyListeners.remove(kind)?.remove()
        if (limit == 0 || firebaseAuth.currentUser?.uid != userId) {
            _historyPageStates.update { it - kind }
            return
        }
        _historyPageStates.update { it + (kind to PageLoadState(loading = true)) }
        val recurring = kind == "deletedRecurring"
        val collection = firestore.collection("users").document(userId)
            .collection(if (recurring) "recurringReminders" else "reminders")
        val query = if (kind == "completed") {
            collection.whereEqualTo("isDeleted", false).whereEqualTo("isCompleted", true)
                .orderBy("completedAt", Query.Direction.DESCENDING)
        } else collection.whereEqualTo("isDeleted", true).orderBy("updatedAt", Query.Direction.DESCENDING)
        var active = true
        val registration = query.orderBy(FieldPath.documentId(), Query.Direction.DESCENDING)
            .limit((limit + 1).toLong()).addSnapshotListener { snapshot, error ->
                if (!active) return@addSnapshotListener
                if (error != null) {
                    _historyPageStates.update { it + (kind to PageLoadState(failed = true)) }
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener
                repositoryScope.launch {
                    for (change in snapshot.documentChanges) {
                        if (!active) break
                        // A bounded query can evict a row without deleting it.
                        val doc = if (change.type == DocumentChange.Type.REMOVED) {
                            try { change.document.reference.get().await() } catch (_: Exception) { continue }
                        } else change.document
                        if (!active) break
                        if (recurring) {
                            val existing = recurringDao.getById(doc.id)
                            if (existing?.syncStatus == SyncStatus.PENDING) continue
                            if (doc.exists()) recurringDao.upsert(doc.toRecurringReminderEntity(userId).copy(syncStatus = SyncStatus.SYNCED))
                            else if (existing != null) recurringDao.upsert(existing.copy(isDeleted = true, syncStatus = SyncStatus.SYNCED))
                        } else {
                            val existing = oneOffDao.getById(doc.id)
                            if (existing?.syncStatus == SyncStatus.PENDING) continue
                            if (doc.exists()) oneOffDao.upsert(doc.toOneOffReminderEntity(userId).copy(syncStatus = SyncStatus.SYNCED))
                            else if (existing != null) oneOffDao.upsert(existing.copy(isDeleted = true, syncStatus = SyncStatus.SYNCED))
                        }
                    }
                    if (active) _historyPageStates.update { it + (kind to PageLoadState()) }
                }
            }
        historyListeners[kind] = ListenerRegistration { active = false; registration.remove() }
    }

    fun stopHistoryListeners() {
        historyListeners.values.forEach { it.remove() }
        historyListeners.clear()
        _historyPageStates.value = emptyMap()
    }

    fun stopFirestoreListener() {
        stopHistoryListeners()
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
