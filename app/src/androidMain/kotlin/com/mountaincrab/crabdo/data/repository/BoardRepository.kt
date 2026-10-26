package com.mountaincrab.crabdo.data.repository

import androidx.work.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    private val repositoryScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var boardListener: ListenerRegistration? = null
    private var columnListener: ListenerRegistration? = null
    private var editingTaskListener: ListenerRegistration? = null
    private var editingTaskGeneration = 0
    private val taskListeners = mutableMapOf<String, Pair<Int, ListenerRegistration>>()
    private val _taskPageStates = MutableStateFlow<Map<String, PageLoadState>>(emptyMap())
    val taskPageStates = _taskPageStates.asStateFlow()
    private var activeBoardId: String? = null
    private var listenerGeneration = 0
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

        boardRef.collection("columns").forEachDocumentPage { doc ->
            columnDao.upsert(doc.toColumnEntity().copy(syncStatus = SyncStatus.SYNCED))
        }

        boardRef.collection("tasks").forEachDocumentPage { taskDoc ->
            applyTaskDocument(taskDoc)
            boardRef.collection("tasks").document(taskDoc.id)
                .collection("subtasks").forEachDocumentPage { subDoc ->
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
        activeBoardId = boardId
        val generation = listenerGeneration
        repositoryScope.launch {
            // Shared boards live under the owner's subtree, not the local user's.
            val ownerUid = boardDao.getBoardById(boardId)?.userId ?: return@launch
            if (generation != listenerGeneration) return@launch
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

        }
    }

    // Only the requested prefix of each column has a live task listener. Room
    // keeps the offline replica; background sync pulls changes in bounded batches.
    suspend fun setTaskWindows(boardId: String, limits: Map<String, Int>, retryColumn: String? = null) {
        val generation = listenerGeneration
        val owner = boardDao.getBoardById(boardId)?.userId ?: return
        if (activeBoardId != boardId || generation != listenerGeneration) return
        val tasksRef = firestore.collection("users").document(owner)
            .collection("boards").document(boardId).collection("tasks")
        (taskListeners.keys - limits.keys).forEach { taskListeners.remove(it)?.second?.remove() }
        limits.forEach { (columnId, limit) ->
            if (taskListeners[columnId]?.first == limit && retryColumn != columnId) return@forEach
            taskListeners.remove(columnId)?.second?.remove()
            var active = true
            _taskPageStates.update { it + (columnId to PageLoadState(loading = true)) }
            val registration = tasksRef.whereEqualTo("columnId", columnId)
                .whereEqualTo("isDeleted", false)
                .orderBy("order").orderBy(FieldPath.documentId())
                .limit((limit + 1).toLong())
                .addSnapshotListener { snapshot, error ->
                    if (!active || generation != listenerGeneration) return@addSnapshotListener
                    if (error != null) {
                        _taskPageStates.update { it + (columnId to PageLoadState(failed = true)) }
                        return@addSnapshotListener
                    }
                    if (snapshot == null) return@addSnapshotListener
                    repositoryScope.launch {
                        for (change in snapshot.documentChanges) {
                            if (!active || generation != listenerGeneration) break
                            // REMOVED can mean moved out of this limited window, not deleted.
                            val document = if (change.type == DocumentChange.Type.REMOVED) {
                                try { change.document.reference.get().await() }
                                catch (_: Exception) { continue }
                            } else change.document
                            if (active && generation == listenerGeneration) applyTaskDocument(document)
                        }
                        if (active && generation == listenerGeneration) _taskPageStates.update { it + (columnId to PageLoadState()) }
                    }
                }
            taskListeners[columnId] = limit to ListenerRegistration { active = false; registration.remove() }
        }
    }

    private suspend fun applyTaskDocument(document: DocumentSnapshot) {
        val existing = taskDao.getTaskById(document.id)
        if (existing?.syncStatus == SyncStatus.PENDING) return
        if (document.exists()) {
            val remote = document.toTaskEntity().copy(syncStatus = SyncStatus.SYNCED)
            if (existing == null || remote.updatedAt >= existing.updatedAt) taskDao.upsert(remote)
        } else if (existing != null) taskDao.upsert(existing.copy(isDeleted = true, syncStatus = SyncStatus.SYNCED))
    }

    suspend fun setEditingTask(boardId: String, taskId: String?) {
        val editingGeneration = ++editingTaskGeneration
        editingTaskListener?.remove()
        editingTaskListener = null
        if (taskId == null) return
        val generation = listenerGeneration
        val owner = boardDao.getBoardById(boardId)?.userId ?: return
        if (activeBoardId != boardId || generation != listenerGeneration || editingGeneration != editingTaskGeneration) return
        var active = true
        val registration = firestore.collection("users").document(owner).collection("boards").document(boardId)
            .collection("tasks").document(taskId).addSnapshotListener { doc, error ->
                if (!active || generation != listenerGeneration || error != null || doc == null) return@addSnapshotListener
                repositoryScope.launch {
                    if (active && generation == listenerGeneration) applyTaskDocument(doc)
                }
            }
        editingTaskListener = ListenerRegistration { active = false; registration.remove() }
    }

    suspend fun setVisibleTasks(boardId: String, ids: List<String>) {
        val generation = listenerGeneration
        val owner = boardDao.getBoardById(boardId)?.userId ?: return
        if (activeBoardId != boardId || generation != listenerGeneration) return
        val boardRef = firestore.collection("users").document(owner).collection("boards").document(boardId)
        (subtaskListeners.keys - ids.toSet()).forEach { subtaskListeners.remove(it)?.remove() }
        ids.forEach { id ->
            if (id !in subtaskListeners) subtaskListeners[id] = listenToSubtasks(boardRef, id)
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
        listenerGeneration++
        activeBoardId = null
        boardListener?.remove(); boardListener = null
        columnListener?.remove(); columnListener = null
        editingTaskListener?.remove(); editingTaskListener = null
        taskListeners.values.forEach { it.second.remove() }
        taskListeners.clear()
        _taskPageStates.value = emptyMap()
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
