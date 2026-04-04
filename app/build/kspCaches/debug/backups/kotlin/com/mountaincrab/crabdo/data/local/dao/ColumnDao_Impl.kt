package com.mountaincrab.crabdo.`data`.local.dao

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.mountaincrab.crabdo.`data`.local.Converters
import com.mountaincrab.crabdo.`data`.local.entity.ColumnEntity
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
public class ColumnDao_Impl(
  __db: RoomDatabase,
) : ColumnDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfColumnEntity: EntityInsertAdapter<ColumnEntity>

  private val __converters: Converters = Converters()
  init {
    this.__db = __db
    this.__insertAdapterOfColumnEntity = object : EntityInsertAdapter<ColumnEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `columns` (`id`,`boardId`,`title`,`order`,`updatedAt`,`syncStatus`,`isDeleted`) VALUES (?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: ColumnEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.boardId)
        statement.bindText(3, entity.title)
        statement.bindDouble(4, entity.order)
        statement.bindLong(5, entity.updatedAt)
        val _tmp: String = __converters.fromSyncStatus(entity.syncStatus)
        statement.bindText(6, _tmp)
        val _tmp_1: Int = if (entity.isDeleted) 1 else 0
        statement.bindLong(7, _tmp_1.toLong())
      }
    }
  }

  public override suspend fun upsert(column: ColumnEntity): Unit = performSuspending(__db, false,
      true) { _connection ->
    __insertAdapterOfColumnEntity.insert(_connection, column)
  }

  public override fun observeColumnsByBoard(boardId: String): Flow<List<ColumnEntity>> {
    val _sql: String = "SELECT * FROM columns WHERE boardId = ? AND isDeleted = 0 ORDER BY `order`"
    return createFlow(__db, false, arrayOf("columns")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, boardId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfBoardId: Int = getColumnIndexOrThrow(_stmt, "boardId")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfOrder: Int = getColumnIndexOrThrow(_stmt, "order")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfSyncStatus: Int = getColumnIndexOrThrow(_stmt, "syncStatus")
        val _columnIndexOfIsDeleted: Int = getColumnIndexOrThrow(_stmt, "isDeleted")
        val _result: MutableList<ColumnEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ColumnEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpBoardId: String
          _tmpBoardId = _stmt.getText(_columnIndexOfBoardId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpOrder: Double
          _tmpOrder = _stmt.getDouble(_columnIndexOfOrder)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpSyncStatus: SyncStatus
          val _tmp: String
          _tmp = _stmt.getText(_columnIndexOfSyncStatus)
          _tmpSyncStatus = __converters.toSyncStatus(_tmp)
          val _tmpIsDeleted: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsDeleted).toInt()
          _tmpIsDeleted = _tmp_1 != 0
          _item =
              ColumnEntity(_tmpId,_tmpBoardId,_tmpTitle,_tmpOrder,_tmpUpdatedAt,_tmpSyncStatus,_tmpIsDeleted)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getColumnsByBoard(boardId: String): List<ColumnEntity> {
    val _sql: String = "SELECT * FROM columns WHERE boardId = ? AND isDeleted = 0 ORDER BY `order`"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, boardId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfBoardId: Int = getColumnIndexOrThrow(_stmt, "boardId")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfOrder: Int = getColumnIndexOrThrow(_stmt, "order")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfSyncStatus: Int = getColumnIndexOrThrow(_stmt, "syncStatus")
        val _columnIndexOfIsDeleted: Int = getColumnIndexOrThrow(_stmt, "isDeleted")
        val _result: MutableList<ColumnEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ColumnEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpBoardId: String
          _tmpBoardId = _stmt.getText(_columnIndexOfBoardId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpOrder: Double
          _tmpOrder = _stmt.getDouble(_columnIndexOfOrder)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpSyncStatus: SyncStatus
          val _tmp: String
          _tmp = _stmt.getText(_columnIndexOfSyncStatus)
          _tmpSyncStatus = __converters.toSyncStatus(_tmp)
          val _tmpIsDeleted: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsDeleted).toInt()
          _tmpIsDeleted = _tmp_1 != 0
          _item =
              ColumnEntity(_tmpId,_tmpBoardId,_tmpTitle,_tmpOrder,_tmpUpdatedAt,_tmpSyncStatus,_tmpIsDeleted)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getUnsyncedColumns(): List<ColumnEntity> {
    val _sql: String = "SELECT * FROM columns WHERE syncStatus != 'SYNCED'"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfBoardId: Int = getColumnIndexOrThrow(_stmt, "boardId")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfOrder: Int = getColumnIndexOrThrow(_stmt, "order")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfSyncStatus: Int = getColumnIndexOrThrow(_stmt, "syncStatus")
        val _columnIndexOfIsDeleted: Int = getColumnIndexOrThrow(_stmt, "isDeleted")
        val _result: MutableList<ColumnEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ColumnEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpBoardId: String
          _tmpBoardId = _stmt.getText(_columnIndexOfBoardId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpOrder: Double
          _tmpOrder = _stmt.getDouble(_columnIndexOfOrder)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpSyncStatus: SyncStatus
          val _tmp: String
          _tmp = _stmt.getText(_columnIndexOfSyncStatus)
          _tmpSyncStatus = __converters.toSyncStatus(_tmp)
          val _tmpIsDeleted: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsDeleted).toInt()
          _tmpIsDeleted = _tmp_1 != 0
          _item =
              ColumnEntity(_tmpId,_tmpBoardId,_tmpTitle,_tmpOrder,_tmpUpdatedAt,_tmpSyncStatus,_tmpIsDeleted)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun markSynced(columnId: String) {
    val _sql: String = "UPDATE columns SET syncStatus = 'SYNCED' WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, columnId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun softDelete(columnId: String, updatedAt: Long) {
    val _sql: String =
        "UPDATE columns SET isDeleted = 1, updatedAt = ?, syncStatus = 'PENDING' WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, updatedAt)
        _argIndex = 2
        _stmt.bindText(_argIndex, columnId)
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
