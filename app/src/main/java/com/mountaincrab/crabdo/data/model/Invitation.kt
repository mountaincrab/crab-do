package com.mountaincrab.crabdo.data.model

data class Invitation(
    val id: String,
    val ownerUid: String,
    val ownerDisplayName: String,
    val boardId: String,
    val boardTitle: String,
    val inviteeEmail: String,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long
)
