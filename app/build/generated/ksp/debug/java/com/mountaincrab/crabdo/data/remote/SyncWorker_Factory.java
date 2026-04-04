package com.mountaincrab.crabdo.data.remote;

import android.content.Context;
import androidx.work.WorkerParameters;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mountaincrab.crabdo.data.local.dao.BoardDao;
import com.mountaincrab.crabdo.data.local.dao.ColumnDao;
import com.mountaincrab.crabdo.data.local.dao.ReminderDao;
import com.mountaincrab.crabdo.data.local.dao.SubtaskDao;
import com.mountaincrab.crabdo.data.local.dao.TaskDao;
import com.mountaincrab.crabdo.preferences.UserPreferencesRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class SyncWorker_Factory {
  private final Provider<BoardDao> boardDaoProvider;

  private final Provider<ColumnDao> columnDaoProvider;

  private final Provider<TaskDao> taskDaoProvider;

  private final Provider<SubtaskDao> subtaskDaoProvider;

  private final Provider<ReminderDao> reminderDaoProvider;

  private final Provider<FirebaseFirestore> firestoreProvider;

  private final Provider<FirebaseAuth> authProvider;

  private final Provider<UserPreferencesRepository> prefsProvider;

  public SyncWorker_Factory(Provider<BoardDao> boardDaoProvider,
      Provider<ColumnDao> columnDaoProvider, Provider<TaskDao> taskDaoProvider,
      Provider<SubtaskDao> subtaskDaoProvider, Provider<ReminderDao> reminderDaoProvider,
      Provider<FirebaseFirestore> firestoreProvider, Provider<FirebaseAuth> authProvider,
      Provider<UserPreferencesRepository> prefsProvider) {
    this.boardDaoProvider = boardDaoProvider;
    this.columnDaoProvider = columnDaoProvider;
    this.taskDaoProvider = taskDaoProvider;
    this.subtaskDaoProvider = subtaskDaoProvider;
    this.reminderDaoProvider = reminderDaoProvider;
    this.firestoreProvider = firestoreProvider;
    this.authProvider = authProvider;
    this.prefsProvider = prefsProvider;
  }

  public SyncWorker get(Context context, WorkerParameters workerParams) {
    return newInstance(context, workerParams, boardDaoProvider.get(), columnDaoProvider.get(), taskDaoProvider.get(), subtaskDaoProvider.get(), reminderDaoProvider.get(), firestoreProvider.get(), authProvider.get(), prefsProvider.get());
  }

  public static SyncWorker_Factory create(Provider<BoardDao> boardDaoProvider,
      Provider<ColumnDao> columnDaoProvider, Provider<TaskDao> taskDaoProvider,
      Provider<SubtaskDao> subtaskDaoProvider, Provider<ReminderDao> reminderDaoProvider,
      Provider<FirebaseFirestore> firestoreProvider, Provider<FirebaseAuth> authProvider,
      Provider<UserPreferencesRepository> prefsProvider) {
    return new SyncWorker_Factory(boardDaoProvider, columnDaoProvider, taskDaoProvider, subtaskDaoProvider, reminderDaoProvider, firestoreProvider, authProvider, prefsProvider);
  }

  public static SyncWorker newInstance(Context context, WorkerParameters workerParams,
      BoardDao boardDao, ColumnDao columnDao, TaskDao taskDao, SubtaskDao subtaskDao,
      ReminderDao reminderDao, FirebaseFirestore firestore, FirebaseAuth auth,
      UserPreferencesRepository prefs) {
    return new SyncWorker(context, workerParams, boardDao, columnDao, taskDao, subtaskDao, reminderDao, firestore, auth, prefs);
  }
}
