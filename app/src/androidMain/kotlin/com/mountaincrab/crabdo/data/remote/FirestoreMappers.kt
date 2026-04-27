package com.mountaincrab.crabdo.data.remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.mountaincrab.crabdo.data.local.entity.BoardEntity
import com.mountaincrab.crabdo.data.local.entity.ColumnEntity
import com.mountaincrab.crabdo.data.local.entity.OneOffReminderEntity
import com.mountaincrab.crabdo.data.local.entity.RecurringReminderEntity
import com.mountaincrab.crabdo.data.local.entity.ReminderStyle
import com.mountaincrab.crabdo.data.local.entity.SubtaskEntity
import com.mountaincrab.crabdo.data.local.entity.TaskEntity
import com.mountaincrab.crabdo.data.model.SyncStatus
import com.mountaincrab.crabdo.util.currentTimeMillis

// ─── BoardEntity ─────────────────────────────────────────────────────────────

fun BoardEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    "userId" to userId,
    "title" to title,
    "columnOrder" to columnOrder,
    "createdAt" to createdAt,
    "updatedAt" to FieldValue.serverTimestamp(),
    "isDeleted" to isDeleted
)

@Suppress("UNCHECKED_CAST")
fun DocumentSnapshot.toBoardEntity(userId: String): BoardEntity {
    val collaborators = get("collaborators") as? Map<String, Any>
    return BoardEntity(
        id = id,
        userId = userId,
        title = getString("title") ?: "",
        columnOrder = getString("columnOrder") ?: "[]",
        createdAt = getLong("createdAt") ?: 0L,
        isShared = !collaborators.isNullOrEmpty(),
        updatedAt = getTimestamp("updatedAt")?.toDate()?.time ?: currentTimeMillis(),
        syncStatus = SyncStatus.SYNCED,
        isDeleted = getBoolean("isDeleted") ?: false
    )
}

// ─── ColumnEntity ─────────────────────────────────────────────────────────────

fun ColumnEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    "boardId" to boardId,
    "title" to title,
    "order" to order,
    "updatedAt" to FieldValue.serverTimestamp(),
    "isDeleted" to isDeleted
)

fun DocumentSnapshot.toColumnEntity(): ColumnEntity = ColumnEntity(
    id = id,
    boardId = getString("boardId") ?: "",
    title = getString("title") ?: "",
    order = getDouble("order") ?: 0.0,
    updatedAt = getTimestamp("updatedAt")?.toDate()?.time ?: currentTimeMillis(),
    syncStatus = SyncStatus.SYNCED,
    isDeleted = getBoolean("isDeleted") ?: false
)

// ─── TaskEntity ───────────────────────────────────────────────────────────────

fun TaskEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    "boardId" to boardId,
    "columnId" to columnId,
    "title" to title,
    "description" to description,
    "order" to order,
    "reminderTimeMillis" to reminderTimeMillis,
    "reminderStyle" to reminderStyle.name,
    "updatedAt" to FieldValue.serverTimestamp(),
    "isDeleted" to isDeleted
)

fun DocumentSnapshot.toTaskEntity(): TaskEntity = TaskEntity(
    id = id,
    boardId = getString("boardId") ?: "",
    columnId = getString("columnId") ?: "",
    title = getString("title") ?: "",
    description = getString("description") ?: "",
    order = getDouble("order") ?: 0.0,
    reminderTimeMillis = getLong("reminderTimeMillis"),
    reminderStyle = try {
        TaskEntity.ReminderStyle.valueOf(getString("reminderStyle") ?: "ALARM")
    } catch (e: Exception) { TaskEntity.ReminderStyle.ALARM },
    updatedAt = getTimestamp("updatedAt")?.toDate()?.time ?: currentTimeMillis(),
    syncStatus = SyncStatus.SYNCED,
    isDeleted = getBoolean("isDeleted") ?: false
)

// ─── SubtaskEntity ────────────────────────────────────────────────────────────

