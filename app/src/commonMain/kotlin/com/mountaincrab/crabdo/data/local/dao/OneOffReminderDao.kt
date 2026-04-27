package com.mountaincrab.crabdo.data.local.dao

import androidx.room.*
import com.mountaincrab.crabdo.data.local.entity.OneOffReminderEntity
import com.mountaincrab.crabdo.util.currentTimeMillis
import kotlinx.coroutines.flow.Flow

@Dao
interface OneOffReminderDao {
    @Query("SELECT * FROM one_off_reminders WHERE userId = :userId AND isDeleted = 0 AND isCompleted = 0 ORDER BY scheduledAt")
    fun observeActive(userId: String): Flow<List<OneOffReminderEntity>>

    @Query("SELECT * FROM one_off_reminders WHERE userId = :userId AND isDeleted = 0 AND isCompleted = 1 ORDER BY completedAt DESC")
    fun observeCompleted(userId: String): Flow<List<OneOffReminderEntity>>

    @Query("SELECT * FROM one_off_reminders WHERE id = :id")
    suspend fun getById(id: String): OneOffReminderEntity?

    @Query("SELECT * FROM one_off_reminders WHERE userId = :userId AND isEnabled = 1 AND isDeleted = 0 AND isCompleted = 0")
    suspend fun getAllActive(userId: String): List<OneOffReminderEntity>

    @Query("SELECT * FROM one_off_reminders WHERE userId = :userId AND isEnabled = 1 AND isDeleted = 0 AND isCompleted = 0 ORDER BY scheduledAt")
    fun observeAllActive(userId: String): Flow<List<OneOffReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(reminder: OneOffReminderEntity)

    @Query("SELECT * FROM one_off_reminders WHERE syncStatus != 'SYNCED' AND isDeleted = 0")
    suspend fun getUnsynced(): List<OneOffReminderEntity>

    @Query("SELECT * FROM one_off_reminders WHERE syncStatus != 'SYNCED' AND isDeleted = 1")
    suspend fun getDeletedUnsynced(): List<OneOffReminderEntity>

    @Query("UPDATE one_off_reminders SET syncStatus = 'SYNCED' WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("UPDATE one_off_reminders SET isDeleted = 1, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long = currentTimeMillis())

    @Query("UPDATE one_off_reminders SET snoozedUntilMillis = :millis WHERE id = :id")
    suspend fun updateSnooze(id: String, millis: Long?)

    @Query("""
        UPDATE one_off_reminders
        SET snoozedUntilMillis = :millis,
            isCompleted = 0, completedAt = NULL,
            updatedAt = :updatedAt, syncStatus = 'PENDING'
        WHERE id = :id
    """)
    suspend fun snoozeAndReactivate(id: String, millis: Long, updatedAt: Long = currentTimeMillis())

    @Query("UPDATE one_off_reminders SET isCompleted = 1, completedAt = :completedAt, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun markCompleted(id: String, completedAt: Long = currentTimeMillis(), updatedAt: Long = currentTimeMillis())

    @Query("SELECT * FROM one_off_reminders WHERE userId = :userId AND isDeleted = 1 ORDER BY updatedAt DESC")
    fun observeDeleted(userId: String): Flow<List<OneOffReminderEntity>>

    @Query("UPDATE one_off_reminders SET isDeleted = 0, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun restore(id: String, updatedAt: Long = currentTimeMillis())
}
