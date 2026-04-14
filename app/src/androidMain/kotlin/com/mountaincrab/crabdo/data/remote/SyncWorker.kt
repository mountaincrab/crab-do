package com.mountaincrab.crabdo.data.remote

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.mountaincrab.crabdo.alarm.AlarmScheduler
import com.mountaincrab.crabdo.data.local.dao.*
import com.mountaincrab.crabdo.data.local.entity.BoardAccessEntity
import com.mountaincrab.crabdo.data.model.SyncStatus
import com.mountaincrab.crabdo.preferences.UserPreferencesRepository
import kotlinx.coroutines.tasks.await
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent {

    private val boardDao: BoardDao by inject()
    private val columnDao: ColumnDao by inject()
    private val taskDao: TaskDao by inject()
    private val subtaskDao: SubtaskDao by inject()
    private val reminderDao: ReminderDao by inject()
    private val alarmScheduler: AlarmScheduler by inject()
    private val boardAccessDao: BoardAccessDao by inject()
    private val firestore: FirebaseFirestore by inject()
    private val auth: FirebaseAuth by inject()
    private val prefs: UserPreferencesRepository by inject()

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

        userRef.collection("reminders")
            .whereGreaterThan("updatedAt", sinceTimestamp)
            .get().await().documents.forEach { doc ->
                val reminder = doc.toReminderEntity(userId).copy(syncStatus = SyncStatus.SYNCED)
                reminderDao.upsert(reminder)
                // Schedule alarm for active reminders pulled from remote (e.g. created on web app)
                if (reminder.isEnabled && !reminder.isCompleted && !reminder.isDeleted &&
                    reminder.nextTriggerMillis > System.currentTimeMillis()
                ) {
                    alarmScheduler.scheduleReminder(reminder)
                }
            }

        prefs.setLastSyncTimestamp(System.currentTimeMillis())
    }
}
