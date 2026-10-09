package com.mountaincrab.crabdo.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Schema-migration tests. [MigrationTestHelper.runMigrationsAndValidate] re-creates the
 * database at the start version from the exported schema JSON, runs the migration(s), and
 * asserts the resulting schema exactly matches the target version's JSON — so a missing or
 * wrong migration fails here instead of crashing on a user's device.
 *
 * Add a new test method for every @Database version bump (see CLAUDE.md).
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migrate6To7_addsDefaultColumnIdAndPreservesBoards() {
        val dbName = "migration-test-6-7"

        // v6: no defaultColumnId column yet. Seed a board.
        helper.createDatabase(dbName, 6).apply {
            execSQL(
                "INSERT INTO boards " +
                    "(id, userId, title, columnOrder, createdAt, updatedAt, isShared, syncStatus, isDeleted) " +
                    "VALUES ('b1', 'u1', 'My Board', '[\"c1\"]', 100, 200, 0, 'SYNCED', 0)"
            )
            close()
        }

        // Run 6 -> 7 and validate the migrated schema against 7.json.
        val db = helper.runMigrationsAndValidate(dbName, 7, true, *ALL_MIGRATIONS)

        // Pre-existing row survives; the new nullable column defaults to NULL.
        db.query("SELECT title, defaultColumnId FROM boards WHERE id = 'b1'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("My Board", c.getString(0))
            assertTrue("defaultColumnId should default to NULL", c.isNull(1))
        }

        // The new column is writable and reads back.
        db.execSQL("UPDATE boards SET defaultColumnId = 'c1' WHERE id = 'b1'")
        db.query("SELECT defaultColumnId FROM boards WHERE id = 'b1'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("c1", c.getString(0))
        }
        db.close()
    }

    @Test
    fun migrate7To8_addsReminderTransitionColumnIdAndPreservesTasks() {
        val dbName = "migration-test-7-8"

        // v7: no reminderTransitionColumnId column yet. Seed a task with a reminder.
        helper.createDatabase(dbName, 7).apply {
            execSQL(
                "INSERT INTO tasks " +
                    "(id, boardId, columnId, title, description, `order`, reminderTimeMillis, " +
                    "reminderStyle, updatedAt, syncStatus, isDeleted) " +
                    "VALUES ('t1', 'b1', 'c1', 'My Task', '', 1.0, 5000, 'ALARM', 200, 'SYNCED', 0)"
            )
            close()
        }

        // Run 7 -> 8 and validate the migrated schema against 8.json.
        val db = helper.runMigrationsAndValidate(dbName, 8, true, *ALL_MIGRATIONS)

        // Pre-existing row survives; the new nullable column defaults to NULL.
        db.query("SELECT title, reminderTimeMillis, reminderTransitionColumnId FROM tasks WHERE id = 't1'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("My Task", c.getString(0))
            assertEquals(5000L, c.getLong(1))
            assertTrue("reminderTransitionColumnId should default to NULL", c.isNull(2))
        }

        // The new column is writable and reads back.
        db.execSQL("UPDATE tasks SET reminderTransitionColumnId = 'c2' WHERE id = 't1'")
        db.query("SELECT reminderTransitionColumnId FROM tasks WHERE id = 't1'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("c2", c.getString(0))
        }
        db.close()
    }
    @Test
    fun migrate8To9_preservesTaskRemindersAndAddsSnooze() {
        val name = "migration-test-8-9"
        helper.createDatabase(name, 8).apply {
            execSQL("INSERT INTO tasks (id, boardId, columnId, title, description, `order`, " +
                "reminderTimeMillis, reminderStyle, updatedAt, syncStatus, isDeleted) " +
                "VALUES ('t1', 'b1', 'c1', 'Pending task', '', 1, 123456, 'ALARM', 200, 'PENDING', 0)")
            close()
        }
        val db = helper.runMigrationsAndValidate(name, 9, true, *ALL_MIGRATIONS)
        db.query("SELECT title, reminderTimeMillis, snoozedUntilMillis, syncStatus FROM tasks WHERE id = 't1'").use {
            assertTrue(it.moveToFirst())
            assertEquals("Pending task", it.getString(0))
            assertEquals(123456L, it.getLong(1))
            assertTrue(it.isNull(2))
            assertEquals("PENDING", it.getString(3))
        }
        db.execSQL("UPDATE tasks SET snoozedUntilMillis = 234567 WHERE id = 't1'")
        db.query("SELECT snoozedUntilMillis FROM tasks WHERE id = 't1'").use {
            assertTrue(it.moveToFirst())
            assertEquals(234567L, it.getLong(0))
        }
        db.close()
    }

}
