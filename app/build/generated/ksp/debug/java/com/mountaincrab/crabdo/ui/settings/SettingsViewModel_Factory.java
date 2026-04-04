package com.mountaincrab.crabdo.ui.settings;

import android.content.Context;
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
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<BoardRepository> boardRepositoryProvider;

  private final Provider<UserPreferencesRepository> prefsRepositoryProvider;

  private final Provider<Context> contextProvider;

  public SettingsViewModel_Factory(Provider<AuthRepository> authRepositoryProvider,
      Provider<BoardRepository> boardRepositoryProvider,
      Provider<UserPreferencesRepository> prefsRepositoryProvider,
      Provider<Context> contextProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
    this.boardRepositoryProvider = boardRepositoryProvider;
    this.prefsRepositoryProvider = prefsRepositoryProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(authRepositoryProvider.get(), boardRepositoryProvider.get(), prefsRepositoryProvider.get(), contextProvider.get());
  }

  public static SettingsViewModel_Factory create(Provider<AuthRepository> authRepositoryProvider,
      Provider<BoardRepository> boardRepositoryProvider,
      Provider<UserPreferencesRepository> prefsRepositoryProvider,
      Provider<Context> contextProvider) {
    return new SettingsViewModel_Factory(authRepositoryProvider, boardRepositoryProvider, prefsRepositoryProvider, contextProvider);
  }

  public static SettingsViewModel newInstance(AuthRepository authRepository,
      BoardRepository boardRepository, UserPreferencesRepository prefsRepository, Context context) {
    return new SettingsViewModel(authRepository, boardRepository, prefsRepository, context);
  }
}
