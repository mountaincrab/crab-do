package com.mountaincrab.crabdo.data.remote

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.mountaincrab.crabdo.data.local.dao.*
import com.mountaincrab.crabdo.data.model.SyncStatus
import com.mountaincrab.crabdo.preferences.UserPreferencesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val boardDao: BoardDao,
    private val columnDao: ColumnDao,
    private val taskDao: TaskDao,
    private val subtaskDao: SubtaskDao,
    private val reminderDao: ReminderDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val prefs: UserPreferencesRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val userId = auth.currentUser?.uid ?: return Result.failure()
        return try {
            pushPendingChanges(userId)
            pullRemoteChanges(userId)
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private suspend fun pushPendingChanges(userId: String) {
        val userRef = firestore.collection("users").document(userId)

        boardDao.getUnsyncedBoards().forEach { board ->
            userRef.collection("boards").document(board.id)
                .set(board.toFirestoreMap(), SetOptions.merge()).await()
            boardDao.markSynced(board.id)
        }
        boardDao.getDeletedUnsyncedBoards().forEach { board ->
            userRef.collection("boards").document(board.id)
                .set(mapOf("isDeleted" to true), SetOptions.merge()).await()
            boardDao.markSynced(board.id)
        }

        columnDao.getUnsyncedColumns().forEach { col ->
            userRef.collection("boards").document(col.boardId)
                .collection("columns").document(col.id)
                .set(col.toFirestoreMap(), SetOptions.merge()).await()
            columnDao.markSynced(col.id)
        }

        taskDao.getUnsyncedTasks().forEach { task ->
            userRef.collection("boards").document(task.boardId)
                .collection("tasks").document(task.id)
                .set(task.toFirestoreMap(), SetOptions.merge()).await()
            taskDao.markSynced(task.id)
        }
        taskDao.getDeletedUnsyncedTasks().forEach { task ->
            userRef.collection("boards").document(task.boardId)
                .collection("tasks").document(task.id)
                .set(mapOf("isDeleted" to true), SetOptions.merge()).await()
            taskDao.markSynced(task.id)
        }

        subtaskDao.getUnsyncedSubtasks().forEach { subtask ->
            val task = taskDao.getTaskById(subtask.taskId) ?: return@forEach
            userRef.collection("boards").document(task.boardId)
                .collection("tasks").document(subtask.taskId)
                .collection("subtasks").document(subtask.id)
                .set(subtask.toFirestoreMap(), SetOptions.merge()).await()
            subtaskDao.markSynced(subtask.id)
        }

        reminderDao.getUnsyncedReminders().forEach { reminder ->
            userRef.collection("reminders").document(reminder.id)
                .set(reminder.toFirestoreMap(), SetOptions.merge()).await()
            reminderDao.markSynced(reminder.id)
        }
    }

    private suspend fun pullRemoteChanges(userId: String) {
        val sinceTimestamp = Timestamp(prefs.getLastSyncTimestamp() / 1000, 0)
        val userRef = firestore.collection("users").document(userId)

        userRef.collection("boards")
            .whereGreaterThan("updatedAt", sinceTimestamp)
            .get().await().documents.forEach { doc ->
                boardDao.upsert(doc.toBoardEntity(userId).copy(syncStatus = SyncStatus.SYNCED))
            }

        userRef.collection("reminders")
            .whereGreaterThan("updatedAt", sinceTimestamp)
            .get().await().documents.forEach { doc ->
                reminderDao.upsert(doc.toReminderEntity(userId).copy(syncStatus = SyncStatus.SYNCED))
            }

        prefs.setLastSyncTimestamp(System.currentTimeMillis())
    }
}
