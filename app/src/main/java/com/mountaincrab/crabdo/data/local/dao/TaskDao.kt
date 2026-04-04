package com.mountaincrab.crabdo.data.local.dao

import androidx.room.*
import com.mountaincrab.crabdo.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE columnId = :columnId AND isDeleted = 0 ORDER BY `order`")
    fun observeTasksByColumn(columnId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE boardId = :boardId AND isDeleted = 0")
    fun observeTasksByBoard(boardId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun observeTask(taskId: String): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE syncStatus != 'SYNCED' AND isDeleted = 0")
    suspend fun getUnsyncedTasks(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE isDeleted = 1 AND syncStatus != 'SYNCED'")
    suspend fun getDeletedUnsyncedTasks(): List<TaskEntity>

    @Query("UPDATE tasks SET syncStatus = 'SYNCED' WHERE id = :taskId")
    suspend fun markSynced(taskId: String)

    @Query("SELECT * FROM tasks WHERE reminderTimeMillis IS NOT NULL AND isDeleted = 0")
    suspend fun getTasksWithReminders(): List<TaskEntity>

    @Query("UPDATE tasks SET isDeleted = 1, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :taskId")
    suspend fun softDelete(taskId: String, updatedAt: Long = System.currentTimeMillis())
}
