package com.mountaincrab.crabdo.di

import android.content.Context
import androidx.work.WorkManager
import com.mountaincrab.crabdo.alarm.AlarmScheduler
import com.mountaincrab.crabdo.auth.AuthRepository
import com.mountaincrab.crabdo.data.local.AppDatabase
import com.mountaincrab.crabdo.data.local.dao.*
import com.mountaincrab.crabdo.data.repository.*
import com.mountaincrab.crabdo.preferences.UserPreferencesRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideBoardRepository(
        boardDao: BoardDao,
        columnDao: ColumnDao,
        taskDao: TaskDao,
        subtaskDao: SubtaskDao,
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        workManager: WorkManager
    ): BoardRepository = BoardRepository(boardDao, columnDao, taskDao, subtaskDao, firestore, auth, workManager)

    @Provides
    @Singleton
    fun provideTaskRepository(
        taskDao: TaskDao,
        alarmScheduler: AlarmScheduler,
        workManager: WorkManager
    ): TaskRepository = TaskRepository(taskDao, alarmScheduler, workManager)

    @Provides
    @Singleton
    fun provideSubtaskRepository(
        subtaskDao: SubtaskDao,
        workManager: WorkManager
    ): SubtaskRepository = SubtaskRepository(subtaskDao, workManager)

    @Provides
    @Singleton
    fun provideReminderRepository(
        reminderDao: ReminderDao,
        alarmScheduler: AlarmScheduler,
        workManager: WorkManager,
        firebaseAuth: FirebaseAuth,
        @ApplicationContext context: Context
    ): ReminderRepository = ReminderRepository(reminderDao, alarmScheduler, workManager, firebaseAuth, context)

    @Provides
    @Singleton
    fun provideAuthRepository(auth: FirebaseAuth, database: AppDatabase, userPreferences: UserPreferencesRepository): AuthRepository = AuthRepository(auth, database, userPreferences)
}
