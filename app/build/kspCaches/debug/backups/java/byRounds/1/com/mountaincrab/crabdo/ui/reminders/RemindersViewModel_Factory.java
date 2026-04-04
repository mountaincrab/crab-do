package com.mountaincrab.crabdo.ui.reminders;

import com.mountaincrab.crabdo.auth.AuthRepository;
import com.mountaincrab.crabdo.data.repository.ReminderRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class RemindersViewModel_Factory implements Factory<RemindersViewModel> {
  private final Provider<ReminderRepository> reminderRepositoryProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  public RemindersViewModel_Factory(Provider<ReminderRepository> reminderRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider) {
    this.reminderRepositoryProvider = reminderRepositoryProvider;
    this.authRepositoryProvider = authRepositoryProvider;
  }

  @Override
  public RemindersViewModel get() {
    return newInstance(reminderRepositoryProvider.get(), authRepositoryProvider.get());
  }

  public static RemindersViewModel_Factory create(
      Provider<ReminderRepository> reminderRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider) {
    return new RemindersViewModel_Factory(reminderRepositoryProvider, authRepositoryProvider);
  }

  public static RemindersViewModel newInstance(ReminderRepository reminderRepository,
      AuthRepository authRepository) {
    return new RemindersViewModel(reminderRepository, authRepository);
  }
}
