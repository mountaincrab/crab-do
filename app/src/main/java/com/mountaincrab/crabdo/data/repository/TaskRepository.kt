package com.mountaincrab.crabdo.data.repository

import androidx.work.*
import com.mountaincrab.crabdo.alarm.AlarmScheduler
import com.mountaincrab.crabdo.data.local.dao.TaskDao
import com.mountaincrab.crabdo.data.local.entity.TaskEntity
import com.mountaincrab.crabdo.data.model.SyncStatus
import com.mountaincrab.crabdo.data.remote.SyncWorker
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val alarmScheduler: AlarmScheduler,
    private val workManager: WorkManager
) {
    fun observeTasksByColumn(columnId: String) = taskDao.observeTasksByColumn(columnId)
    fun observeTask(taskId: String) = taskDao.observeTask(taskId)

    suspend fun createTask(
        boardId: String,
        columnId: String,
        title: String,
        description: String = "",
        reminderTimeMillis: Long? = null,
        reminderStyle: TaskEntity.ReminderStyle = TaskEntity.ReminderStyle.ALARM
    ): TaskEntity {
        val tasks = taskDao.observeTasksByColumn(columnId).first()
        val maxOrder = tasks.maxOfOrNull { it.order } ?: 0.0
        val task = TaskEntity(
            boardId = boardId, columnId = columnId,
            title = title, description = description, order = maxOrder + 1.0,
            reminderTimeMillis = reminderTimeMillis,
            reminderStyle = reminderStyle
        )
        taskDao.upsert(task)
        if (reminderTimeMillis != null && reminderTimeMillis > System.currentTimeMillis()) {
            alarmScheduler.scheduleTaskReminder(task.id, reminderTimeMillis, reminderStyle)
        }
        enqueueSyncWork()
        return task
    }

    suspend fun updateTask(task: TaskEntity) {
        taskDao.upsert(task.copy(
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        ))
        task.reminderTimeMillis?.let { time ->
            alarmScheduler.scheduleTaskReminder(task.id, time, task.reminderStyle)
        } ?: alarmScheduler.cancelTaskReminder(task.id)
        enqueueSyncWork()
    }

    suspend fun moveTask(taskId: String, newColumnId: String,
                         orderBefore: Double, orderAfter: Double) {
        val task = taskDao.getTaskById(taskId) ?: return
        val newOrder = if (orderAfter <= orderBefore) orderBefore + 1.0
                       else (orderBefore + orderAfter) / 2.0
        taskDao.upsert(task.copy(
            columnId = newColumnId,
            order = newOrder,
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        ))
        enqueueSyncWork()
    }

    suspend fun deleteTask(taskId: String) {
        alarmScheduler.cancelTaskReminder(taskId)
        taskDao.softDelete(taskId)
        enqueueSyncWork()
    }

    suspend fun rescheduleAllTaskReminders() {
        taskDao.getTasksWithReminders().forEach { task ->
            task.reminderTimeMillis?.let { time ->
                if (time > System.currentTimeMillis()) {
                    alarmScheduler.scheduleTaskReminder(task.id, time, task.reminderStyle)
                }
            }
        }
    }

    private fun enqueueSyncWork() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork("sync", ExistingWorkPolicy.REPLACE, request)
    }
}
