package com.mountaincrab.crabdo.data.repository;

import androidx.work.WorkManager;
import com.mountaincrab.crabdo.alarm.AlarmScheduler;
import com.mountaincrab.crabdo.data.local.dao.ReminderDao;
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
public final class ReminderRepository_Factory implements Factory<ReminderRepository> {
  private final Provider<ReminderDao> reminderDaoProvider;

  private final Provider<AlarmScheduler> alarmSchedulerProvider;

  private final Provider<WorkManager> workManagerProvider;

  public ReminderRepository_Factory(Provider<ReminderDao> reminderDaoProvider,
      Provider<AlarmScheduler> alarmSchedulerProvider, Provider<WorkManager> workManagerProvider) {
    this.reminderDaoProvider = reminderDaoProvider;
    this.alarmSchedulerProvider = alarmSchedulerProvider;
    this.workManagerProvider = workManagerProvider;
  }

  @Override
  public ReminderRepository get() {
    return newInstance(reminderDaoProvider.get(), alarmSchedulerProvider.get(), workManagerProvider.get());
  }

  public static ReminderRepository_Factory create(Provider<ReminderDao> reminderDaoProvider,
      Provider<AlarmScheduler> alarmSchedulerProvider, Provider<WorkManager> workManagerProvider) {
    return new ReminderRepository_Factory(reminderDaoProvider, alarmSchedulerProvider, workManagerProvider);
  }

  public static ReminderRepository newInstance(ReminderDao reminderDao,
      AlarmScheduler alarmScheduler, WorkManager workManager) {
    return new ReminderRepository(reminderDao, alarmScheduler, workManager);
  }
}