fun SubtaskEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    "taskId" to taskId,
    "title" to title,
    "isCompleted" to isCompleted,
    "order" to order,
    "updatedAt" to FieldValue.serverTimestamp(),
    "isDeleted" to isDeleted
)

fun DocumentSnapshot.toSubtaskEntity(): SubtaskEntity = SubtaskEntity(
    id = id,
    taskId = getString("taskId") ?: "",
    title = getString("title") ?: "",
    isCompleted = getBoolean("isCompleted") ?: false,
    order = getDouble("order") ?: 0.0,
    updatedAt = getTimestamp("updatedAt")?.toDate()?.time ?: currentTimeMillis(),
    syncStatus = SyncStatus.SYNCED,
    isDeleted = getBoolean("isDeleted") ?: false
)

// ─── OneOffReminderEntity ─────────────────────────────────────────────────────

fun OneOffReminderEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    "userId" to userId,
    "title" to title,
    "scheduledAt" to scheduledAt,
    "nextTriggerMillis" to scheduledAt,  // alias for webapp compatibility
    "reminderStyle" to reminderStyle.name,
    "isEnabled" to isEnabled,
    "snoozedUntilMillis" to snoozedUntilMillis,
    "isCompleted" to isCompleted,
    "completedAt" to completedAt,
    "createdAt" to createdAt,
    "updatedAt" to FieldValue.serverTimestamp(),
    "isDeleted" to isDeleted
)

fun DocumentSnapshot.toOneOffReminderEntity(userId: String): OneOffReminderEntity = OneOffReminderEntity(
    id = id,
    userId = userId,
    title = getString("title") ?: "",
    scheduledAt = getLong("scheduledAt") ?: getLong("nextTriggerMillis") ?: 0L,
    reminderStyle = try {
        ReminderStyle.valueOf(getString("reminderStyle") ?: "ALARM")
    } catch (e: Exception) { ReminderStyle.ALARM },
    isEnabled = getBoolean("isEnabled") ?: true,
    snoozedUntilMillis = getLong("snoozedUntilMillis"),
    isCompleted = getBoolean("isCompleted") ?: false,
    completedAt = getLong("completedAt"),
    createdAt = getLong("createdAt") ?: 0L,
    updatedAt = getTimestamp("updatedAt")?.toDate()?.time ?: currentTimeMillis(),
    syncStatus = SyncStatus.SYNCED,
    isDeleted = getBoolean("isDeleted") ?: false
)

// ─── RecurringReminderEntity ──────────────────────────────────────────────────

fun RecurringReminderEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    "userId" to userId,
    "title" to title,
    "recurrenceRuleJson" to recurrenceRuleJson,
    "startDate" to startDate,
    "reminderTime" to reminderTime,
    "nextFireAt" to nextFireAt,
    "reminderStyle" to reminderStyle.name,
    "isEnabled" to isEnabled,
    "snoozedUntilMillis" to snoozedUntilMillis,
    "createdAt" to createdAt,
    "updatedAt" to FieldValue.serverTimestamp(),
    "isDeleted" to isDeleted
)

fun DocumentSnapshot.toRecurringReminderEntity(userId: String): RecurringReminderEntity = RecurringReminderEntity(
    id = id,
    userId = userId,
    title = getString("title") ?: "",
    recurrenceRuleJson = getString("recurrenceRuleJson") ?: "",
    startDate = getLong("startDate") ?: 0L,
    reminderTime = getString("reminderTime") ?: "09:00",
    nextFireAt = getLong("nextFireAt") ?: 0L,
    reminderStyle = try {
        ReminderStyle.valueOf(getString("reminderStyle") ?: "ALARM")
    } catch (e: Exception) { ReminderStyle.ALARM },
    isEnabled = getBoolean("isEnabled") ?: true,
    snoozedUntilMillis = getLong("snoozedUntilMillis"),
    createdAt = getLong("createdAt") ?: 0L,
    updatedAt = getTimestamp("updatedAt")?.toDate()?.time ?: currentTimeMillis(),
    syncStatus = SyncStatus.SYNCED,
    isDeleted = getBoolean("isDeleted") ?: false
)
