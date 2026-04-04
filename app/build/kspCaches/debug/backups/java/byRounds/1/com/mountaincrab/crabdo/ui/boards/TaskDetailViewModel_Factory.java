package com.mountaincrab.crabdo.ui.boards;

import androidx.lifecycle.SavedStateHandle;
import com.mountaincrab.crabdo.data.repository.SubtaskRepository;
import com.mountaincrab.crabdo.data.repository.TaskRepository;
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
public final class TaskDetailViewModel_Factory implements Factory<TaskDetailViewModel> {
  private final Provider<TaskRepository> taskRepositoryProvider;

  private final Provider<SubtaskRepository> subtaskRepositoryProvider;

  private final Provider<SavedStateHandle> savedStateHandleProvider;

  public TaskDetailViewModel_Factory(Provider<TaskRepository> taskRepositoryProvider,
      Provider<SubtaskRepository> subtaskRepositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    this.taskRepositoryProvider = taskRepositoryProvider;
    this.subtaskRepositoryProvider = subtaskRepositoryProvider;
    this.savedStateHandleProvider = savedStateHandleProvider;
  }

  @Override
  public TaskDetailViewModel get() {
    return newInstance(taskRepositoryProvider.get(), subtaskRepositoryProvider.get(), savedStateHandleProvider.get());
  }

  public static TaskDetailViewModel_Factory create(Provider<TaskRepository> taskRepositoryProvider,
      Provider<SubtaskRepository> subtaskRepositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    return new TaskDetailViewModel_Factory(taskRepositoryProvider, subtaskRepositoryProvider, savedStateHandleProvider);
  }

  public static TaskDetailViewModel newInstance(TaskRepository taskRepository,
      SubtaskRepository subtaskRepository, SavedStateHandle savedStateHandle) {
    return new TaskDetailViewModel(taskRepository, subtaskRepository, savedStateHandle);
  }
}
