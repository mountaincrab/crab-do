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
import com.mountaincrab.crabdo.data.local.entity.BoardAccessEntity
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
    private val boardAccessDao: BoardAccessDao,
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

        // Push boards — use board.userId (the owner) for the Firestore path so
        // collaborators write to the owner's document tree.
        boardDao.getUnsyncedBoards().forEach { board ->
            val ownerRef = firestore.collection("users").document(board.userId)
            ownerRef.collection("boards").document(board.id)
                .set(board.toFirestoreMap(), SetOptions.merge()).await()
            boardDao.markSynced(board.id)
        }
        boardDao.getDeletedUnsyncedBoards().forEach { board ->
            val ownerRef = firestore.collection("users").document(board.userId)
            ownerRef.collection("boards").document(board.id)
                .set(mapOf("isDeleted" to true), SetOptions.merge()).await()
            boardDao.markSynced(board.id)
        }

        // For columns/tasks/subtasks, look up the board to get the owner UID.
        columnDao.getUnsyncedColumns().forEach { col ->
            val board = boardDao.getBoardById(col.boardId) ?: return@forEach
            val ownerRef = firestore.collection("users").document(board.userId)
            ownerRef.collection("boards").document(col.boardId)
                .collection("columns").document(col.id)
                .set(col.toFirestoreMap(), SetOptions.merge()).await()
            columnDao.markSynced(col.id)
        }

        taskDao.getUnsyncedTasks().forEach { task ->
            val board = boardDao.getBoardById(task.boardId) ?: return@forEach
            val ownerRef = firestore.collection("users").document(board.userId)
            ownerRef.collection("boards").document(task.boardId)
                .collection("tasks").document(task.id)
                .set(task.toFirestoreMap(), SetOptions.merge()).await()
            taskDao.markSynced(task.id)
        }
        taskDao.getDeletedUnsyncedTasks().forEach { task ->
            val board = boardDao.getBoardById(task.boardId) ?: return@forEach
            val ownerRef = firestore.collection("users").document(board.userId)
            ownerRef.collection("boards").document(task.boardId)
                .collection("tasks").document(task.id)
                .set(mapOf("isDeleted" to true), SetOptions.merge()).await()
            taskDao.markSynced(task.id)
        }

        subtaskDao.getUnsyncedSubtasks().forEach { subtask ->
            val task = taskDao.getTaskById(subtask.taskId) ?: return@forEach
            val board = boardDao.getBoardById(task.boardId) ?: return@forEach
            val ownerRef = firestore.collection("users").document(board.userId)
            ownerRef.collection("boards").document(task.boardId)
                .collection("tasks").document(subtask.taskId)
                .collection("subtasks").document(subtask.id)
                .set(subtask.toFirestoreMap(), SetOptions.merge()).await()
            subtaskDao.markSynced(subtask.id)
        }

        // Reminders are always owner-only, so use the current user's path.
        reminderDao.getUnsyncedReminders().forEach { reminder ->
            userRef.collection("reminders").document(reminder.id)
                .set(reminder.toFirestoreMap(), SetOptions.merge()).await()
            reminderDao.markSynced(reminder.id)
        }
    }

    private suspend fun pullRemoteChanges(userId: String) {
        val sinceTimestamp = Timestamp(prefs.getLastSyncTimestamp() / 1000, 0)
        val userRef = firestore.collection("users").document(userId)

        // Pull own boards
        userRef.collection("boards")
            .whereGreaterThan("updatedAt", sinceTimestamp)
            .get().await().documents.forEach { doc ->
                boardDao.upsert(doc.toBoardEntity(userId).copy(syncStatus = SyncStatus.SYNCED))
            }

        // Pull columns, tasks, and subtasks for own boards
        boardDao.getBoardIdsForUser(userId).forEach { boardId ->
            val boardRef = userRef.collection("boards").document(boardId)
            boardRef.collection("columns")
                .whereGreaterThan("updatedAt", sinceTimestamp)
                .get().await().documents.forEach { doc ->
                    columnDao.upsert(doc.toColumnEntity().copy(syncStatus = SyncStatus.SYNCED))
                }
            boardRef.collection("tasks")
                .whereGreaterThan("updatedAt", sinceTimestamp)
                .get().await().documents.forEach { taskDoc ->
                    taskDao.upsert(taskDoc.toTaskEntity().copy(syncStatus = SyncStatus.SYNCED))
                    boardRef.collection("tasks").document(taskDoc.id)
                        .collection("subtasks")
                        .whereGreaterThan("updatedAt", sinceTimestamp)
                        .get().await().documents.forEach { subDoc ->
                            subtaskDao.upsert(subDoc.toSubtaskEntity().copy(syncStatus = SyncStatus.SYNCED))
                        }
                }
        }

        // Pull shared boards the user collaborates on
        boardAccessDao.getSharedBoardAccess(userId).forEach { access ->
            val ownerRef = firestore.collection("users").document(access.ownerUserId)
            val boardDoc = ownerRef.collection("boards").document(access.boardId).get().await()
            if (boardDoc.exists()) {
                boardDao.upsert(
                    boardDoc.toBoardEntity(access.ownerUserId).copy(
                        syncStatus = SyncStatus.SYNCED,
                        isShared = true
                    )
                )
                // Pull columns and tasks for shared boards
                ownerRef.collection("boards").document(access.boardId)
                    .collection("columns").get().await().documents.forEach { doc ->
                        columnDao.upsert(doc.toColumnEntity().copy(syncStatus = SyncStatus.SYNCED))
                    }
                ownerRef.collection("boards").document(access.boardId)
                    .collection("tasks").get().await().documents.forEach { taskDoc ->
                        taskDao.upsert(taskDoc.toTaskEntity().copy(syncStatus = SyncStatus.SYNCED))
                        ownerRef.collection("boards").document(access.boardId)
                            .collection("tasks").document(taskDoc.id)
                            .collection("subtasks").get().await().documents.forEach { subDoc ->
                                subtaskDao.upsert(subDoc.toSubtaskEntity().copy(syncStatus = SyncStatus.SYNCED))
                            }
                    }
            }
        }

        // Pull reminders (owner-only)
        userRef.collection("reminders")
            .whereGreaterThan("updatedAt", sinceTimestamp)
            .get().await().documents.forEach { doc ->
                reminderDao.upsert(doc.toReminderEntity(userId).copy(syncStatus = SyncStatus.SYNCED))
            }

        prefs.setLastSyncTimestamp(System.currentTimeMillis())
    }
}
