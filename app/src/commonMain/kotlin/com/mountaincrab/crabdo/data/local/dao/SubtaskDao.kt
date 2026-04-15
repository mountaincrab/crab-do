package com.mountaincrab.crabdo.data.local.dao

import androidx.room.*
import com.mountaincrab.crabdo.data.local.entity.SubtaskEntity
import com.mountaincrab.crabdo.util.currentTimeMillis
import kotlinx.coroutines.flow.Flow

@Dao
interface SubtaskDao {
    @Query("SELECT * FROM subtasks WHERE taskId = :taskId AND isDeleted = 0 ORDER BY `order`")
    fun observeSubtasks(taskId: String): Flow<List<SubtaskEntity>>

    @Query("SELECT * FROM subtasks WHERE taskId = :taskId AND isDeleted = 0 ORDER BY `order`")
    suspend fun getSubtasksByTask(taskId: String): List<SubtaskEntity>

    @Query("SELECT * FROM subtasks WHERE id = :subtaskId")
    suspend fun getSubtaskById(subtaskId: String): SubtaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(subtask: SubtaskEntity)

    @Query("UPDATE subtasks SET isCompleted = :isCompleted, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :subtaskId")
    suspend fun setCompleted(subtaskId: String, isCompleted: Boolean, updatedAt: Long = currentTimeMillis())

    @Query("SELECT * FROM subtasks WHERE syncStatus != 'SYNCED'")
    suspend fun getUnsyncedSubtasks(): List<SubtaskEntity>

    @Query("UPDATE subtasks SET syncStatus = 'SYNCED' WHERE id = :subtaskId")
    suspend fun markSynced(subtaskId: String)

    @Query("UPDATE subtasks SET isDeleted = 1, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :subtaskId")
    suspend fun softDelete(subtaskId: String, updatedAt: Long = currentTimeMillis())
}
