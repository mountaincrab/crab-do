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

val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_5_6)
