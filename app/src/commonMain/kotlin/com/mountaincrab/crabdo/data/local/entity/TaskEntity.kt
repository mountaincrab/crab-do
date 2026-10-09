package com.mountaincrab.crabdo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import com.mountaincrab.crabdo.data.model.SyncStatus
import com.mountaincrab.crabdo.util.currentTimeMillis
import com.mountaincrab.crabdo.util.randomUUID

@Entity(tableName = "tasks", indices = [Index(value = ["boardId", "isDeleted"])])
data class TaskEntity(
    @PrimaryKey val id: String = randomUUID(),
    val boardId: String,
    val columnId: String,
    val title: String,
    val description: String = "",
    val order: Double = 0.0,
    val reminderTimeMillis: Long? = null,
    val snoozedUntilMillis: Long? = null,
    val reminderStyle: ReminderStyle = ReminderStyle.ALARM,
    // Column to move the task into when its reminder fires. Null = don't move.
    val reminderTransitionColumnId: String? = null,
    val updatedAt: Long = currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val isDeleted: Boolean = false
) {
    enum class ReminderStyle { ALARM, NOTIFICATION }

    fun nextReminderTimeMillis(): Long? = snoozedUntilMillis ?: reminderTimeMillis
}
