package com.mountaincrab.crabdo.data.local.dao

import androidx.room.*
import com.mountaincrab.crabdo.data.local.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE userId = :userId AND isDeleted = 0 ORDER BY nextTriggerMillis")
    fun observeReminders(userId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: String): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE isEnabled = 1 AND isDeleted = 0")
    suspend fun getAllActiveReminders(): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(reminder: ReminderEntity)

    @Query("SELECT * FROM reminders WHERE syncStatus != 'SYNCED' AND isDeleted = 0")
    suspend fun getUnsyncedReminders(): List<ReminderEntity>

    @Query("UPDATE reminders SET syncStatus = 'SYNCED' WHERE id = :reminderId")
    suspend fun markSynced(reminderId: String)

    @Query("UPDATE reminders SET nextTriggerMillis = :nextTriggerMillis, updatedAt = :updatedAt WHERE id = :reminderId")
    suspend fun updateNextTrigger(reminderId: String, nextTriggerMillis: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE reminders SET isDeleted = 1, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :reminderId")
    suspend fun softDelete(reminderId: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE reminders SET snoozedUntilMillis = :millis WHERE id = :reminderId")
    suspend fun updateSnoozeUntil(reminderId: String, millis: Long?)
}
