package com.mountaincrab.crabdo.ui.reminders;

import androidx.lifecycle.SavedStateHandle;
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
public final class AddEditReminderViewModel_Factory implements Factory<AddEditReminderViewModel> {
  private final Provider<ReminderRepository> reminderRepositoryProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<SavedStateHandle> savedStateHandleProvider;

  public AddEditReminderViewModel_Factory(Provider<ReminderRepository> reminderRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    this.reminderRepositoryProvider = reminderRepositoryProvider;
    this.authRepositoryProvider = authRepositoryProvider;
    this.savedStateHandleProvider = savedStateHandleProvider;
  }

  @Override
  public AddEditReminderViewModel get() {
    return newInstance(reminderRepositoryProvider.get(), authRepositoryProvider.get(), savedStateHandleProvider.get());
  }

  public static AddEditReminderViewModel_Factory create(
      Provider<ReminderRepository> reminderRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    return new AddEditReminderViewModel_Factory(reminderRepositoryProvider, authRepositoryProvider, savedStateHandleProvider);
  }

  public static AddEditReminderViewModel newInstance(ReminderRepository reminderRepository,
      AuthRepository authRepository, SavedStateHandle savedStateHandle) {
    return new AddEditReminderViewModel(reminderRepository, authRepository, savedStateHandle);
  }
}
