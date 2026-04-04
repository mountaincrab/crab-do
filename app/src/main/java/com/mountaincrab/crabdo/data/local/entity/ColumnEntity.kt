package com.mountaincrab.crabdo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mountaincrab.crabdo.data.model.SyncStatus
import java.util.UUID

@Entity(tableName = "columns")
data class ColumnEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val boardId: String,
    val title: String,
    val order: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val isDeleted: Boolean = false
)
