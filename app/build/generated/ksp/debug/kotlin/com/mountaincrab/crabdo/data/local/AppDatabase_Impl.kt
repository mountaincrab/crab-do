package com.mountaincrab.crabdo.`data`.local

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.mountaincrab.crabdo.`data`.local.dao.BoardDao
import com.mountaincrab.crabdo.`data`.local.dao.BoardDao_Impl
import com.mountaincrab.crabdo.`data`.local.dao.ColumnDao
import com.mountaincrab.crabdo.`data`.local.dao.ColumnDao_Impl
import com.mountaincrab.crabdo.`data`.local.dao.ReminderDao
import com.mountaincrab.crabdo.`data`.local.dao.ReminderDao_Impl
import com.mountaincrab.crabdo.`data`.local.dao.SubtaskDao
import com.mountaincrab.crabdo.`data`.local.dao.SubtaskDao_Impl
import com.mountaincrab.crabdo.`data`.local.dao.TaskDao
import com.mountaincrab.crabdo.`data`.local.dao.TaskDao_Impl
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class AppDatabase_Impl : AppDatabase() {
  private val _boardDao: Lazy<BoardDao> = lazy {
    BoardDao_Impl(this)
  }

  private val _columnDao: Lazy<ColumnDao> = lazy {
    ColumnDao_Impl(this)
  }

  private val _taskDao: Lazy<TaskDao> = lazy {
    TaskDao_Impl(this)
  }

  private val _subtaskDao: Lazy<SubtaskDao> = lazy {
    SubtaskDao_Impl(this)
  }

  private val _reminderDao: Lazy<ReminderDao> = lazy {
    ReminderDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(1,
        "a90590932805b9639100c44fa446751d", "385a6989b363b823d67a21c338b2e7f4") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `boards` (`id` TEXT NOT NULL, `userId` TEXT NOT NULL, `title` TEXT NOT NULL, `columnOrder` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `syncStatus` TEXT NOT NULL, `isDeleted` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `columns` (`id` TEXT NOT NULL, `boardId` TEXT NOT NULL, `title` TEXT NOT NULL, `order` REAL NOT NULL, `updatedAt` INTEGER NOT NULL, `syncStatus` TEXT NOT NULL, `isDeleted` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `tasks` (`id` TEXT NOT NULL, `boardId` TEXT NOT NULL, `columnId` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `order` REAL NOT NULL, `reminderTimeMillis` INTEGER, `reminderStyle` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `syncStatus` TEXT NOT NULL, `isDeleted` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `subtasks` (`id` TEXT NOT NULL, `taskId` TEXT NOT NULL, `title` TEXT NOT NULL, `isCompleted` INTEGER NOT NULL, `order` REAL NOT NULL, `updatedAt` INTEGER NOT NULL, `syncStatus` TEXT NOT NULL, `isDeleted` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `reminders` (`id` TEXT NOT NULL, `userId` TEXT NOT NULL, `title` TEXT NOT NULL, `nextTriggerMillis` INTEGER NOT NULL, `reminderStyle` TEXT NOT NULL, `recurrenceRuleJson` TEXT, `isEnabled` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `syncStatus` TEXT NOT NULL, `isDeleted` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'a90590932805b9639100c44fa446751d')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `boards`")
        connection.execSQL("DROP TABLE IF EXISTS `columns`")
        connection.execSQL("DROP TABLE IF EXISTS `tasks`")
        connection.execSQL("DROP TABLE IF EXISTS `subtasks`")
        connection.execSQL("DROP TABLE IF EXISTS `reminders`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection):
          RoomOpenDelegate.ValidationResult {
        val _columnsBoards: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsBoards.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBoards.put("userId", TableInfo.Column("userId", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBoards.put("title", TableInfo.Column("title", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBoards.put("columnOrder", TableInfo.Column("columnOrder", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBoards.put("createdAt", TableInfo.Column("createdAt", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBoards.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBoards.put("syncStatus", TableInfo.Column("syncStatus", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBoards.put("isDeleted", TableInfo.Column("isDeleted", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysBoards: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesBoards: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoBoards: TableInfo = TableInfo("boards", _columnsBoards, _foreignKeysBoards,
            _indicesBoards)
        val _existingBoards: TableInfo = read(connection, "boards")
        if (!_infoBoards.equals(_existingBoards)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |boards(com.mountaincrab.crabdo.data.local.entity.BoardEntity).
              | Expected:
              |""".trimMargin() + _infoBoards + """
              |
              | Found:
              |""".trimMargin() + _existingBoards)
        }
        val _columnsColumns: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsColumns.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsColumns.put("boardId", TableInfo.Column("boardId", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsColumns.put("title", TableInfo.Column("title", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsColumns.put("order", TableInfo.Column("order", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsColumns.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsColumns.put("syncStatus", TableInfo.Column("syncStatus", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsColumns.put("isDeleted", TableInfo.Column("isDeleted", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysColumns: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesColumns: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoColumns: TableInfo = TableInfo("columns", _columnsColumns, _foreignKeysColumns,
            _indicesColumns)
        val _existingColumns: TableInfo = read(connection, "columns")
        if (!_infoColumns.equals(_existingColumns)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |columns(com.mountaincrab.crabdo.data.local.entity.ColumnEntity).
              | Expected:
              |""".trimMargin() + _infoColumns + """
              |
              | Found:
              |""".trimMargin() + _existingColumns)
        }
        val _columnsTasks: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsTasks.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTasks.put("boardId", TableInfo.Column("boardId", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTasks.put("columnId", TableInfo.Column("columnId", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTasks.put("title", TableInfo.Column("title", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTasks.put("description", TableInfo.Column("description", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTasks.put("order", TableInfo.Column("order", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTasks.put("reminderTimeMillis", TableInfo.Column("reminderTimeMillis", "INTEGER",
            false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTasks.put("reminderStyle", TableInfo.Column("reminderStyle", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTasks.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTasks.put("syncStatus", TableInfo.Column("syncStatus", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTasks.put("isDeleted", TableInfo.Column("isDeleted", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysTasks: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesTasks: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoTasks: TableInfo = TableInfo("tasks", _columnsTasks, _foreignKeysTasks,
            _indicesTasks)
        val _existingTasks: TableInfo = read(connection, "tasks")
        if (!_infoTasks.equals(_existingTasks)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |tasks(com.mountaincrab.crabdo.data.local.entity.TaskEntity).
              | Expected:
              |""".trimMargin() + _infoTasks + """
              |
              | Found:
              |""".trimMargin() + _existingTasks)
        }
        val _columnsSubtasks: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsSubtasks.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSubtasks.put("taskId", TableInfo.Column("taskId", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSubtasks.put("title", TableInfo.Column("title", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSubtasks.put("isCompleted", TableInfo.Column("isCompleted", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSubtasks.put("order", TableInfo.Column("order", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSubtasks.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSubtasks.put("syncStatus", TableInfo.Column("syncStatus", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSubtasks.put("isDeleted", TableInfo.Column("isDeleted", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysSubtasks: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesSubtasks: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoSubtasks: TableInfo = TableInfo("subtasks", _columnsSubtasks, _foreignKeysSubtasks,
            _indicesSubtasks)
        val _existingSubtasks: TableInfo = read(connection, "subtasks")
        if (!_infoSubtasks.equals(_existingSubtasks)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |subtasks(com.mountaincrab.crabdo.data.local.entity.SubtaskEntity).
              | Expected:
              |""".trimMargin() + _infoSubtasks + """
              |
              | Found:
              |""".trimMargin() + _existingSubtasks)
        }
        val _columnsReminders: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsReminders.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsReminders.put("userId", TableInfo.Column("userId", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsReminders.put("title", TableInfo.Column("title", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsReminders.put("nextTriggerMillis", TableInfo.Column("nextTriggerMillis", "INTEGER",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsReminders.put("reminderStyle", TableInfo.Column("reminderStyle", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsReminders.put("recurrenceRuleJson", TableInfo.Column("recurrenceRuleJson", "TEXT",
            false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsReminders.put("isEnabled", TableInfo.Column("isEnabled", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsReminders.put("createdAt", TableInfo.Column("createdAt", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsReminders.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsReminders.put("syncStatus", TableInfo.Column("syncStatus", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsReminders.put("isDeleted", TableInfo.Column("isDeleted", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysReminders: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesReminders: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoReminders: TableInfo = TableInfo("reminders", _columnsReminders,
            _foreignKeysReminders, _indicesReminders)
        val _existingReminders: TableInfo = read(connection, "reminders")
        if (!_infoReminders.equals(_existingReminders)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |reminders(com.mountaincrab.crabdo.data.local.entity.ReminderEntity).
              | Expected:
              |""".trimMargin() + _infoReminders + """
              |
              | Found:
              |""".trimMargin() + _existingReminders)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "boards", "columns", "tasks",
        "subtasks", "reminders")
  }

  public override fun clearAllTables() {
    super.performClear(false, "boards", "columns", "tasks", "subtasks", "reminders")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(BoardDao::class, BoardDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(ColumnDao::class, ColumnDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(TaskDao::class, TaskDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(SubtaskDao::class, SubtaskDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(ReminderDao::class, ReminderDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override
      fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>):
      List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun boardDao(): BoardDao = _boardDao.value

  public override fun columnDao(): ColumnDao = _columnDao.value

  public override fun taskDao(): TaskDao = _taskDao.value

  public override fun subtaskDao(): SubtaskDao = _subtaskDao.value

  public override fun reminderDao(): ReminderDao = _reminderDao.value
}
