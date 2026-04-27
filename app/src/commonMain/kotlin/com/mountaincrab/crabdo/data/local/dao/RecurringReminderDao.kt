package com.mountaincrab.crabdo.data.local.dao

import androidx.room.*
import com.mountaincrab.crabdo.data.local.entity.RecurringReminderEntity
import com.mountaincrab.crabdo.util.currentTimeMillis
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringReminderDao {
    @Query("SELECT * FROM recurring_reminders WHERE userId = :userId AND isDeleted = 0 ORDER BY COALESCE(snoozedUntilMillis, nextFireAt)")
    fun observeActive(userId: String): Flow<List<RecurringReminderEntity>>

    @Query("SELECT * FROM recurring_reminders WHERE userId = :userId AND isDeleted = 1 ORDER BY updatedAt DESC")
    fun observeDeleted(userId: String): Flow<List<RecurringReminderEntity>>

    @Query("SELECT * FROM recurring_reminders WHERE id = :id")
    suspend fun getById(id: String): RecurringReminderEntity?

    @Query("SELECT * FROM recurring_reminders WHERE userId = :userId AND isEnabled = 1 AND isDeleted = 0")
    suspend fun getAllActive(userId: String): List<RecurringReminderEntity>

    @Query("SELECT * FROM recurring_reminders WHERE userId = :userId AND isEnabled = 1 AND isDeleted = 0 ORDER BY nextFireAt")
    fun observeAllActive(userId: String): Flow<List<RecurringReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(reminder: RecurringReminderEntity)

    @Query("SELECT * FROM recurring_reminders WHERE syncStatus != 'SYNCED' AND isDeleted = 0")
    suspend fun getUnsynced(): List<RecurringReminderEntity>

    @Query("SELECT * FROM recurring_reminders WHERE syncStatus != 'SYNCED' AND isDeleted = 1")
    suspend fun getDeletedUnsynced(): List<RecurringReminderEntity>

    @Query("UPDATE recurring_reminders SET syncStatus = 'SYNCED' WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("UPDATE recurring_reminders SET isDeleted = 1, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long = currentTimeMillis())

    @Query("UPDATE recurring_reminders SET snoozedUntilMillis = :millis WHERE id = :id")
    suspend fun updateSnooze(id: String, millis: Long?)

    @Query("UPDATE recurring_reminders SET nextFireAt = :nextFireAt, snoozedUntilMillis = NULL, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun advanceToNext(id: String, nextFireAt: Long, updatedAt: Long = currentTimeMillis())

    @Query("UPDATE recurring_reminders SET isDeleted = 0, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun restore(id: String, updatedAt: Long = currentTimeMillis())
}
