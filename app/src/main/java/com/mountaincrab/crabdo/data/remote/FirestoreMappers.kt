package com.mountaincrab.crabdo.data.remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.mountaincrab.crabdo.data.local.entity.*
import com.mountaincrab.crabdo.data.model.SyncStatus

// ─── BoardEntity ─────────────────────────────────────────────────────────────

fun BoardEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    "userId" to userId,
    "title" to title,
    "columnOrder" to columnOrder,
    "createdAt" to createdAt,
    "updatedAt" to FieldValue.serverTimestamp(),
    "isDeleted" to isDeleted
)

fun DocumentSnapshot.toBoardEntity(userId: String): BoardEntity = BoardEntity(
    id = id,
    userId = userId,
    title = getString("title") ?: "",
    columnOrder = getString("columnOrder") ?: "[]",
    createdAt = getLong("createdAt") ?: 0L,
    updatedAt = getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis(),
    syncStatus = SyncStatus.SYNCED,
    isDeleted = getBoolean("isDeleted") ?: false
)

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
    updatedAt = getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis(),
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
    updatedAt = getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis(),
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
    updatedAt = getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis(),
    syncStatus = SyncStatus.SYNCED,
    isDeleted = getBoolean("isDeleted") ?: false
)

// ─── ReminderEntity ───────────────────────────────────────────────────────────

fun ReminderEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    "userId" to userId,
    "title" to title,
    "nextTriggerMillis" to nextTriggerMillis,
    "reminderStyle" to reminderStyle.name,
    "recurrenceRuleJson" to recurrenceRuleJson,
    "isEnabled" to isEnabled,
    "createdAt" to createdAt,
    "updatedAt" to FieldValue.serverTimestamp(),
    "isDeleted" to isDeleted
)

fun DocumentSnapshot.toReminderEntity(userId: String): ReminderEntity = ReminderEntity(
    id = id,
    userId = userId,
    title = getString("title") ?: "",
    nextTriggerMillis = getLong("nextTriggerMillis") ?: 0L,
    reminderStyle = try {
        ReminderEntity.ReminderStyle.valueOf(getString("reminderStyle") ?: "ALARM")
    } catch (e: Exception) { ReminderEntity.ReminderStyle.ALARM },
    recurrenceRuleJson = getString("recurrenceRuleJson"),
    isEnabled = getBoolean("isEnabled") ?: true,
    createdAt = getLong("createdAt") ?: 0L,
    updatedAt = getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis(),
    syncStatus = SyncStatus.SYNCED,
    isDeleted = getBoolean("isDeleted") ?: false
)
