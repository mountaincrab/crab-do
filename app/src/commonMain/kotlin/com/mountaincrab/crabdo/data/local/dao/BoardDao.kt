package com.mountaincrab.crabdo.data.local.dao

import androidx.room.*
import com.mountaincrab.crabdo.data.local.entity.BoardEntity
import com.mountaincrab.crabdo.util.currentTimeMillis
import kotlinx.coroutines.flow.Flow

@Dao
interface BoardDao {
    @Query("""
        SELECT * FROM boards
        WHERE (userId = :userId OR id IN (SELECT boardId FROM board_access WHERE userId = :userId))
          AND isDeleted = 0
        ORDER BY createdAt
    """)
    fun observeBoards(userId: String): Flow<List<BoardEntity>>

    @Query("SELECT * FROM boards WHERE id = :boardId AND isDeleted = 0")
    fun observeBoard(boardId: String): Flow<BoardEntity?>

    @Query("SELECT * FROM boards WHERE id = :boardId")
    suspend fun getBoardById(boardId: String): BoardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(board: BoardEntity)

    @Query("SELECT * FROM boards WHERE syncStatus != 'SYNCED' AND isDeleted = 0")
    suspend fun getUnsyncedBoards(): List<BoardEntity>

    @Query("SELECT * FROM boards WHERE isDeleted = 1 AND syncStatus != 'SYNCED'")
    suspend fun getDeletedUnsyncedBoards(): List<BoardEntity>

    @Query("UPDATE boards SET syncStatus = 'SYNCED' WHERE id = :boardId")
    suspend fun markSynced(boardId: String)

    @Query("SELECT id FROM boards WHERE userId = :userId AND isDeleted = 0")
    suspend fun getBoardIdsForUser(userId: String): List<String>

    @Query("UPDATE boards SET columnOrder = :columnOrder, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :boardId")
    suspend fun updateColumnOrder(boardId: String, columnOrder: String, updatedAt: Long = currentTimeMillis())
}
