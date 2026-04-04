package com.mountaincrab.crabdo.data.repository

import androidx.work.*
import com.mountaincrab.crabdo.data.local.dao.SubtaskDao
import com.mountaincrab.crabdo.data.local.entity.SubtaskEntity
import com.mountaincrab.crabdo.data.model.SyncStatus
import com.mountaincrab.crabdo.data.remote.SyncWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubtaskRepository @Inject constructor(
    private val subtaskDao: SubtaskDao,
    private val workManager: WorkManager
) {
    fun observeSubtasks(taskId: String) = subtaskDao.observeSubtasks(taskId)

    suspend fun createSubtask(taskId: String, title: String): SubtaskEntity {
        val existing = subtaskDao.getSubtasksByTask(taskId)
        val maxOrder = existing.maxOfOrNull { it.order } ?: 0.0
        val subtask = SubtaskEntity(taskId = taskId, title = title, order = maxOrder + 1.0)
        subtaskDao.upsert(subtask)
        enqueueSyncWork()
        return subtask
    }

    suspend fun setCompleted(subtaskId: String, isCompleted: Boolean) {
        subtaskDao.setCompleted(subtaskId, isCompleted)
        enqueueSyncWork()
    }

    suspend fun updateSubtask(subtask: SubtaskEntity) {
        subtaskDao.upsert(subtask.copy(
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        ))
        enqueueSyncWork()
    }

    suspend fun deleteSubtask(subtaskId: String) {
        subtaskDao.softDelete(subtaskId)
        enqueueSyncWork()
    }

    private fun enqueueSyncWork() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork("sync", ExistingWorkPolicy.REPLACE, request)
    }
}
