package com.mountaincrab.crabdo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mountaincrab.crabdo.data.model.SyncStatus
import com.mountaincrab.crabdo.util.currentTimeMillis
import com.mountaincrab.crabdo.util.randomUUID

@Entity(tableName = "boards")
data class BoardEntity(
    @PrimaryKey val id: String = randomUUID(),
    val userId: String,
    val title: String,
    val columnOrder: String = "[]",
    val createdAt: Long = currentTimeMillis(),
    val updatedAt: Long = currentTimeMillis(),
    val isShared: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val isDeleted: Boolean = false
)
