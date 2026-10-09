package com.mountaincrab.crabdo.data.local.entity

import androidx.room.Embedded

data class TaskReminderSummary(
    @Embedded val task: TaskEntity,
    val boardTitle: String,
    val columnTitle: String
)
