package com.mountaincrab.crabdo.di

import android.content.Context
import androidx.room.Room
import com.mountaincrab.crabdo.data.local.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "crabban_db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideBoardDao(db: AppDatabase) = db.boardDao()
    @Provides fun provideColumnDao(db: AppDatabase) = db.columnDao()
    @Provides fun provideTaskDao(db: AppDatabase) = db.taskDao()
    @Provides fun provideSubtaskDao(db: AppDatabase) = db.subtaskDao()
    @Provides fun provideReminderDao(db: AppDatabase) = db.reminderDao()
}
