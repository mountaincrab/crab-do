package com.mountaincrab.crabdo.di

import android.app.Application
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mountaincrab.crabdo.alarm.AlarmScheduler
import com.mountaincrab.crabdo.auth.AuthRepository
import com.mountaincrab.crabdo.data.local.AppDatabase
import com.mountaincrab.crabdo.data.repository.*
import com.mountaincrab.crabdo.preferences.UserPreferencesRepository
import com.mountaincrab.crabdo.ui.auth.LoginViewModel
import com.mountaincrab.crabdo.ui.boards.BoardListViewModel
import com.mountaincrab.crabdo.ui.boards.KanbanBoardViewModel
import com.mountaincrab.crabdo.ui.boards.TaskDetailViewModel
import com.mountaincrab.crabdo.ui.reminders.AddEditOneOffReminderViewModel
import com.mountaincrab.crabdo.ui.reminders.AddEditRecurringReminderViewModel
import com.mountaincrab.crabdo.ui.reminders.RemindersViewModel
import com.mountaincrab.crabdo.ui.settings.SettingsViewModel
import com.mountaincrab.crabdo.ui.theme.ThemeViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Firebase
    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }

    // WorkManager
    single { WorkManager.getInstance(androidContext()) }

    // AlarmScheduler
    single { AlarmScheduler(androidContext()) }

    // UserPreferences (DataStore)
    single { UserPreferencesRepository(androidContext()) }

    // Room database
    single {
        Room.databaseBuilder<AppDatabase>(
            context = androidContext(),
            name = "crabban_db"
        )
            .setDriver(BundledSQLiteDriver())
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    // DAOs
    single { get<AppDatabase>().boardDao() }
    single { get<AppDatabase>().columnDao() }
    single { get<AppDatabase>().taskDao() }
    single { get<AppDatabase>().subtaskDao() }
    single { get<AppDatabase>().oneOffReminderDao() }
    single { get<AppDatabase>().recurringReminderDao() }
    single { get<AppDatabase>().boardAccessDao() }

    // Repositories
    single {
        BoardRepository(
            boardDao = get(), columnDao = get(), taskDao = get(),
            subtaskDao = get(), firestore = get(), workManager = get()
        )
    }
    single { TaskRepository(taskDao = get(), alarmScheduler = get(), workManager = get()) }
    single { SubtaskRepository(subtaskDao = get(), workManager = get()) }
    single {
        ReminderRepository(
            oneOffDao = get(), recurringDao = get(), alarmScheduler = get(), workManager = get(),
            firebaseAuth = get(), firestore = get(), context = androidContext()
        )
    }
    single { AuthRepository(auth = get(), database = get(), userPreferences = get()) }
    single {
        InvitationRepository(
            firestore = get(), auth = get(), boardDao = get(),
            columnDao = get(), taskDao = get(), subtaskDao = get(),
            boardAccessDao = get()
        )
    }

    // ViewModels
    viewModel { LoginViewModel(authRepository = get(), boardRepository = get()) }
    viewModel {
        BoardListViewModel(
            boardRepository = get(), authRepository = get(),
            prefsRepository = get(), invitationRepository = get(),
            workManager = get(), reminderRepository = get()
        )
    }
    viewModel { (boardId: String) ->
        KanbanBoardViewModel(boardId = boardId, boardRepository = get(), taskRepository = get())
    }
    viewModel { (taskId: String) ->
        TaskDetailViewModel(
            taskId = taskId, taskRepository = get(),
            subtaskRepository = get(), userPrefsRepository = get()
        )
    }
    viewModel { RemindersViewModel(reminderRepository = get(), authRepository = get(), workManager = get()) }
    viewModel { (reminderId: String?) ->
        AddEditOneOffReminderViewModel(
            existingReminderId = reminderId, reminderRepository = get(),
            authRepository = get(), userPrefsRepository = get()
        )
    }
    viewModel { (reminderId: String?) ->
        AddEditRecurringReminderViewModel(
            existingReminderId = reminderId, reminderRepository = get(),
            authRepository = get(), userPrefsRepository = get()
        )
    }
    viewModel {
        SettingsViewModel(
            authRepository = get(), boardRepository = get(),
            prefsRepository = get(), context = get<Application>()
        )
    }
    viewModel { ThemeViewModel(prefs = get()) }
}
