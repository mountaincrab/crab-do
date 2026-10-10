package com.mountaincrab.crabdo.data.repository

import androidx.work.*
import com.mountaincrab.crabdo.alarm.AlarmScheduler
import com.mountaincrab.crabdo.data.local.dao.ColumnDao
import com.mountaincrab.crabdo.data.local.dao.TaskDao
import com.mountaincrab.crabdo.data.local.entity.TaskEntity
import com.mountaincrab.crabdo.data.model.SyncStatus
import com.mountaincrab.crabdo.data.remote.SyncWorker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class TaskRepository(
    private val taskDao: TaskDao,
    private val columnDao: ColumnDao,
    private val alarmScheduler: AlarmScheduler,
    private val workManager: WorkManager
) {
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        // Observe Room once for the lifetime of the repository. This also reconciles
        // remote edits/deletions and deleted parent boards/columns, even off-screen.
        repositoryScope.launch {
            var previous = emptyMap<String, TaskEntity>()
            taskDao.observeTasksWithReminders().collect { tasks ->
                val current = tasks.associateBy { it.id }
                (previous.keys - current.keys).forEach { id ->
                    // Re-read because a snooze may have been saved after this emission.
                    val latest = taskDao.getActiveTaskById(id)
                    if (latest == null) alarmScheduler.cancelTaskReminder(id)
                    else scheduleTaskReminder(latest)
                }
                tasks.forEach { task ->
                    val old = previous[task.id]
                    if (old == null || old.title != task.title || old.reminderStyle != task.reminderStyle ||
                        old.nextReminderTimeMillis() != task.nextReminderTimeMillis()) {
                        taskDao.getActiveTaskById(task.id)?.let { scheduleTaskReminder(it) }
                    }
                }
                previous = current
            }
        }
    }

    fun observeTasksByColumn(columnId: String) = taskDao.observeTasksByColumn(columnId)
    fun observeTask(taskId: String) = taskDao.observeTask(taskId)
    fun observeReminderSummary(userId: String) = taskDao.observeReminderSummary(userId)

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
        scheduleTaskReminder(task)
        enqueueSyncWork()
    }

    /**
     * A task reminder is one-shot. Once it has fired, clear the scheduled time so
     * nothing can re-arm it — otherwise the task stays permanently "armed" and later
     * edits (or a reboot) would fire it again.
     */
    suspend fun onTaskReminderFired(taskId: String, triggerMillis: Long?): TaskEntity? {
        val task = fireTaskReminder(taskId, triggerMillis) ?: return null
        enqueueSyncWork()
        return task
    }

    // Catch up column transitions after a sync when the alarm could not run.
    // The caller pushes the PENDING changes; it must not enqueue another worker.
    suspend fun applyDueReminderTransitions(): Boolean {
        val now = System.currentTimeMillis()
        var changed = false
        taskDao.getTasksWithReminders().forEach { task ->
            val time = task.nextReminderTimeMillis() ?: return@forEach
            if (task.reminderTransitionColumnId != null && time <= now) {
                changed = (fireTaskReminder(task.id, time) != null) || changed
            }
        }
        return changed
    }

    private suspend fun fireTaskReminder(taskId: String, triggerMillis: Long?): TaskEntity? {
        val task = taskDao.getActiveTaskById(taskId) ?: return null
        val trigger = triggerMillis ?: task.nextReminderTimeMillis() ?: return null
        if (task.nextReminderTimeMillis() != trigger) return null
        val target = task.reminderTransitionColumnId
            ?.let { columnDao.getColumnById(it) }
            ?.takeIf { !it.isDeleted && it.boardId == task.boardId && it.id != task.columnId }
        val order = if (target != null) {
            (taskDao.observeTasksByColumn(target.id).first().maxOfOrNull { it.order } ?: 0.0) + 1.0
        } else null
        if (taskDao.consumeReminder(taskId, trigger, newColumnId = target?.id, newOrder = order) == 0) return null
        return task
    }

    suspend fun snoozeTaskReminder(taskId: String, millis: Long) {
        if (taskDao.snoozeReminder(taskId, millis) == 0) return
        taskDao.getTaskById(taskId)?.let { scheduleTaskReminder(it) }
        enqueueSyncWork()
    }

    suspend fun clearTaskReminder(taskId: String) {
        if (taskDao.clearReminder(taskId) == 0) return
        alarmScheduler.cancelTaskReminder(taskId)
        enqueueSyncWork()
    }

    private fun scheduleTaskReminder(task: TaskEntity) {
        val time = task.nextReminderTimeMillis()
        if (!task.isDeleted && time != null && time > System.currentTimeMillis()) {
            alarmScheduler.scheduleTaskReminder(task.id, task.title, time, task.reminderStyle)
        } else {
            alarmScheduler.cancelTaskReminder(task.id)
        }
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
        taskDao.softDelete(taskId)
        alarmScheduler.cancelTaskReminder(taskId)
        enqueueSyncWork()
    }

    suspend fun rescheduleAllTaskReminders() {
        taskDao.getTasksWithReminders().forEach { scheduleTaskReminder(it) }
    }

    private fun enqueueSyncWork() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork("sync", ExistingWorkPolicy.REPLACE, request)
    }
}
