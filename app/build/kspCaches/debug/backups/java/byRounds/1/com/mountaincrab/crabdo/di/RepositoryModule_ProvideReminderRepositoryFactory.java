package com.mountaincrab.crabdo.di;

import androidx.work.WorkManager;
import com.mountaincrab.crabdo.alarm.AlarmScheduler;
import com.mountaincrab.crabdo.data.local.dao.ReminderDao;
import com.mountaincrab.crabdo.data.repository.ReminderRepository;
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
public final class RepositoryModule_ProvideReminderRepositoryFactory implements Factory<ReminderRepository> {
  private final Provider<ReminderDao> reminderDaoProvider;

  private final Provider<AlarmScheduler> alarmSchedulerProvider;

  private final Provider<WorkManager> workManagerProvider;

  public RepositoryModule_ProvideReminderRepositoryFactory(
      Provider<ReminderDao> reminderDaoProvider, Provider<AlarmScheduler> alarmSchedulerProvider,
      Provider<WorkManager> workManagerProvider) {
    this.reminderDaoProvider = reminderDaoProvider;
    this.alarmSchedulerProvider = alarmSchedulerProvider;
    this.workManagerProvider = workManagerProvider;
  }

  @Override
  public ReminderRepository get() {
    return provideReminderRepository(reminderDaoProvider.get(), alarmSchedulerProvider.get(), workManagerProvider.get());
  }

  public static RepositoryModule_ProvideReminderRepositoryFactory create(
      Provider<ReminderDao> reminderDaoProvider, Provider<AlarmScheduler> alarmSchedulerProvider,
      Provider<WorkManager> workManagerProvider) {
    return new RepositoryModule_ProvideReminderRepositoryFactory(reminderDaoProvider, alarmSchedulerProvider, workManagerProvider);
  }

  public static ReminderRepository provideReminderRepository(ReminderDao reminderDao,
      AlarmScheduler alarmScheduler, WorkManager workManager) {
    return Preconditions.checkNotNullFromProvides(RepositoryModule.INSTANCE.provideReminderRepository(reminderDao, alarmScheduler, workManager));
  }
}
