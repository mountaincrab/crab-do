package com.mountaincrab.crabdo.ui.boards;

import androidx.lifecycle.SavedStateHandle;
import com.mountaincrab.crabdo.data.repository.BoardRepository;
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
public final class KanbanBoardViewModel_Factory implements Factory<KanbanBoardViewModel> {
  private final Provider<BoardRepository> boardRepositoryProvider;

  private final Provider<TaskRepository> taskRepositoryProvider;

  private final Provider<SavedStateHandle> savedStateHandleProvider;

  public KanbanBoardViewModel_Factory(Provider<BoardRepository> boardRepositoryProvider,
      Provider<TaskRepository> taskRepositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    this.boardRepositoryProvider = boardRepositoryProvider;
    this.taskRepositoryProvider = taskRepositoryProvider;
    this.savedStateHandleProvider = savedStateHandleProvider;
  }

  @Override
  public KanbanBoardViewModel get() {
    return newInstance(boardRepositoryProvider.get(), taskRepositoryProvider.get(), savedStateHandleProvider.get());
  }

  public static KanbanBoardViewModel_Factory create(
      Provider<BoardRepository> boardRepositoryProvider,
      Provider<TaskRepository> taskRepositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    return new KanbanBoardViewModel_Factory(boardRepositoryProvider, taskRepositoryProvider, savedStateHandleProvider);
  }

  public static KanbanBoardViewModel newInstance(BoardRepository boardRepository,
      TaskRepository taskRepository, SavedStateHandle savedStateHandle) {
    return new KanbanBoardViewModel(boardRepository, taskRepository, savedStateHandle);
  }
}
