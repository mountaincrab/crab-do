package com.mountaincrab.crabdo.di;

import com.mountaincrab.crabdo.data.local.AppDatabase;
import com.mountaincrab.crabdo.data.local.dao.BoardDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DatabaseModule_ProvideBoardDaoFactory implements Factory<BoardDao> {
  private final Provider<AppDatabase> dbProvider;

  public DatabaseModule_ProvideBoardDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public BoardDao get() {
    return provideBoardDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideBoardDaoFactory create(Provider<AppDatabase> dbProvider) {
    return new DatabaseModule_ProvideBoardDaoFactory(dbProvider);
  }

  public static BoardDao provideBoardDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideBoardDao(db));
  }
}
