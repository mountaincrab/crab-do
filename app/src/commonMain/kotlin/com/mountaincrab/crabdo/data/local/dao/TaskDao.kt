package com.mountaincrab.crabdo.data.local.dao

import androidx.room.*
import com.mountaincrab.crabdo.data.local.entity.TaskEntity
import com.mountaincrab.crabdo.data.local.entity.TaskReminderSummary
import com.mountaincrab.crabdo.util.currentTimeMillis
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

    @Query("""
        SELECT t.* FROM tasks t
        JOIN boards b ON b.id = t.boardId AND b.isDeleted = 0
        JOIN columns c ON c.id = t.columnId AND c.boardId = t.boardId AND c.isDeleted = 0
        WHERE t.id = :taskId AND t.isDeleted = 0
    """)
    suspend fun getActiveTaskById(taskId: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE syncStatus != 'SYNCED' AND isDeleted = 0")
    suspend fun getUnsyncedTasks(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE isDeleted = 1 AND syncStatus != 'SYNCED'")
    suspend fun getDeletedUnsyncedTasks(): List<TaskEntity>

    @Query("UPDATE tasks SET syncStatus = 'SYNCED' WHERE id = :taskId")
    suspend fun markSynced(taskId: String)

    @Query("""
        SELECT t.* FROM tasks t
        JOIN boards b ON b.id = t.boardId AND b.isDeleted = 0
        JOIN columns c ON c.id = t.columnId AND c.boardId = t.boardId AND c.isDeleted = 0
        WHERE t.isDeleted = 0 AND (t.reminderTimeMillis IS NOT NULL OR t.snoozedUntilMillis IS NOT NULL)
    """)
    suspend fun getTasksWithReminders(): List<TaskEntity>

    @Query("""
        SELECT t.* FROM tasks t
        JOIN boards b ON b.id = t.boardId AND b.isDeleted = 0
        JOIN columns c ON c.id = t.columnId AND c.boardId = t.boardId AND c.isDeleted = 0
        WHERE t.isDeleted = 0 AND (t.reminderTimeMillis IS NOT NULL OR t.snoozedUntilMillis IS NOT NULL)
    """)
    fun observeTasksWithReminders(): Flow<List<TaskEntity>>

    @Query("""
        SELECT t.*, b.title AS boardTitle, c.title AS columnTitle FROM boards b
        JOIN tasks t ON t.boardId = b.id AND t.isDeleted = 0
        JOIN columns c ON c.id = t.columnId AND c.boardId = b.id AND c.isDeleted = 0
        WHERE b.isDeleted = 0
          AND b.id IN (
              SELECT id FROM boards WHERE userId = :userId
              UNION SELECT boardId FROM board_access WHERE userId = :userId
          )
          AND (t.reminderTimeMillis IS NOT NULL OR t.snoozedUntilMillis IS NOT NULL)
        ORDER BY COALESCE(t.snoozedUntilMillis, t.reminderTimeMillis), t.id
    """)
    fun observeReminderSummary(userId: String): Flow<List<TaskReminderSummary>>

    // Conditional updates keep a delivered old alarm from consuming a newer reminder,
    // and keep a stale snooze dialog from resurrecting a deleted task.
    @Query("""
        UPDATE tasks SET reminderTimeMillis = NULL, snoozedUntilMillis = NULL, reminderTransitionColumnId = NULL,
            columnId = COALESCE(:newColumnId, columnId), `order` = COALESCE(:newOrder, `order`),
            updatedAt = :updatedAt, syncStatus = 'PENDING'
        WHERE id = :taskId AND isDeleted = 0
          AND COALESCE(snoozedUntilMillis, reminderTimeMillis) = :triggerMillis
          AND boardId IN (SELECT id FROM boards WHERE isDeleted = 0)
          AND columnId IN (SELECT id FROM columns WHERE isDeleted = 0)
    """)
    suspend fun consumeReminder(taskId: String, triggerMillis: Long, updatedAt: Long = currentTimeMillis(), newColumnId: String? = null, newOrder: Double? = null): Int

    @Query("""
        UPDATE tasks SET snoozedUntilMillis = :millis, updatedAt = :updatedAt, syncStatus = 'PENDING'
        WHERE id = :taskId AND isDeleted = 0 AND reminderTimeMillis IS NULL
          AND boardId IN (SELECT id FROM boards WHERE isDeleted = 0)
          AND columnId IN (SELECT id FROM columns WHERE isDeleted = 0)
    """)
    suspend fun snoozeReminder(taskId: String, millis: Long, updatedAt: Long = currentTimeMillis()): Int

    @Query("UPDATE tasks SET isDeleted = 1, reminderTimeMillis = NULL, snoozedUntilMillis = NULL, reminderTransitionColumnId = NULL, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :taskId")
    suspend fun softDelete(taskId: String, updatedAt: Long = currentTimeMillis())
}
