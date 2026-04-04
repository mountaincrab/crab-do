package com.mountaincrab.crabdo.`data`.local.dao

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.mountaincrab.crabdo.`data`.local.Converters
import com.mountaincrab.crabdo.`data`.local.entity.ReminderEntity
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
public class ReminderDao_Impl(
  __db: RoomDatabase,
) : ReminderDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfReminderEntity: EntityInsertAdapter<ReminderEntity>

  private val __converters: Converters = Converters()
  init {
    this.__db = __db
    this.__insertAdapterOfReminderEntity = object : EntityInsertAdapter<ReminderEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `reminders` (`id`,`userId`,`title`,`nextTriggerMillis`,`reminderStyle`,`recurrenceRuleJson`,`isEnabled`,`createdAt`,`updatedAt`,`syncStatus`,`isDeleted`) VALUES (?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: ReminderEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.userId)
        statement.bindText(3, entity.title)
        statement.bindLong(4, entity.nextTriggerMillis)
        val _tmp: String = __converters.fromReminderEntityStyle(entity.reminderStyle)
        statement.bindText(5, _tmp)
        val _tmpRecurrenceRuleJson: String? = entity.recurrenceRuleJson
        if (_tmpRecurrenceRuleJson == null) {
          statement.bindNull(6)
        } else {
          statement.bindText(6, _tmpRecurrenceRuleJson)
        }
        val _tmp_1: Int = if (entity.isEnabled) 1 else 0
        statement.bindLong(7, _tmp_1.toLong())
        statement.bindLong(8, entity.createdAt)
        statement.bindLong(9, entity.updatedAt)
        val _tmp_2: String = __converters.fromSyncStatus(entity.syncStatus)
        statement.bindText(10, _tmp_2)
        val _tmp_3: Int = if (entity.isDeleted) 1 else 0
        statement.bindLong(11, _tmp_3.toLong())
      }
    }
  }

  public override suspend fun upsert(reminder: ReminderEntity): Unit = performSuspending(__db,
      false, true) { _connection ->
    __insertAdapterOfReminderEntity.insert(_connection, reminder)
  }

  public override fun observeReminders(userId: String): Flow<List<ReminderEntity>> {
    val _sql: String =
        "SELECT * FROM reminders WHERE userId = ? AND isDeleted = 0 ORDER BY nextTriggerMillis"
    return createFlow(__db, false, arrayOf("reminders")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, userId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfUserId: Int = getColumnIndexOrThrow(_stmt, "userId")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfNextTriggerMillis: Int = getColumnIndexOrThrow(_stmt, "nextTriggerMillis")
        val _columnIndexOfReminderStyle: Int = getColumnIndexOrThrow(_stmt, "reminderStyle")
        val _columnIndexOfRecurrenceRuleJson: Int = getColumnIndexOrThrow(_stmt,
            "recurrenceRuleJson")
        val _columnIndexOfIsEnabled: Int = getColumnIndexOrThrow(_stmt, "isEnabled")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfSyncStatus: Int = getColumnIndexOrThrow(_stmt, "syncStatus")
        val _columnIndexOfIsDeleted: Int = getColumnIndexOrThrow(_stmt, "isDeleted")
        val _result: MutableList<ReminderEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ReminderEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpUserId: String
          _tmpUserId = _stmt.getText(_columnIndexOfUserId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpNextTriggerMillis: Long
          _tmpNextTriggerMillis = _stmt.getLong(_columnIndexOfNextTriggerMillis)
          val _tmpReminderStyle: ReminderEntity.ReminderStyle
          val _tmp: String
          _tmp = _stmt.getText(_columnIndexOfReminderStyle)
          _tmpReminderStyle = __converters.toReminderEntityStyle(_tmp)
          val _tmpRecurrenceRuleJson: String?
          if (_stmt.isNull(_columnIndexOfRecurrenceRuleJson)) {
            _tmpRecurrenceRuleJson = null
          } else {
            _tmpRecurrenceRuleJson = _stmt.getText(_columnIndexOfRecurrenceRuleJson)
          }
          val _tmpIsEnabled: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsEnabled).toInt()
          _tmpIsEnabled = _tmp_1 != 0
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpSyncStatus: SyncStatus
          val _tmp_2: String
          _tmp_2 = _stmt.getText(_columnIndexOfSyncStatus)
          _tmpSyncStatus = __converters.toSyncStatus(_tmp_2)
          val _tmpIsDeleted: Boolean
          val _tmp_3: Int
          _tmp_3 = _stmt.getLong(_columnIndexOfIsDeleted).toInt()
          _tmpIsDeleted = _tmp_3 != 0
          _item =
              ReminderEntity(_tmpId,_tmpUserId,_tmpTitle,_tmpNextTriggerMillis,_tmpReminderStyle,_tmpRecurrenceRuleJson,_tmpIsEnabled,_tmpCreatedAt,_tmpUpdatedAt,_tmpSyncStatus,_tmpIsDeleted)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getReminderById(id: String): ReminderEntity? {
    val _sql: String = "SELECT * FROM reminders WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfUserId: Int = getColumnIndexOrThrow(_stmt, "userId")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfNextTriggerMillis: Int = getColumnIndexOrThrow(_stmt, "nextTriggerMillis")
        val _columnIndexOfReminderStyle: Int = getColumnIndexOrThrow(_stmt, "reminderStyle")
        val _columnIndexOfRecurrenceRuleJson: Int = getColumnIndexOrThrow(_stmt,
            "recurrenceRuleJson")
        val _columnIndexOfIsEnabled: Int = getColumnIndexOrThrow(_stmt, "isEnabled")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfSyncStatus: Int = getColumnIndexOrThrow(_stmt, "syncStatus")
        val _columnIndexOfIsDeleted: Int = getColumnIndexOrThrow(_stmt, "isDeleted")
        val _result: ReminderEntity?
        if (_stmt.step()) {
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpUserId: String
          _tmpUserId = _stmt.getText(_columnIndexOfUserId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpNextTriggerMillis: Long
          _tmpNextTriggerMillis = _stmt.getLong(_columnIndexOfNextTriggerMillis)
          val _tmpReminderStyle: ReminderEntity.ReminderStyle
          val _tmp: String
          _tmp = _stmt.getText(_columnIndexOfReminderStyle)
          _tmpReminderStyle = __converters.toReminderEntityStyle(_tmp)
          val _tmpRecurrenceRuleJson: String?
          if (_stmt.isNull(_columnIndexOfRecurrenceRuleJson)) {
            _tmpRecurrenceRuleJson = null
          } else {
            _tmpRecurrenceRuleJson = _stmt.getText(_columnIndexOfRecurrenceRuleJson)
          }
          val _tmpIsEnabled: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsEnabled).toInt()
          _tmpIsEnabled = _tmp_1 != 0
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpSyncStatus: SyncStatus
          val _tmp_2: String
          _tmp_2 = _stmt.getText(_columnIndexOfSyncStatus)
          _tmpSyncStatus = __converters.toSyncStatus(_tmp_2)
          val _tmpIsDeleted: Boolean
          val _tmp_3: Int
          _tmp_3 = _stmt.getLong(_columnIndexOfIsDeleted).toInt()
          _tmpIsDeleted = _tmp_3 != 0
          _result =
              ReminderEntity(_tmpId,_tmpUserId,_tmpTitle,_tmpNextTriggerMillis,_tmpReminderStyle,_tmpRecurrenceRuleJson,_tmpIsEnabled,_tmpCreatedAt,_tmpUpdatedAt,_tmpSyncStatus,_tmpIsDeleted)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAllActiveReminders(): List<ReminderEntity> {
    val _sql: String = "SELECT * FROM reminders WHERE isEnabled = 1 AND isDeleted = 0"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfUserId: Int = getColumnIndexOrThrow(_stmt, "userId")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfNextTriggerMillis: Int = getColumnIndexOrThrow(_stmt, "nextTriggerMillis")
        val _columnIndexOfReminderStyle: Int = getColumnIndexOrThrow(_stmt, "reminderStyle")
        val _columnIndexOfRecurrenceRuleJson: Int = getColumnIndexOrThrow(_stmt,
            "recurrenceRuleJson")
        val _columnIndexOfIsEnabled: Int = getColumnIndexOrThrow(_stmt, "isEnabled")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfSyncStatus: Int = getColumnIndexOrThrow(_stmt, "syncStatus")
        val _columnIndexOfIsDeleted: Int = getColumnIndexOrThrow(_stmt, "isDeleted")
        val _result: MutableList<ReminderEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ReminderEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpUserId: String
          _tmpUserId = _stmt.getText(_columnIndexOfUserId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpNextTriggerMillis: Long
          _tmpNextTriggerMillis = _stmt.getLong(_columnIndexOfNextTriggerMillis)
          val _tmpReminderStyle: ReminderEntity.ReminderStyle
          val _tmp: String
          _tmp = _stmt.getText(_columnIndexOfReminderStyle)
          _tmpReminderStyle = __converters.toReminderEntityStyle(_tmp)
          val _tmpRecurrenceRuleJson: String?
          if (_stmt.isNull(_columnIndexOfRecurrenceRuleJson)) {
            _tmpRecurrenceRuleJson = null
          } else {
            _tmpRecurrenceRuleJson = _stmt.getText(_columnIndexOfRecurrenceRuleJson)
          }
          val _tmpIsEnabled: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsEnabled).toInt()
          _tmpIsEnabled = _tmp_1 != 0
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpSyncStatus: SyncStatus
          val _tmp_2: String
          _tmp_2 = _stmt.getText(_columnIndexOfSyncStatus)
          _tmpSyncStatus = __converters.toSyncStatus(_tmp_2)
          val _tmpIsDeleted: Boolean
          val _tmp_3: Int
          _tmp_3 = _stmt.getLong(_columnIndexOfIsDeleted).toInt()
          _tmpIsDeleted = _tmp_3 != 0
          _item =
              ReminderEntity(_tmpId,_tmpUserId,_tmpTitle,_tmpNextTriggerMillis,_tmpReminderStyle,_tmpRecurrenceRuleJson,_tmpIsEnabled,_tmpCreatedAt,_tmpUpdatedAt,_tmpSyncStatus,_tmpIsDeleted)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getUnsyncedReminders(): List<ReminderEntity> {
    val _sql: String = "SELECT * FROM reminders WHERE syncStatus != 'SYNCED' AND isDeleted = 0"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfUserId: Int = getColumnIndexOrThrow(_stmt, "userId")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfNextTriggerMillis: Int = getColumnIndexOrThrow(_stmt, "nextTriggerMillis")
        val _columnIndexOfReminderStyle: Int = getColumnIndexOrThrow(_stmt, "reminderStyle")
        val _columnIndexOfRecurrenceRuleJson: Int = getColumnIndexOrThrow(_stmt,
            "recurrenceRuleJson")
        val _columnIndexOfIsEnabled: Int = getColumnIndexOrThrow(_stmt, "isEnabled")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfSyncStatus: Int = getColumnIndexOrThrow(_stmt, "syncStatus")
        val _columnIndexOfIsDeleted: Int = getColumnIndexOrThrow(_stmt, "isDeleted")
        val _result: MutableList<ReminderEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ReminderEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpUserId: String
          _tmpUserId = _stmt.getText(_columnIndexOfUserId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpNextTriggerMillis: Long
          _tmpNextTriggerMillis = _stmt.getLong(_columnIndexOfNextTriggerMillis)
          val _tmpReminderStyle: ReminderEntity.ReminderStyle
          val _tmp: String
          _tmp = _stmt.getText(_columnIndexOfReminderStyle)
          _tmpReminderStyle = __converters.toReminderEntityStyle(_tmp)
          val _tmpRecurrenceRuleJson: String?
          if (_stmt.isNull(_columnIndexOfRecurrenceRuleJson)) {
            _tmpRecurrenceRuleJson = null
          } else {
            _tmpRecurrenceRuleJson = _stmt.getText(_columnIndexOfRecurrenceRuleJson)
          }
          val _tmpIsEnabled: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsEnabled).toInt()
          _tmpIsEnabled = _tmp_1 != 0
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpSyncStatus: SyncStatus
          val _tmp_2: String
          _tmp_2 = _stmt.getText(_columnIndexOfSyncStatus)
          _tmpSyncStatus = __converters.toSyncStatus(_tmp_2)
          val _tmpIsDeleted: Boolean
          val _tmp_3: Int
          _tmp_3 = _stmt.getLong(_columnIndexOfIsDeleted).toInt()
          _tmpIsDeleted = _tmp_3 != 0
          _item =
              ReminderEntity(_tmpId,_tmpUserId,_tmpTitle,_tmpNextTriggerMillis,_tmpReminderStyle,_tmpRecurrenceRuleJson,_tmpIsEnabled,_tmpCreatedAt,_tmpUpdatedAt,_tmpSyncStatus,_tmpIsDeleted)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun markSynced(reminderId: String) {
    val _sql: String = "UPDATE reminders SET syncStatus = 'SYNCED' WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, reminderId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updateNextTrigger(
    reminderId: String,
    nextTriggerMillis: Long,
    updatedAt: Long,
  ) {
    val _sql: String = "UPDATE reminders SET nextTriggerMillis = ?, updatedAt = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, nextTriggerMillis)
        _argIndex = 2
        _stmt.bindLong(_argIndex, updatedAt)
        _argIndex = 3
        _stmt.bindText(_argIndex, reminderId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun softDelete(reminderId: String, updatedAt: Long) {
    val _sql: String =
        "UPDATE reminders SET isDeleted = 1, updatedAt = ?, syncStatus = 'PENDING' WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, updatedAt)
        _argIndex = 2
        _stmt.bindText(_argIndex, reminderId)
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
