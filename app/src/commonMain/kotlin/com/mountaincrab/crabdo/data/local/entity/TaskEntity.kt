package com.mountaincrab.crabdo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mountaincrab.crabdo.data.model.SyncStatus
import com.mountaincrab.crabdo.util.currentTimeMillis
import com.mountaincrab.crabdo.util.randomUUID

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String = randomUUID(),
    val boardId: String,
    val columnId: String,
    val title: String,
    val description: String = "",
    val order: Double = 0.0,
    val reminderTimeMillis: Long? = null,
    val reminderStyle: ReminderStyle = ReminderStyle.ALARM,
    val updatedAt: Long = currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val isDeleted: Boolean = false
) {
    enum class ReminderStyle { ALARM, NOTIFICATION }
}
