package com.mountaincrab.crabdo.`data`.local.dao

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.mountaincrab.crabdo.`data`.local.Converters
import com.mountaincrab.crabdo.`data`.local.entity.SubtaskEntity
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
public class SubtaskDao_Impl(
  __db: RoomDatabase,
) : SubtaskDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfSubtaskEntity: EntityInsertAdapter<SubtaskEntity>

  private val __converters: Converters = Converters()
  init {
    this.__db = __db
    this.__insertAdapterOfSubtaskEntity = object : EntityInsertAdapter<SubtaskEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `subtasks` (`id`,`taskId`,`title`,`isCompleted`,`order`,`updatedAt`,`syncStatus`,`isDeleted`) VALUES (?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: SubtaskEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.taskId)
        statement.bindText(3, entity.title)
        val _tmp: Int = if (entity.isCompleted) 1 else 0
        statement.bindLong(4, _tmp.toLong())
        statement.bindDouble(5, entity.order)
        statement.bindLong(6, entity.updatedAt)
        val _tmp_1: String = __converters.fromSyncStatus(entity.syncStatus)
        statement.bindText(7, _tmp_1)
        val _tmp_2: Int = if (entity.isDeleted) 1 else 0
        statement.bindLong(8, _tmp_2.toLong())
      }
    }
  }

  public override suspend fun upsert(subtask: SubtaskEntity): Unit = performSuspending(__db, false,
      true) { _connection ->
    __insertAdapterOfSubtaskEntity.insert(_connection, subtask)
  }

  public override fun observeSubtasks(taskId: String): Flow<List<SubtaskEntity>> {
    val _sql: String = "SELECT * FROM subtasks WHERE taskId = ? AND isDeleted = 0 ORDER BY `order`"
    return createFlow(__db, false, arrayOf("subtasks")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, taskId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTaskId: Int = getColumnIndexOrThrow(_stmt, "taskId")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfIsCompleted: Int = getColumnIndexOrThrow(_stmt, "isCompleted")
        val _columnIndexOfOrder: Int = getColumnIndexOrThrow(_stmt, "order")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfSyncStatus: Int = getColumnIndexOrThrow(_stmt, "syncStatus")
        val _columnIndexOfIsDeleted: Int = getColumnIndexOrThrow(_stmt, "isDeleted")
        val _result: MutableList<SubtaskEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: SubtaskEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpTaskId: String
          _tmpTaskId = _stmt.getText(_columnIndexOfTaskId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpIsCompleted: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsCompleted).toInt()
          _tmpIsCompleted = _tmp != 0
          val _tmpOrder: Double
          _tmpOrder = _stmt.getDouble(_columnIndexOfOrder)
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
              SubtaskEntity(_tmpId,_tmpTaskId,_tmpTitle,_tmpIsCompleted,_tmpOrder,_tmpUpdatedAt,_tmpSyncStatus,_tmpIsDeleted)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getSubtasksByTask(taskId: String): List<SubtaskEntity> {
    val _sql: String = "SELECT * FROM subtasks WHERE taskId = ? AND isDeleted = 0 ORDER BY `order`"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, taskId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTaskId: Int = getColumnIndexOrThrow(_stmt, "taskId")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfIsCompleted: Int = getColumnIndexOrThrow(_stmt, "isCompleted")
        val _columnIndexOfOrder: Int = getColumnIndexOrThrow(_stmt, "order")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfSyncStatus: Int = getColumnIndexOrThrow(_stmt, "syncStatus")
        val _columnIndexOfIsDeleted: Int = getColumnIndexOrThrow(_stmt, "isDeleted")
        val _result: MutableList<SubtaskEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: SubtaskEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpTaskId: String
          _tmpTaskId = _stmt.getText(_columnIndexOfTaskId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpIsCompleted: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsCompleted).toInt()
          _tmpIsCompleted = _tmp != 0
          val _tmpOrder: Double
          _tmpOrder = _stmt.getDouble(_columnIndexOfOrder)
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
              SubtaskEntity(_tmpId,_tmpTaskId,_tmpTitle,_tmpIsCompleted,_tmpOrder,_tmpUpdatedAt,_tmpSyncStatus,_tmpIsDeleted)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getUnsyncedSubtasks(): List<SubtaskEntity> {
    val _sql: String = "SELECT * FROM subtasks WHERE syncStatus != 'SYNCED'"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTaskId: Int = getColumnIndexOrThrow(_stmt, "taskId")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfIsCompleted: Int = getColumnIndexOrThrow(_stmt, "isCompleted")
        val _columnIndexOfOrder: Int = getColumnIndexOrThrow(_stmt, "order")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfSyncStatus: Int = getColumnIndexOrThrow(_stmt, "syncStatus")
        val _columnIndexOfIsDeleted: Int = getColumnIndexOrThrow(_stmt, "isDeleted")
        val _result: MutableList<SubtaskEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: SubtaskEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpTaskId: String
          _tmpTaskId = _stmt.getText(_columnIndexOfTaskId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpIsCompleted: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsCompleted).toInt()
          _tmpIsCompleted = _tmp != 0
          val _tmpOrder: Double
          _tmpOrder = _stmt.getDouble(_columnIndexOfOrder)
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
              SubtaskEntity(_tmpId,_tmpTaskId,_tmpTitle,_tmpIsCompleted,_tmpOrder,_tmpUpdatedAt,_tmpSyncStatus,_tmpIsDeleted)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun setCompleted(
    subtaskId: String,
    isCompleted: Boolean,
    updatedAt: Long,
  ) {
    val _sql: String =
        "UPDATE subtasks SET isCompleted = ?, updatedAt = ?, syncStatus = 'PENDING' WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        val _tmp: Int = if (isCompleted) 1 else 0
        _stmt.bindLong(_argIndex, _tmp.toLong())
        _argIndex = 2
        _stmt.bindLong(_argIndex, updatedAt)
        _argIndex = 3
        _stmt.bindText(_argIndex, subtaskId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun markSynced(subtaskId: String) {
    val _sql: String = "UPDATE subtasks SET syncStatus = 'SYNCED' WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, subtaskId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun softDelete(subtaskId: String, updatedAt: Long) {
    val _sql: String =
        "UPDATE subtasks SET isDeleted = 1, updatedAt = ?, syncStatus = 'PENDING' WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, updatedAt)
        _argIndex = 2
        _stmt.bindText(_argIndex, subtaskId)
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
