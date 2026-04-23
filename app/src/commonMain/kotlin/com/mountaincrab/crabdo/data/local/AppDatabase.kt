package com.mountaincrab.crabdo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.mountaincrab.crabdo.data.local.dao.*
import com.mountaincrab.crabdo.data.local.entity.*
import com.mountaincrab.crabdo.data.model.SyncStatus

@Database(
    entities = [
        BoardEntity::class,
        ColumnEntity::class,
        TaskEntity::class,
        SubtaskEntity::class,
        OneOffReminderEntity::class,
        RecurringReminderEntity::class,
        BoardAccessEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun boardDao(): BoardDao
    abstract fun columnDao(): ColumnDao
    abstract fun taskDao(): TaskDao
    abstract fun subtaskDao(): SubtaskDao
    abstract fun oneOffReminderDao(): OneOffReminderDao
    abstract fun recurringReminderDao(): RecurringReminderDao
    abstract fun boardAccessDao(): BoardAccessDao
}

class Converters {
    @TypeConverter fun fromSyncStatus(value: SyncStatus): String = value.name
    @TypeConverter fun toSyncStatus(value: String): SyncStatus = SyncStatus.valueOf(value)

    @TypeConverter fun fromTaskReminderStyle(value: TaskEntity.ReminderStyle): String = value.name
    @TypeConverter fun toTaskReminderStyle(value: String): TaskEntity.ReminderStyle =
        TaskEntity.ReminderStyle.valueOf(value)

    @TypeConverter fun fromReminderStyle(value: ReminderStyle): String = value.name
    @TypeConverter fun toReminderStyle(value: String): ReminderStyle = ReminderStyle.valueOf(value)
}
