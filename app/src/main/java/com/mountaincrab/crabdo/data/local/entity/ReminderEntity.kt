package com.mountaincrab.crabdo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mountaincrab.crabdo.data.model.SyncStatus
import java.util.UUID

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val title: String,
    val nextTriggerMillis: Long,
    val reminderStyle: ReminderStyle = ReminderStyle.ALARM,
    val recurrenceRuleJson: String? = null,
    val isEnabled: Boolean = true,
    val snoozedUntilMillis: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val isDeleted: Boolean = false
) {
    enum class ReminderStyle { ALARM, NOTIFICATION }
}
