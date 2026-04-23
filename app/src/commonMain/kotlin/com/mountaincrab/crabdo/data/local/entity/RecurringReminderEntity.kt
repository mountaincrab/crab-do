package com.mountaincrab.crabdo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mountaincrab.crabdo.data.model.SyncStatus
import com.mountaincrab.crabdo.util.currentTimeMillis
import com.mountaincrab.crabdo.util.randomUUID

@Entity(tableName = "recurring_reminders")
data class RecurringReminderEntity(
    @PrimaryKey val id: String = randomUUID(),
    val userId: String,
    val title: String,
    val recurrenceRuleJson: String,
    val startDate: Long,        // midnight local — anchors the recurrence cycle
    val reminderTime: String,   // "HH:mm"
    val nextFireAt: Long,       // cached: when to schedule the next alarm
    val reminderStyle: ReminderStyle = ReminderStyle.ALARM,
    val isEnabled: Boolean = true,
    val snoozedUntilMillis: Long? = null,
    val createdAt: Long = currentTimeMillis(),
    val updatedAt: Long = currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val isDeleted: Boolean = false
)
