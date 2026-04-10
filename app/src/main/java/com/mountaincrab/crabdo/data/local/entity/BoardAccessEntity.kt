package com.mountaincrab.crabdo.data.local.entity

import androidx.room.Entity

@Entity(tableName = "board_access", primaryKeys = ["boardId", "userId"])
data class BoardAccessEntity(
    val boardId: String,
    val userId: String,
    val ownerUserId: String,
    val role: String = "editor"
)
