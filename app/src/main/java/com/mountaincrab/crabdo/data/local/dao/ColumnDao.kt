package com.mountaincrab.crabdo.data.local.dao

import androidx.room.*
import com.mountaincrab.crabdo.data.local.entity.ColumnEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ColumnDao {
    @Query("SELECT * FROM columns WHERE boardId = :boardId AND isDeleted = 0 ORDER BY `order`")
    fun observeColumnsByBoard(boardId: String): Flow<List<ColumnEntity>>

    @Query("SELECT * FROM columns WHERE boardId = :boardId AND isDeleted = 0 ORDER BY `order`")
    suspend fun getColumnsByBoard(boardId: String): List<ColumnEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(column: ColumnEntity)

    @Query("SELECT * FROM columns WHERE syncStatus != 'SYNCED'")
    suspend fun getUnsyncedColumns(): List<ColumnEntity>

    @Query("UPDATE columns SET syncStatus = 'SYNCED' WHERE id = :columnId")
    suspend fun markSynced(columnId: String)

    @Query("UPDATE columns SET isDeleted = 1, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :columnId")
    suspend fun softDelete(columnId: String, updatedAt: Long = System.currentTimeMillis())
}
