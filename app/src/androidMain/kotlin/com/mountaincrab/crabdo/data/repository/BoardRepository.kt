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
    private val listenerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var boardsListener: ListenerRegistration? = null
    // Per-board child listeners (columns + tasks), keyed by boardId. Guarded by [listenersLock].
    private val childListeners = mutableMapOf<String, List<ListenerRegistration>>()
    private val listenersLock = Any()

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

    // ── Real-time Firestore listener ────────────────────────────────────────────
    //
    // Streams remote changes to the user's OWNED boards into Room while the app is
    // in the foreground, so edits made on another surface (e.g. the webapp) appear
    // live without a manual pull-to-refresh. Mirrors ReminderRepository's listener.
    //
    // Covers boards + their columns + tasks in real time. Subtasks are NOT listened
    // to individually (that would require a listener per task); subtask-only remote
    // edits are reconciled by SyncWorker on the next foreground sync / pull-to-refresh.
    // Shared boards (owned by other users) are likewise left to SyncWorker.
    //
    // Preserves the critical invariant (see CLAUDE.md): a remote document is skipped
    // when the local row is SyncStatus.PENDING, so we never clobber a local write
    // that SyncWorker has not yet pushed.

    fun startFirestoreListener(userId: String) {
        stopFirestoreListener()
        val boardsRef = firestore.collection("users").document(userId).collection("boards")

        boardsListener = boardsRef
            .whereEqualTo("isDeleted", false)
            .addSnapshotListener { snap, error ->
                if (error != null || snap == null) return@addSnapshotListener
                listenerScope.launch {
                    for (change in snap.documentChanges) {
                        val boardId = change.document.id
                        when (change.type) {
                            DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                val remote = change.document.toBoardEntity(userId)
                                    .copy(syncStatus = SyncStatus.SYNCED)
                                val existing = boardDao.getBoardById(remote.id)
                                if (existing == null || existing.syncStatus != SyncStatus.PENDING) {
                                    boardDao.upsert(remote)
                                }
                                registerChildListeners(userId, boardId)
                            }
                            DocumentChange.Type.REMOVED -> {
                                // Board dropped out of the isDeleted==false query (deleted remotely).
                                unregisterChildListeners(boardId)
                                val existing = boardDao.getBoardById(boardId)
                                if (existing != null && existing.syncStatus != SyncStatus.PENDING) {
                                    boardDao.upsert(existing.copy(isDeleted = true, syncStatus = SyncStatus.SYNCED))
                                }
                            }
                        }
                    }
                }
            }
    }

    fun stopFirestoreListener() {
        boardsListener?.remove(); boardsListener = null
        synchronized(listenersLock) {
            childListeners.values.forEach { regs -> regs.forEach { it.remove() } }
            childListeners.clear()
        }
    }

    private fun registerChildListeners(userId: String, boardId: String) {
        synchronized(listenersLock) {
            if (childListeners.containsKey(boardId)) return
            val boardRef = firestore.collection("users").document(userId)
                .collection("boards").document(boardId)

            val columnsReg = boardRef.collection("columns")
                .addSnapshotListener { snap, error ->
                    if (error != null || snap == null) return@addSnapshotListener
                    listenerScope.launch {
                        for (change in snap.documentChanges) {
                            val remote = change.document.toColumnEntity()
                                .copy(syncStatus = SyncStatus.SYNCED)
                            val existing = columnDao.getColumnsByBoard(boardId).find { it.id == remote.id }
                            if (existing == null || existing.syncStatus != SyncStatus.PENDING) {
                                columnDao.upsert(remote)
                            }
                        }
                    }
                }

            val tasksReg = boardRef.collection("tasks")
                .addSnapshotListener { snap, error ->
                    if (error != null || snap == null) return@addSnapshotListener
                    listenerScope.launch {
                        for (change in snap.documentChanges) {
                            val remote = change.document.toTaskEntity()
                                .copy(syncStatus = SyncStatus.SYNCED)
                            val existing = taskDao.getTaskById(remote.id)
                            if (existing == null || existing.syncStatus != SyncStatus.PENDING) {
                                taskDao.upsert(remote)
                            }
                        }
                    }
                }

            childListeners[boardId] = listOf(columnsReg, tasksReg)
        }
    }

    private fun unregisterChildListeners(boardId: String) {
        synchronized(listenersLock) {
            childListeners.remove(boardId)?.forEach { it.remove() }
        }
    }

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
