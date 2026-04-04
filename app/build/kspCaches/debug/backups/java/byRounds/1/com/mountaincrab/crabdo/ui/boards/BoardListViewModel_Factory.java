package com.mountaincrab.crabdo.ui.boards;

import com.mountaincrab.crabdo.auth.AuthRepository;
import com.mountaincrab.crabdo.data.repository.BoardRepository;
import com.mountaincrab.crabdo.preferences.UserPreferencesRepository;
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
public final class BoardListViewModel_Factory implements Factory<BoardListViewModel> {
  private final Provider<BoardRepository> boardRepositoryProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<UserPreferencesRepository> prefsRepositoryProvider;

  public BoardListViewModel_Factory(Provider<BoardRepository> boardRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<UserPreferencesRepository> prefsRepositoryProvider) {
    this.boardRepositoryProvider = boardRepositoryProvider;
    this.authRepositoryProvider = authRepositoryProvider;
    this.prefsRepositoryProvider = prefsRepositoryProvider;
  }

  @Override
  public BoardListViewModel get() {
    return newInstance(boardRepositoryProvider.get(), authRepositoryProvider.get(), prefsRepositoryProvider.get());
  }

  public static BoardListViewModel_Factory create(Provider<BoardRepository> boardRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<UserPreferencesRepository> prefsRepositoryProvider) {
    return new BoardListViewModel_Factory(boardRepositoryProvider, authRepositoryProvider, prefsRepositoryProvider);
  }

  public static BoardListViewModel newInstance(BoardRepository boardRepository,
      AuthRepository authRepository, UserPreferencesRepository prefsRepository) {
    return new BoardListViewModel(boardRepository, authRepository, prefsRepository);
  }
}
