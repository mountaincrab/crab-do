package com.mountaincrab.crabdo.data.local

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mountaincrab.crabdo.data.local.entity.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskReminderDaoTest {
    private lateinit var db: AppDatabase

    @Before
    fun createDatabase() {
        db = Room.inMemoryDatabaseBuilder<AppDatabase>(
            InstrumentationRegistry.getInstrumentation().targetContext
        ).setDriver(BundledSQLiteDriver()).build()
    }

    @After
    fun closeDatabase() = db.close()

    private suspend fun seedBoard(id: String, owner: String = "u1") {
        db.boardDao().upsert(BoardEntity(id = id, userId = owner, title = "Board $id"))
        db.columnDao().upsert(ColumnEntity(id = "c-$id", boardId = id, title = "Column $id"))
    }

    private fun task(id: String, board: String = "b1", time: Long? = 100) =
        TaskEntity(id = id, boardId = board, columnId = "c-$board", title = id, reminderTimeMillis = time)

    @Test
    fun summaryIncludesOwnedAndSharedBoardsInEffectiveTimeOrder() = runBlocking {
        seedBoard("b1")
        seedBoard("shared", "owner")
        seedBoard("other", "someone-else")
        db.boardAccessDao().upsert(BoardAccessEntity("shared", "u1", "owner"))
        db.taskDao().upsert(task("scheduled", time = 300))
        db.taskDao().upsert(task("snoozed", "shared", null).copy(snoozedUntilMillis = 200))
        db.taskDao().upsert(task("hidden", "other"))
        db.taskDao().upsert(task("no-reminder", time = null))
        val rows = db.taskDao().observeReminderSummary("u1").first()
        assertEquals(listOf("snoozed", "scheduled"), rows.map { it.task.id })
        assertEquals("Board shared", rows.first().boardTitle)
        assertEquals("Column shared", rows.first().columnTitle)
    }

    @Test
    fun firingAndRepeatedSnoozingConsumeOnlyTheMatchingAlarm() = runBlocking {
        seedBoard("b1")
        val dao = db.taskDao()
        dao.upsert(task("t1"))
        assertEquals(0, dao.consumeReminder("t1", 99))
        assertEquals(1, dao.consumeReminder("t1", 100))
        assertEquals(1, dao.snoozeReminder("t1", 200))
        assertEquals(200L, dao.observeReminderSummary("u1").first().single().task.nextReminderTimeMillis())
        assertEquals(0, dao.consumeReminder("t1", 100))
        assertEquals(1, dao.consumeReminder("t1", 200))
        assertEquals(1, dao.snoozeReminder("t1", 300))
        assertEquals(1, dao.consumeReminder("t1", 300))
        assertTrue(dao.observeReminderSummary("u1").first().isEmpty())
    }

    @Test
    fun deletingASnoozedTaskClearsTimesAndPreventsResurrection() = runBlocking {
        seedBoard("b1")
        val dao = db.taskDao()
        dao.upsert(task("t1", time = null).copy(snoozedUntilMillis = 200))
        dao.softDelete("t1")
        val deleted = dao.getTaskById("t1")!!
        assertTrue(deleted.isDeleted)
        assertNull(deleted.reminderTimeMillis)
        assertNull(deleted.snoozedUntilMillis)
        assertEquals(0, dao.snoozeReminder("t1", 300))
        assertEquals(0, dao.consumeReminder("t1", 200))
        assertTrue(dao.getTasksWithReminders().isEmpty())
        assertTrue(dao.observeReminderSummary("u1").first().isEmpty())
    }

    @Test
    fun deletedParentsAndNewSchedulesRejectStaleSnoozes() = runBlocking {
        seedBoard("b1")
        val dao = db.taskDao()
        dao.upsert(task("new-schedule", time = 400))
        assertEquals(0, dao.snoozeReminder("new-schedule", 300))
        db.columnDao().softDelete("c-b1")
        assertTrue(dao.getTasksWithReminders().isEmpty())
        assertTrue(dao.observeReminderSummary("u1").first().isEmpty())
        assertEquals(0, dao.consumeReminder("new-schedule", 400))
        seedBoard("b2")
        dao.upsert(task("deleted-board", "b2"))
        db.boardDao().upsert(db.boardDao().getBoardById("b2")!!.copy(isDeleted = true))
        assertEquals(0, dao.consumeReminder("deleted-board", 100))
        assertTrue(dao.observeReminderSummary("u1").first().isEmpty())
    }
}
