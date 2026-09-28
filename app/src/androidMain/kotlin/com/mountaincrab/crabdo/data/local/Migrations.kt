package com.mountaincrab.crabdo.data.local

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

// Add a new Migration object here every time @Database version is bumped.
// Schemas are emitted to app/schemas/<dbClass>/<version>.json on each build —
// diff the JSONs to derive the SQL for a new migration.

// v5 → v6: one-off reminders no longer have an enable/disable toggle; drop the column.
private val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE one_off_reminders DROP COLUMN isEnabled")
    }
}

// v6 → v7: boards gain a per-board default column (the column shown first when
// the board is opened). Nullable — null means fall back to the first column.
private val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE boards ADD COLUMN defaultColumnId TEXT")
    }
}

// v7 → v8: a task reminder can optionally move the task into another column
// when it fires. Nullable — null means the reminder doesn't move the task.
private val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE tasks ADD COLUMN reminderTransitionColumnId TEXT")
    }
}

val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
