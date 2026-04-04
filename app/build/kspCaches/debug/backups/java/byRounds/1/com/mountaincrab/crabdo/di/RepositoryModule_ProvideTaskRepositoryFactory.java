package com.mountaincrab.crabdo.di;

import androidx.work.WorkManager;
import com.mountaincrab.crabdo.alarm.AlarmScheduler;
import com.mountaincrab.crabdo.data.local.dao.TaskDao;
import com.mountaincrab.crabdo.data.repository.TaskRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class RepositoryModule_ProvideTaskRepositoryFactory implements Factory<TaskRepository> {
  private final Provider<TaskDao> taskDaoProvider;

  private final Provider<AlarmScheduler> alarmSchedulerProvider;

  private final Provider<WorkManager> workManagerProvider;

  public RepositoryModule_ProvideTaskRepositoryFactory(Provider<TaskDao> taskDaoProvider,
      Provider<AlarmScheduler> alarmSchedulerProvider, Provider<WorkManager> workManagerProvider) {
    this.taskDaoProvider = taskDaoProvider;
    this.alarmSchedulerProvider = alarmSchedulerProvider;
    this.workManagerProvider = workManagerProvider;
  }

  @Override
  public TaskRepository get() {
    return provideTaskRepository(taskDaoProvider.get(), alarmSchedulerProvider.get(), workManagerProvider.get());
  }

  public static RepositoryModule_ProvideTaskRepositoryFactory create(
      Provider<TaskDao> taskDaoProvider, Provider<AlarmScheduler> alarmSchedulerProvider,
      Provider<WorkManager> workManagerProvider) {
    return new RepositoryModule_ProvideTaskRepositoryFactory(taskDaoProvider, alarmSchedulerProvider, workManagerProvider);
  }

  public static TaskRepository provideTaskRepository(TaskDao taskDao, AlarmScheduler alarmScheduler,
      WorkManager workManager) {
    return Preconditions.checkNotNullFromProvides(RepositoryModule.INSTANCE.provideTaskRepository(taskDao, alarmScheduler, workManager));
  }
}
