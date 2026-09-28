package com.mountaincrab.crabdo.data.repository

import androidx.work.*
import com.mountaincrab.crabdo.alarm.AlarmScheduler
import com.mountaincrab.crabdo.data.local.dao.ColumnDao
import com.mountaincrab.crabdo.data.local.dao.TaskDao
import com.mountaincrab.crabdo.data.local.entity.TaskEntity
import com.mountaincrab.crabdo.data.model.SyncStatus
import com.mountaincrab.crabdo.data.remote.SyncWorker
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class TaskRepository(
    private val taskDao: TaskDao,
    private val columnDao: ColumnDao,
    private val alarmScheduler: AlarmScheduler,
    private val workManager: WorkManager
) {
    fun observeTasksByColumn(columnId: String) = taskDao.observeTasksByColumn(columnId)
    fun observeTask(taskId: String) = taskDao.observeTask(taskId)

    suspend fun getTask(taskId: String) = taskDao.getTaskById(taskId)

    suspend fun createTask(
        boardId: String,
        columnId: String,
        title: String,
        description: String = "",
        reminderTimeMillis: Long? = null,
        reminderStyle: TaskEntity.ReminderStyle = TaskEntity.ReminderStyle.ALARM,
        reminderTransitionColumnId: String? = null
    ): TaskEntity {
        val tasks = taskDao.observeTasksByColumn(columnId).first()
        val maxOrder = tasks.maxOfOrNull { it.order } ?: 0.0
        val task = TaskEntity(
            boardId = boardId, columnId = columnId,
            title = title, description = description, order = maxOrder + 1.0,
            reminderTimeMillis = reminderTimeMillis,
            reminderStyle = reminderStyle,
            reminderTransitionColumnId = reminderTransitionColumnId.takeIf { reminderTimeMillis != null }
        )
        taskDao.upsert(task)
        if (reminderTimeMillis != null && reminderTimeMillis > System.currentTimeMillis()) {
            alarmScheduler.scheduleTaskReminder(task.id, task.title, reminderTimeMillis, reminderStyle)
        }
        enqueueSyncWork()
        return task
    }

    suspend fun updateTask(task: TaskEntity) {
        taskDao.upsert(task.copy(
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        ))
        // Only re-arm a reminder that is still in the future. AlarmManager delivers a
        // past trigger time immediately, so rescheduling an already-fired reminder here
        // would re-ping the user on every subsequent edit of the task.
        val time = task.reminderTimeMillis
        if (time != null && time > System.currentTimeMillis()) {
            alarmScheduler.scheduleTaskReminder(task.id, task.title, time, task.reminderStyle)
        } else {
            alarmScheduler.cancelTaskReminder(task.id)
        }
        enqueueSyncWork()
    }

    /**
     * A task reminder is one-shot. Once it has fired, clear the scheduled time so
     * nothing can re-arm it — otherwise the task stays permanently "armed" and later
     * edits (or a reboot) would fire it again. If the reminder carries a transition
     * target, the task is also moved to the bottom of that column.
     */
    suspend fun onTaskReminderFired(taskId: String) {
        if (fireTaskReminder(taskId)) enqueueSyncWork()
    }

    /**
     * Catch-up for reminders whose alarm never ran on this device (phone off, process
     * dead, or the reminder was set on another device): apply any due column
     * transition. Called by [SyncWorker] after a pull, so it acts on fresh data; it
     * doesn't enqueue a sync — the caller pushes the resulting PENDING rows.
     *
     * @return true if any task was changed.
     */
    suspend fun applyDueReminderTransitions(): Boolean {
        val now = System.currentTimeMillis()
        var changed = false
        taskDao.getTasksWithReminders().forEach { task ->
            val time = task.reminderTimeMillis ?: return@forEach
            if (task.reminderTransitionColumnId != null && time <= now) {
                changed = fireTaskReminder(task.id) || changed
            }
        }
        return changed
    }

    private suspend fun fireTaskReminder(taskId: String): Boolean {
        val task = taskDao.getTaskById(taskId) ?: return false
        if (task.isDeleted || task.reminderTimeMillis == null) return false
        // Only move into a live column on the same board; a deleted target is ignored.
        val target = task.reminderTransitionColumnId
            ?.let { columnDao.getColumnById(it) }
            ?.takeIf { !it.isDeleted && it.boardId == task.boardId && it.id != task.columnId }
        val order = if (target != null) {
            (taskDao.observeTasksByColumn(target.id).first().maxOfOrNull { it.order } ?: 0.0) + 1.0
        } else task.order
        taskDao.upsert(task.copy(
            columnId = target?.id ?: task.columnId,
            order = order,
            reminderTimeMillis = null,
            reminderTransitionColumnId = null,
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        ))
        alarmScheduler.cancelTaskReminder(taskId)
        return true
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
                    alarmScheduler.scheduleTaskReminder(task.id, task.title, time, task.reminderStyle)
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
