package com.mountaincrab.crabdo.data.repository

import androidx.work.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.mountaincrab.crabdo.data.local.dao.BoardDao
import com.mountaincrab.crabdo.data.local.dao.ColumnDao
import com.mountaincrab.crabdo.data.local.dao.SubtaskDao
import com.mountaincrab.crabdo.data.local.dao.TaskDao
import com.mountaincrab.crabdo.data.local.entity.BoardEntity
import com.mountaincrab.crabdo.data.local.entity.ColumnEntity
import com.mountaincrab.crabdo.data.model.SyncStatus
import com.mountaincrab.crabdo.data.remote.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

class BoardRepository(
    private val boardDao: BoardDao,
    private val columnDao: ColumnDao,
    private val taskDao: TaskDao,
    private val subtaskDao: SubtaskDao,
    private val firestore: FirebaseFirestore,
    private val workManager: WorkManager
) {
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var boardListener: ListenerRegistration? = null
    private var columnListener: ListenerRegistration? = null
    private var taskListener: ListenerRegistration? = null
    private val subtaskListeners = mutableMapOf<String, ListenerRegistration>()

    fun observeBoards(userId: String) = boardDao.observeBoards(userId)
    fun observeBoard(boardId: String) = boardDao.observeBoard(boardId)
    fun observeColumns(boardId: String) = columnDao.observeColumnsByBoard(boardId)

    suspend fun createBoard(userId: String, title: String): BoardEntity {
        val board = BoardEntity(userId = userId, title = title)
        boardDao.upsert(board)
        enqueueSyncWork()
        return board
    }

    suspend fun updateBoard(board: BoardEntity) {
        boardDao.upsert(board.copy(
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        ))
        enqueueSyncWork()
    }

    suspend fun deleteBoard(boardId: String) {
        val board = boardDao.getBoardById(boardId) ?: return
        boardDao.upsert(board.copy(
            isDeleted = true,
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        ))
        enqueueSyncWork()
    }

    suspend fun createColumn(boardId: String, title: String): ColumnEntity {
        val existingColumns = columnDao.getColumnsByBoard(boardId)
        val maxOrder = existingColumns.maxOfOrNull { it.order } ?: 0.0
        val column = ColumnEntity(boardId = boardId, title = title, order = maxOrder + 1.0)
        columnDao.upsert(column)

        val board = boardDao.getBoardById(boardId)
        if (board != null) {
            val currentOrder = parseColumnOrder(board.columnOrder).toMutableList()
            currentOrder.add(column.id)
            boardDao.updateColumnOrder(boardId, serializeColumnOrder(currentOrder))
        }
        enqueueSyncWork()
        return column
    }

    suspend fun updateColumn(column: ColumnEntity) {
        columnDao.upsert(column.copy(
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        ))
        enqueueSyncWork()
    }

    suspend fun reorderColumns(boardId: String, newOrderedIds: List<String>) {
        val allColumns = columnDao.getColumnsByBoard(boardId)
        newOrderedIds.forEachIndexed { index, columnId ->
            val col = allColumns.find { it.id == columnId } ?: return@forEachIndexed
            columnDao.upsert(col.copy(
                order = (index + 1).toDouble(),
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING
            ))
        }
        boardDao.updateColumnOrder(boardId, serializeColumnOrder(newOrderedIds))
        enqueueSyncWork()
    }

    suspend fun deleteColumn(columnId: String) {
        columnDao.softDelete(columnId)
        enqueueSyncWork()
    }

    suspend fun setDefaultColumn(boardId: String, columnId: String) {
        val board = boardDao.getBoardById(boardId) ?: return
        boardDao.upsert(board.copy(
            defaultColumnId = columnId,
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING
        ))
        enqueueSyncWork()
    }

    fun triggerSync() = enqueueSyncWork()

    suspend fun refreshBoard(boardId: String) {
        val board = boardDao.getBoardById(boardId) ?: return
        val ownerUid = board.userId
        val boardRef = firestore.collection("users").document(ownerUid)
            .collection("boards").document(boardId)

        val boardDoc = boardRef.get().await()
        if (boardDoc.exists()) {
            boardDao.upsert(
                boardDoc.toBoardEntity(ownerUid).copy(
                    syncStatus = SyncStatus.SYNCED,
                    isShared = board.isShared
                )
            )
        }

        boardRef.collection("columns").get().await().documents.forEach { doc ->
            columnDao.upsert(doc.toColumnEntity().copy(syncStatus = SyncStatus.SYNCED))
        }

        boardRef.collection("tasks").get().await().documents.forEach { taskDoc ->
            taskDao.upsert(taskDoc.toTaskEntity().copy(syncStatus = SyncStatus.SYNCED))
            boardRef.collection("tasks").document(taskDoc.id)
                .collection("subtasks").get().await().documents.forEach { subDoc ->
                    subtaskDao.upsert(subDoc.toSubtaskEntity().copy(syncStatus = SyncStatus.SYNCED))
                }
        }
    }

    // ── Firestore listener ────────────────────────────────────────────────────

    /**
     * Reflects remote changes to a board's columns, tasks and subtasks into Room
     * as they happen, so an edit made on another device (or in the web app) shows
     * up while the board is open instead of waiting for the next [SyncWorker] run.
     * Mirrors [ReminderRepository.startFirestoreListener].
     *
     * Documents whose local row is still `PENDING` are skipped: that row has a
     * local write which [SyncWorker] has not pushed yet, and applying the remote
     * copy over it would silently drop the user's change.
     */
    fun startBoardListener(boardId: String) {
        stopBoardListener()
        repositoryScope.launch {
            // Shared boards live under the owner's subtree, not the local user's.
            val ownerUid = boardDao.getBoardById(boardId)?.userId ?: return@launch
            val boardRef = firestore.collection("users").document(ownerUid)
                .collection("boards").document(boardId)

            boardListener = boardRef.addSnapshotListener { doc, error ->
                if (error != null || doc == null || !doc.exists()) return@addSnapshotListener
                repositoryScope.launch {
                    val existing = boardDao.getBoardById(boardId)
                    if (existing?.syncStatus == SyncStatus.PENDING) return@launch
                    boardDao.upsert(
                        doc.toBoardEntity(ownerUid).copy(
                            syncStatus = SyncStatus.SYNCED,
                            isShared = existing?.isShared ?: false
                        )
                    )
                }
            }

            columnListener = boardRef.collection("columns")
                .addSnapshotListener { snap, error ->
                    if (error != null || snap == null) return@addSnapshotListener
                    repositoryScope.launch {
                        for (change in snap.documentChanges) {
                            val id = change.document.id
                            when (change.type) {
                                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                    if (columnDao.getColumnById(id)?.syncStatus == SyncStatus.PENDING) continue
                                    columnDao.upsert(
                                        change.document.toColumnEntity().copy(syncStatus = SyncStatus.SYNCED)
                                    )
                                }
                                DocumentChange.Type.REMOVED -> columnDao.softDelete(id)
                            }
                        }
                    }
                }

            taskListener = boardRef.collection("tasks")
                .addSnapshotListener { snap, error ->
                    if (error != null || snap == null) return@addSnapshotListener
                    // Subtasks are a per-task subcollection, so each task carries its
                    // own listener; attach/detach them as tasks come and go.
                    for (change in snap.documentChanges) {
                        val taskId = change.document.id
                        when (change.type) {
                            DocumentChange.Type.ADDED ->
                                if (!subtaskListeners.containsKey(taskId)) {
                                    subtaskListeners[taskId] = listenToSubtasks(boardRef, taskId)
                                }
                            DocumentChange.Type.REMOVED ->
                                subtaskListeners.remove(taskId)?.remove()
                            DocumentChange.Type.MODIFIED -> Unit
                        }
                    }
                    repositoryScope.launch {
                        for (change in snap.documentChanges) {
                            val id = change.document.id
                            when (change.type) {
                                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                    if (taskDao.getTaskById(id)?.syncStatus == SyncStatus.PENDING) continue
                                    taskDao.upsert(
                                        change.document.toTaskEntity().copy(syncStatus = SyncStatus.SYNCED)
                                    )
                                }
                                DocumentChange.Type.REMOVED -> taskDao.softDelete(id)
                            }
                        }
                    }
                }
        }
    }

    private fun listenToSubtasks(
        boardRef: com.google.firebase.firestore.DocumentReference,
        taskId: String
    ): ListenerRegistration =
        boardRef.collection("tasks").document(taskId).collection("subtasks")
            .addSnapshotListener { snap, error ->
                if (error != null || snap == null) return@addSnapshotListener
                repositoryScope.launch {
                    for (change in snap.documentChanges) {
                        val id = change.document.id
                        when (change.type) {
                            DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                if (subtaskDao.getSubtaskById(id)?.syncStatus == SyncStatus.PENDING) continue
                                subtaskDao.upsert(
                                    change.document.toSubtaskEntity().copy(syncStatus = SyncStatus.SYNCED)
                                )
                            }
                            DocumentChange.Type.REMOVED -> subtaskDao.softDelete(id)
                        }
                    }
                }
            }

    fun stopBoardListener() {
        boardListener?.remove(); boardListener = null
        columnListener?.remove(); columnListener = null
        taskListener?.remove(); taskListener = null
        subtaskListeners.values.forEach { it.remove() }
        subtaskListeners.clear()
    }

    private fun parseColumnOrder(json: String): List<String> =
        try { Json.decodeFromString<List<String>>(json) }
        catch (e: Exception) { emptyList() }

    private fun serializeColumnOrder(ids: List<String>): String = Json.encodeToString(ids)

    private fun enqueueSyncWork() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork("sync", ExistingWorkPolicy.REPLACE, request)
    }
}
