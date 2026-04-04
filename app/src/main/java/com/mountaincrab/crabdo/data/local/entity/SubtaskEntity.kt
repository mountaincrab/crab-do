package com.mountaincrab.crabdo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mountaincrab.crabdo.data.model.SyncStatus
import java.util.UUID

@Entity(tableName = "subtasks")
data class SubtaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val taskId: String,
    val title: String,
    val isCompleted: Boolean = false,
    val order: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val isDeleted: Boolean = false
)
