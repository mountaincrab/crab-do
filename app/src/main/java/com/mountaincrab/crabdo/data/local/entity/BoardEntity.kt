package com.mountaincrab.crabdo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mountaincrab.crabdo.data.model.SyncStatus
import java.util.UUID

@Entity(tableName = "boards")
data class BoardEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val title: String,
    val columnOrder: String = "[]",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val isDeleted: Boolean = false
)
