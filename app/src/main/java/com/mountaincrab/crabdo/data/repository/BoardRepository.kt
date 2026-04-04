package com.mountaincrab.crabdo.data.repository

import androidx.work.*
import com.google.gson.Gson
import com.mountaincrab.crabdo.data.local.dao.BoardDao
import com.mountaincrab.crabdo.data.local.dao.ColumnDao
import com.mountaincrab.crabdo.data.local.entity.BoardEntity
import com.mountaincrab.crabdo.data.local.entity.ColumnEntity
import com.mountaincrab.crabdo.data.model.SyncStatus
import com.mountaincrab.crabdo.data.remote.SyncWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BoardRepository @Inject constructor(
    private val boardDao: BoardDao,
    private val columnDao: ColumnDao,
    private val workManager: WorkManager
) {
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

    private fun parseColumnOrder(json: String): List<String> =
        try { Gson().fromJson(json, Array<String>::class.java).toList() }
        catch (e: Exception) { emptyList() }

    private fun serializeColumnOrder(ids: List<String>): String = Gson().toJson(ids)

    private fun enqueueSyncWork() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork("sync", ExistingWorkPolicy.REPLACE, request)
    }
}
