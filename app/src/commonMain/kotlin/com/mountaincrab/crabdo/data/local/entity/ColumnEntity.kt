package com.mountaincrab.crabdo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mountaincrab.crabdo.data.model.SyncStatus
import com.mountaincrab.crabdo.util.currentTimeMillis
import com.mountaincrab.crabdo.util.randomUUID

@Entity(tableName = "columns")
data class ColumnEntity(
    @PrimaryKey val id: String = randomUUID(),
    val boardId: String,
    val title: String,
    val order: Double = 0.0,
    val updatedAt: Long = currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val isDeleted: Boolean = false
)
