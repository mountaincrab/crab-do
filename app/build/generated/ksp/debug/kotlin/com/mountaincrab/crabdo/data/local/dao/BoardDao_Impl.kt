package com.mountaincrab.crabdo.`data`.local.dao

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.mountaincrab.crabdo.`data`.local.Converters
import com.mountaincrab.crabdo.`data`.local.entity.BoardEntity
import com.mountaincrab.crabdo.`data`.model.SyncStatus
import javax.`annotation`.processing.Generated
import kotlin.Boolean
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
public class BoardDao_Impl(
  __db: RoomDatabase,
) : BoardDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfBoardEntity: EntityInsertAdapter<BoardEntity>

  private val __converters: Converters = Converters()
  init {
    this.__db = __db
    this.__insertAdapterOfBoardEntity = object : EntityInsertAdapter<BoardEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `boards` (`id`,`userId`,`title`,`columnOrder`,`createdAt`,`updatedAt`,`syncStatus`,`isDeleted`) VALUES (?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: BoardEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.userId)
        statement.bindText(3, entity.title)
        statement.bindText(4, entity.columnOrder)
        statement.bindLong(5, entity.createdAt)
        statement.bindLong(6, entity.updatedAt)
        val _tmp: String = __converters.fromSyncStatus(entity.syncStatus)
        statement.bindText(7, _tmp)
        val _tmp_1: Int = if (entity.isDeleted) 1 else 0
        statement.bindLong(8, _tmp_1.toLong())
      }
    }
  }

  public override suspend fun upsert(board: BoardEntity): Unit = performSuspending(__db, false,
      true) { _connection ->
    __insertAdapterOfBoardEntity.insert(_connection, board)
  }

  public override fun observeBoards(userId: String): Flow<List<BoardEntity>> {
    val _sql: String = "SELECT * FROM boards WHERE userId = ? AND isDeleted = 0 ORDER BY createdAt"
    return createFlow(__db, false, arrayOf("boards")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, userId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfUserId: Int = getColumnIndexOrThrow(_stmt, "userId")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfColumnOrder: Int = getColumnIndexOrThrow(_stmt, "columnOrder")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfSyncStatus: Int = getColumnIndexOrThrow(_stmt, "syncStatus")
        val _columnIndexOfIsDeleted: Int = getColumnIndexOrThrow(_stmt, "isDeleted")
        val _result: MutableList<BoardEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: BoardEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpUserId: String
          _tmpUserId = _stmt.getText(_columnIndexOfUserId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpColumnOrder: String
          _tmpColumnOrder = _stmt.getText(_columnIndexOfColumnOrder)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
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
              BoardEntity(_tmpId,_tmpUserId,_tmpTitle,_tmpColumnOrder,_tmpCreatedAt,_tmpUpdatedAt,_tmpSyncStatus,_tmpIsDeleted)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeBoard(boardId: String): Flow<BoardEntity?> {
    val _sql: String = "SELECT * FROM boards WHERE id = ? AND isDeleted = 0"
    return createFlow(__db, false, arrayOf("boards")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, boardId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfUserId: Int = getColumnIndexOrThrow(_stmt, "userId")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfColumnOrder: Int = getColumnIndexOrThrow(_stmt, "columnOrder")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfSyncStatus: Int = getColumnIndexOrThrow(_stmt, "syncStatus")
        val _columnIndexOfIsDeleted: Int = getColumnIndexOrThrow(_stmt, "isDeleted")
        val _result: BoardEntity?
        if (_stmt.step()) {
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpUserId: String
          _tmpUserId = _stmt.getText(_columnIndexOfUserId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpColumnOrder: String
          _tmpColumnOrder = _stmt.getText(_columnIndexOfColumnOrder)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
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
          _result =
              BoardEntity(_tmpId,_tmpUserId,_tmpTitle,_tmpColumnOrder,_tmpCreatedAt,_tmpUpdatedAt,_tmpSyncStatus,_tmpIsDeleted)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getBoardById(boardId: String): BoardEntity? {
    val _sql: String = "SELECT * FROM boards WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, boardId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfUserId: Int = getColumnIndexOrThrow(_stmt, "userId")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfColumnOrder: Int = getColumnIndexOrThrow(_stmt, "columnOrder")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfSyncStatus: Int = getColumnIndexOrThrow(_stmt, "syncStatus")
        val _columnIndexOfIsDeleted: Int = getColumnIndexOrThrow(_stmt, "isDeleted")
        val _result: BoardEntity?
        if (_stmt.step()) {
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpUserId: String
          _tmpUserId = _stmt.getText(_columnIndexOfUserId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpColumnOrder: String
          _tmpColumnOrder = _stmt.getText(_columnIndexOfColumnOrder)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
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
          _result =
              BoardEntity(_tmpId,_tmpUserId,_tmpTitle,_tmpColumnOrder,_tmpCreatedAt,_tmpUpdatedAt,_tmpSyncStatus,_tmpIsDeleted)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getUnsyncedBoards(): List<BoardEntity> {
    val _sql: String = "SELECT * FROM boards WHERE syncStatus != 'SYNCED' AND isDeleted = 0"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfUserId: Int = getColumnIndexOrThrow(_stmt, "userId")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfColumnOrder: Int = getColumnIndexOrThrow(_stmt, "columnOrder")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfSyncStatus: Int = getColumnIndexOrThrow(_stmt, "syncStatus")
        val _columnIndexOfIsDeleted: Int = getColumnIndexOrThrow(_stmt, "isDeleted")
        val _result: MutableList<BoardEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: BoardEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpUserId: String
          _tmpUserId = _stmt.getText(_columnIndexOfUserId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpColumnOrder: String
          _tmpColumnOrder = _stmt.getText(_columnIndexOfColumnOrder)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
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
              BoardEntity(_tmpId,_tmpUserId,_tmpTitle,_tmpColumnOrder,_tmpCreatedAt,_tmpUpdatedAt,_tmpSyncStatus,_tmpIsDeleted)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getDeletedUnsyncedBoards(): List<BoardEntity> {
    val _sql: String = "SELECT * FROM boards WHERE isDeleted = 1 AND syncStatus != 'SYNCED'"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfUserId: Int = getColumnIndexOrThrow(_stmt, "userId")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfColumnOrder: Int = getColumnIndexOrThrow(_stmt, "columnOrder")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfSyncStatus: Int = getColumnIndexOrThrow(_stmt, "syncStatus")
        val _columnIndexOfIsDeleted: Int = getColumnIndexOrThrow(_stmt, "isDeleted")
        val _result: MutableList<BoardEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: BoardEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpUserId: String
          _tmpUserId = _stmt.getText(_columnIndexOfUserId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpColumnOrder: String
          _tmpColumnOrder = _stmt.getText(_columnIndexOfColumnOrder)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
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
              BoardEntity(_tmpId,_tmpUserId,_tmpTitle,_tmpColumnOrder,_tmpCreatedAt,_tmpUpdatedAt,_tmpSyncStatus,_tmpIsDeleted)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun markSynced(boardId: String) {
    val _sql: String = "UPDATE boards SET syncStatus = 'SYNCED' WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, boardId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updateColumnOrder(
    boardId: String,
    columnOrder: String,
    updatedAt: Long,
  ) {
    val _sql: String =
        "UPDATE boards SET columnOrder = ?, updatedAt = ?, syncStatus = 'PENDING' WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, columnOrder)
        _argIndex = 2
        _stmt.bindLong(_argIndex, updatedAt)
        _argIndex = 3
        _stmt.bindText(_argIndex, boardId)
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
