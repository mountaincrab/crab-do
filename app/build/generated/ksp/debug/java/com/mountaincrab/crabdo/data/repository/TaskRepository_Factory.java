package com.mountaincrab.crabdo.data.repository;

import androidx.work.WorkManager;
import com.mountaincrab.crabdo.alarm.AlarmScheduler;
import com.mountaincrab.crabdo.data.local.dao.TaskDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class TaskRepository_Factory implements Factory<TaskRepository> {
  private final Provider<TaskDao> taskDaoProvider;

  private final Provider<AlarmScheduler> alarmSchedulerProvider;

  private final Provider<WorkManager> workManagerProvider;

  public TaskRepository_Factory(Provider<TaskDao> taskDaoProvider,
      Provider<AlarmScheduler> alarmSchedulerProvider, Provider<WorkManager> workManagerProvider) {
    this.taskDaoProvider = taskDaoProvider;
    this.alarmSchedulerProvider = alarmSchedulerProvider;
    this.workManagerProvider = workManagerProvider;
  }

  @Override
  public TaskRepository get() {
    return newInstance(taskDaoProvider.get(), alarmSchedulerProvider.get(), workManagerProvider.get());
  }

  public static TaskRepository_Factory create(Provider<TaskDao> taskDaoProvider,
      Provider<AlarmScheduler> alarmSchedulerProvider, Provider<WorkManager> workManagerProvider) {
    return new TaskRepository_Factory(taskDaoProvider, alarmSchedulerProvider, workManagerProvider);
  }

  public static TaskRepository newInstance(TaskDao taskDao, AlarmScheduler alarmScheduler,
      WorkManager workManager) {
    return new TaskRepository(taskDao, alarmScheduler, workManager);
  }
}
