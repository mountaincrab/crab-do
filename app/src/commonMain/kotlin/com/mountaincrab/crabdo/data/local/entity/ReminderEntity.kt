package com.mountaincrab.crabdo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mountaincrab.crabdo.data.model.SyncStatus
import com.mountaincrab.crabdo.util.currentTimeMillis
import com.mountaincrab.crabdo.util.randomUUID

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val id: String = randomUUID(),
    val userId: String,
    val title: String,
    val nextTriggerMillis: Long,
    val reminderStyle: ReminderStyle = ReminderStyle.ALARM,
    val recurrenceRuleJson: String? = null,
    val isEnabled: Boolean = true,
    val snoozedUntilMillis: Long? = null,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long = currentTimeMillis(),
    val updatedAt: Long = currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val isDeleted: Boolean = false
) {
    enum class ReminderStyle { ALARM, NOTIFICATION }
}
