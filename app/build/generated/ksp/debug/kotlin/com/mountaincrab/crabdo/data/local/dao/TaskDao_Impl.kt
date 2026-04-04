package com.mountaincrab.crabdo.`data`.local.dao

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.mountaincrab.crabdo.`data`.local.Converters
import com.mountaincrab.crabdo.`data`.local.entity.TaskEntity
import com.mountaincrab.crabdo.`data`.model.SyncStatus
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Double
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class TaskDao_Impl(
  __db: RoomDatabase,
) : TaskDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfTaskEntity: EntityInsertAdapter<TaskEntity>

  private val __converters: Converters = Converters()
  init {
    this.__db = __db
    this.__insertAdapterOfTaskEntity = object : EntityInsertAdapter<TaskEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `tasks` (`id`,`boardId`,`columnId`,`title`,`description`,`order`,`reminderTimeMillis`,`reminderStyle`,`updatedAt`,`syncStatus`,`isDeleted`) VALUES (?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: TaskEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.boardId)
        statement.bindText(3, entity.columnId)
        statement.bindText(4, entity.title)
        statement.bindText(5, entity.description)
        statement.bindDouble(6, entity.order)
        val _tmpReminderTimeMillis: Long? = entity.reminderTimeMillis
        if (_tmpReminderTimeMillis == null) {
          statement.bindNull(7)
        } else {
          statement.bindLong(7, _tmpReminderTimeMillis)
        }
        val _tmp: String = __converters.fromTaskReminderStyle(entity.reminderStyle)
        statement.bindText(8, _tmp)
        statement.bindLong(9, entity.updatedAt)
        val _tmp_1: String = __converters.fromSyncStatus(entity.syncStatus)
        statement.bindText(10, _tmp_1)
        val _tmp_2: Int = if (entity.isDeleted) 1 else 0
        statement.bindLong(11, _tmp_2.toLong())
      }
    }
  }

  public override suspend fun upsert(task: TaskEntity): Unit = performSuspending(__db, false, true)
      { _connection ->
    __insertAdapterOfTaskEntity.insert(_connection, task)
  }

  public override fun observeTasksByColumn(columnId: String): Flow<List<TaskEntity>> {
    val _sql: String = "SELECT * FROM tasks WHERE columnId = ? AND isDeleted = 0 ORDER BY `order`"
    return createFlow(__db, false, arrayOf("tasks")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, columnId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfBoardId: Int = getColumnIndexOrThrow(_stmt, "boardId")
        val _columnIndexOfColumnId: Int = getColumnIndexOrThrow(_stmt, "columnId")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfOrder: Int = getColumnIndexOrThrow(_stmt, "order")
        val _columnIndexOfReminderTimeMillis: Int = getColumnIndexOrThrow(_stmt,
            "reminderTimeMillis")
        val _columnIndexOfReminderStyle: Int = getColumnIndexOrThrow(_stmt, "reminderStyle")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfSyncStatus: Int = getColumnIndexOrThrow(_stmt, "syncStatus")
        val _columnIndexOfIsDeleted: Int = getColumnIndexOrThrow(_stmt, "isDeleted")
        val _result: MutableList<TaskEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: TaskEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpBoardId: String
          _tmpBoardId = _stmt.getText(_columnIndexOfBoardId)
          val _tmpColumnId: String
          _tmpColumnId = _stmt.getText(_columnIndexOfColumnId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpOrder: Double
          _tmpOrder = _stmt.getDouble(_columnIndexOfOrder)
          val _tmpReminderTimeMillis: Long?
          if (_stmt.isNull(_columnIndexOfReminderTimeMillis)) {
            _tmpReminderTimeMillis = null
          } else {
            _tmpReminderTimeMillis = _stmt.getLong(_columnIndexOfReminderTimeMillis)
          }
          val _tmpReminderStyle: TaskEntity.ReminderStyle
          val _tmp: String
          _tmp = _stmt.getText(_columnIndexOfReminderStyle)
          _tmpReminderStyle = __converters.toTaskReminderStyle(_tmp)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpSyncStatus: SyncStatus
          val _tmp_1: String
          _tmp_1 = _stmt.getText(_columnIndexOfSyncStatus)
          _tmpSyncStatus = __converters.toSyncStatus(_tmp_1)
          val _tmpIsDeleted: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfIsDeleted).toInt()
          _tmpIsDeleted = _tmp_2 != 0
          _item =
              TaskEntity(_tmpId,_tmpBoardId,_tmpColumnId,_tmpTitle,_tmpDescription,_tmpOrder,_tmpReminderTimeMillis,_tmpReminderStyle,_tmpUpdatedAt,_tmpSyncStatus,_tmpIsDeleted)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeTasksByBoard(boardId: String): Flow<List<TaskEntity>> {
    val _sql: String = "SELECT * FROM tasks WHERE boardId = ? AND isDeleted = 0"
    return createFlow(__db, false, arrayOf("tasks")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, boardId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfBoardId: Int = getColumnIndexOrThrow(_stmt, "boardId")
        val _columnIndexOfColumnId: Int = getColumnIndexOrThrow(_stmt, "columnId")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfOrder: Int = getColumnIndexOrThrow(_stmt, "order")
        val _columnIndexOfReminderTimeMillis: Int = getColumnIndexOrThrow(_stmt,
            "reminderTimeMillis")
        val _columnIndexOfReminderStyle: Int = getColumnIndexOrThrow(_stmt, "reminderStyle")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfSyncStatus: Int = getColumnIndexOrThrow(_stmt, "syncStatus")
        val _columnIndexOfIsDeleted: Int = getColumnIndexOrThrow(_stmt, "isDeleted")
        val _result: MutableList<TaskEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: TaskEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpBoardId: String
          _tmpBoardId = _stmt.getText(_columnIndexOfBoardId)
          val _tmpColumnId: String
          _tmpColumnId = _stmt.getText(_columnIndexOfColumnId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpOrder: Double
          _tmpOrder = _stmt.getDouble(_columnIndexOfOrder)
          val _tmpReminderTimeMillis: Long?
          if (_stmt.isNull(_columnIndexOfReminderTimeMillis)) {
            _tmpReminderTimeMillis = null
          } else {
            _tmpReminderTimeMillis = _stmt.getLong(_columnIndexOfReminderTimeMillis)
          }
          val _tmpReminderStyle: TaskEntity.ReminderStyle
          val _tmp: String
          _tmp = _stmt.getText(_columnIndexOfReminderStyle)
          _tmpReminderStyle = __converters.toTaskReminderStyle(_tmp)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpSyncStatus: SyncStatus
          val _tmp_1: String
          _tmp_1 = _stmt.getText(_columnIndexOfSyncStatus)
          _tmpSyncStatus = __converters.toSyncStatus(_tmp_1)
          val _tmpIsDeleted: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfIsDeleted).toInt()
          _tmpIsDeleted = _tmp_2 != 0
          _item =
              TaskEntity(_tmpId,_tmpBoardId,_tmpColumnId,_tmpTitle,_tmpDescription,_tmpOrder,_tmpReminderTimeMillis,_tmpReminderStyle,_tmpUpdatedAt,_tmpSyncStatus,_tmpIsDeleted)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeTask(taskId: String): Flow<TaskEntity?> {
    val _sql: String = "SELECT * FROM tasks WHERE id = ?"
    return createFlow(__db, false, arrayOf("tasks")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, taskId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfBoardId: Int = getColumnIndexOrThrow(_stmt, "boardId")
        val _columnIndexOfColumnId: Int = getColumnIndexOrThrow(_stmt, "columnId")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfOrder: Int = getColumnIndexOrThrow(_stmt, "order")
        val _columnIndexOfReminderTimeMillis: Int = getColumnIndexOrThrow(_stmt,
            "reminderTimeMillis")
        val _columnIndexOfReminderStyle: Int = getColumnIndexOrThrow(_stmt, "reminderStyle")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfSyncStatus: Int = getColumnIndexOrThrow(_stmt, "syncStatus")
        val _columnIndexOfIsDeleted: Int = getColumnIndexOrThrow(_stmt, "isDeleted")
        val _result: TaskEntity?
        if (_stmt.step()) {
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpBoardId: String
          _tmpBoardId = _stmt.getText(_columnIndexOfBoardId)
          val _tmpColumnId: String
          _tmpColumnId = _stmt.getText(_columnIndexOfColumnId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpOrder: Double
          _tmpOrder = _stmt.getDouble(_columnIndexOfOrder)
          val _tmpReminderTimeMillis: Long?
          if (_stmt.isNull(_columnIndexOfReminderTimeMillis)) {
            _tmpReminderTimeMillis = null
          } else {
            _tmpReminderTimeMillis = _stmt.getLong(_columnIndexOfReminderTimeMillis)
          }
          val _tmpReminderStyle: TaskEntity.ReminderStyle
          val _tmp: String
          _tmp = _stmt.getText(_columnIndexOfReminderStyle)
          _tmpReminderStyle = __converters.toTaskReminderStyle(_tmp)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpSyncStatus: SyncStatus
          val _tmp_1: String
          _tmp_1 = _stmt.getText(_columnIndexOfSyncStatus)
          _tmpSyncStatus = __converters.toSyncStatus(_tmp_1)
          val _tmpIsDeleted: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfIsDeleted).toInt()
          _tmpIsDeleted = _tmp_2 != 0
          _result =
              TaskEntity(_tmpId,_tmpBoardId,_tmpColumnId,_tmpTitle,_tmpDescription,_tmpOrder,_tmpReminderTimeMillis,_tmpReminderStyle,_tmpUpdatedAt,_tmpSyncStatus,_tmpIsDeleted)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getTaskById(taskId: String): TaskEntity? {
    val _sql: String = "SELECT * FROM tasks WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, taskId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfBoardId: Int = getColumnIndexOrThrow(_stmt, "boardId")
        val _columnIndexOfColumnId: Int = getColumnIndexOrThrow(_stmt, "columnId")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfOrder: Int = getColumnIndexOrThrow(_stmt, "order")
        val _columnIndexOfReminderTimeMillis: Int = getColumnIndexOrThrow(_stmt,
            "reminderTimeMillis")
        val _columnIndexOfReminderStyle: Int = getColumnIndexOrThrow(_stmt, "reminderStyle")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfSyncStatus: Int = getColumnIndexOrThrow(_stmt, "syncStatus")
        val _columnIndexOfIsDeleted: Int = getColumnIndexOrThrow(_stmt, "isDeleted")
        val _result: TaskEntity?
        if (_stmt.step()) {
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpBoardId: String
          _tmpBoardId = _stmt.getText(_columnIndexOfBoardId)
          val _tmpColumnId: String
          _tmpColumnId = _stmt.getText(_columnIndexOfColumnId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpOrder: Double
          _tmpOrder = _stmt.getDouble(_columnIndexOfOrder)
          val _tmpReminderTimeMillis: Long?
          if (_stmt.isNull(_columnIndexOfReminderTimeMillis)) {
            _tmpReminderTimeMillis = null
          } else {
            _tmpReminderTimeMillis = _stmt.getLong(_columnIndexOfReminderTimeMillis)
          }
          val _tmpReminderStyle: TaskEntity.ReminderStyle
          val _tmp: String
          _tmp = _stmt.getText(_columnIndexOfReminderStyle)
          _tmpReminderStyle = __converters.toTaskReminderStyle(_tmp)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpSyncStatus: SyncStatus
          val _tmp_1: String
          _tmp_1 = _stmt.getText(_columnIndexOfSyncStatus)
          _tmpSyncStatus = __converters.toSyncStatus(_tmp_1)
          val _tmpIsDeleted: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfIsDeleted).toInt()
          _tmpIsDeleted = _tmp_2 != 0
          _result =
              TaskEntity(_tmpId,_tmpBoardId,_tmpColumnId,_tmpTitle,_tmpDescription,_tmpOrder,_tmpReminderTimeMillis,_tmpReminderStyle,_tmpUpdatedAt,_tmpSyncStatus,_tmpIsDeleted)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getUnsyncedTasks(): List<TaskEntity> {
    val _sql: String = "SELECT * FROM tasks WHERE syncStatus != 'SYNCED' AND isDeleted = 0"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfBoardId: Int = getColumnIndexOrThrow(_stmt, "boardId")
        val _columnIndexOfColumnId: Int = getColumnIndexOrThrow(_stmt, "columnId")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfOrder: Int = getColumnIndexOrThrow(_stmt, "order")
        val _columnIndexOfReminderTimeMillis: Int = getColumnIndexOrThrow(_stmt,
            "reminderTimeMillis")
        val _columnIndexOfReminderStyle: Int = getColumnIndexOrThrow(_stmt, "reminderStyle")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfSyncStatus: Int = getColumnIndexOrThrow(_stmt, "syncStatus")
        val _columnIndexOfIsDeleted: Int = getColumnIndexOrThrow(_stmt, "isDeleted")
        val _result: MutableList<TaskEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: TaskEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpBoardId: String
          _tmpBoardId = _stmt.getText(_columnIndexOfBoardId)
          val _tmpColumnId: String
          _tmpColumnId = _stmt.getText(_columnIndexOfColumnId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpOrder: Double
          _tmpOrder = _stmt.getDouble(_columnIndexOfOrder)
          val _tmpReminderTimeMillis: Long?
          if (_stmt.isNull(_columnIndexOfReminderTimeMillis)) {
            _tmpReminderTimeMillis = null
          } else {
            _tmpReminderTimeMillis = _stmt.getLong(_columnIndexOfReminderTimeMillis)
          }
          val _tmpReminderStyle: TaskEntity.ReminderStyle
          val _tmp: String
          _tmp = _stmt.getText(_columnIndexOfReminderStyle)
          _tmpReminderStyle = __converters.toTaskReminderStyle(_tmp)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpSyncStatus: SyncStatus
          val _tmp_1: String
          _tmp_1 = _stmt.getText(_columnIndexOfSyncStatus)
          _tmpSyncStatus = __converters.toSyncStatus(_tmp_1)
          val _tmpIsDeleted: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfIsDeleted).toInt()
          _tmpIsDeleted = _tmp_2 != 0
          _item =
              TaskEntity(_tmpId,_tmpBoardId,_tmpColumnId,_tmpTitle,_tmpDescription,_tmpOrder,_tmpReminderTimeMillis,_tmpReminderStyle,_tmpUpdatedAt,_tmpSyncStatus,_tmpIsDeleted)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getDeletedUnsyncedTasks(): List<TaskEntity> {
    val _sql: String = "SELECT * FROM tasks WHERE isDeleted = 1 AND syncStatus != 'SYNCED'"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfBoardId: Int = getColumnIndexOrThrow(_stmt, "boardId")
        val _columnIndexOfColumnId: Int = getColumnIndexOrThrow(_stmt, "columnId")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfOrder: Int = getColumnIndexOrThrow(_stmt, "order")
        val _columnIndexOfReminderTimeMillis: Int = getColumnIndexOrThrow(_stmt,
            "reminderTimeMillis")
        val _columnIndexOfReminderStyle: Int = getColumnIndexOrThrow(_stmt, "reminderStyle")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfSyncStatus: Int = getColumnIndexOrThrow(_stmt, "syncStatus")
        val _columnIndexOfIsDeleted: Int = getColumnIndexOrThrow(_stmt, "isDeleted")
        val _result: MutableList<TaskEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: TaskEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpBoardId: String
          _tmpBoardId = _stmt.getText(_columnIndexOfBoardId)
          val _tmpColumnId: String
          _tmpColumnId = _stmt.getText(_columnIndexOfColumnId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpOrder: Double
          _tmpOrder = _stmt.getDouble(_columnIndexOfOrder)
          val _tmpReminderTimeMillis: Long?
          if (_stmt.isNull(_columnIndexOfReminderTimeMillis)) {
            _tmpReminderTimeMillis = null
          } else {
            _tmpReminderTimeMillis = _stmt.getLong(_columnIndexOfReminderTimeMillis)
          }
          val _tmpReminderStyle: TaskEntity.ReminderStyle
          val _tmp: String
          _tmp = _stmt.getText(_columnIndexOfReminderStyle)
          _tmpReminderStyle = __converters.toTaskReminderStyle(_tmp)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpSyncStatus: SyncStatus
          val _tmp_1: String
          _tmp_1 = _stmt.getText(_columnIndexOfSyncStatus)
          _tmpSyncStatus = __converters.toSyncStatus(_tmp_1)
          val _tmpIsDeleted: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfIsDeleted).toInt()
          _tmpIsDeleted = _tmp_2 != 0
          _item =
              TaskEntity(_tmpId,_tmpBoardId,_tmpColumnId,_tmpTitle,_tmpDescription,_tmpOrder,_tmpReminderTimeMillis,_tmpReminderStyle,_tmpUpdatedAt,_tmpSyncStatus,_tmpIsDeleted)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getTasksWithReminders(): List<TaskEntity> {
    val _sql: String = "SELECT * FROM tasks WHERE reminderTimeMillis IS NOT NULL AND isDeleted = 0"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfBoardId: Int = getColumnIndexOrThrow(_stmt, "boardId")
        val _columnIndexOfColumnId: Int = getColumnIndexOrThrow(_stmt, "columnId")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfOrder: Int = getColumnIndexOrThrow(_stmt, "order")
        val _columnIndexOfReminderTimeMillis: Int = getColumnIndexOrThrow(_stmt,
            "reminderTimeMillis")
        val _columnIndexOfReminderStyle: Int = getColumnIndexOrThrow(_stmt, "reminderStyle")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfSyncStatus: Int = getColumnIndexOrThrow(_stmt, "syncStatus")
        val _columnIndexOfIsDeleted: Int = getColumnIndexOrThrow(_stmt, "isDeleted")
        val _result: MutableList<TaskEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: TaskEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpBoardId: String
          _tmpBoardId = _stmt.getText(_columnIndexOfBoardId)
          val _tmpColumnId: String
          _tmpColumnId = _stmt.getText(_columnIndexOfColumnId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpOrder: Double
          _tmpOrder = _stmt.getDouble(_columnIndexOfOrder)
          val _tmpReminderTimeMillis: Long?
          if (_stmt.isNull(_columnIndexOfReminderTimeMillis)) {
            _tmpReminderTimeMillis = null
          } else {
            _tmpReminderTimeMillis = _stmt.getLong(_columnIndexOfReminderTimeMillis)
          }
          val _tmpReminderStyle: TaskEntity.ReminderStyle
          val _tmp: String
          _tmp = _stmt.getText(_columnIndexOfReminderStyle)
          _tmpReminderStyle = __converters.toTaskReminderStyle(_tmp)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpSyncStatus: SyncStatus
          val _tmp_1: String
          _tmp_1 = _stmt.getText(_columnIndexOfSyncStatus)
          _tmpSyncStatus = __converters.toSyncStatus(_tmp_1)
          val _tmpIsDeleted: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfIsDeleted).toInt()
          _tmpIsDeleted = _tmp_2 != 0
          _item =
              TaskEntity(_tmpId,_tmpBoardId,_tmpColumnId,_tmpTitle,_tmpDescription,_tmpOrder,_tmpReminderTimeMillis,_tmpReminderStyle,_tmpUpdatedAt,_tmpSyncStatus,_tmpIsDeleted)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun markSynced(taskId: String) {
    val _sql: String = "UPDATE tasks SET syncStatus = 'SYNCED' WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, taskId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun softDelete(taskId: String, updatedAt: Long) {
    val _sql: String =
        "UPDATE tasks SET isDeleted = 1, updatedAt = ?, syncStatus = 'PENDING' WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, updatedAt)
        _argIndex = 2
        _stmt.bindText(_argIndex, taskId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
