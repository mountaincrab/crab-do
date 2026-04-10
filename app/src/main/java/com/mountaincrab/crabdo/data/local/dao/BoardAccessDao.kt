package com.mountaincrab.crabdo.data.local.dao

import androidx.room.*
import com.mountaincrab.crabdo.data.local.entity.BoardAccessEntity

@Dao
interface BoardAccessDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(access: BoardAccessEntity)

    @Query("SELECT * FROM board_access WHERE userId = :userId AND ownerUserId != :userId")
    suspend fun getSharedBoardAccess(userId: String): List<BoardAccessEntity>

    @Query("DELETE FROM board_access WHERE boardId = :boardId AND userId = :userId")
    suspend fun remove(boardId: String, userId: String)

    @Query("DELETE FROM board_access WHERE boardId = :boardId")
    suspend fun removeAllForBoard(boardId: String)
}
