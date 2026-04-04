package com.mountaincrab.crabdo.di;

import com.mountaincrab.crabdo.data.local.AppDatabase;
import com.mountaincrab.crabdo.data.local.dao.SubtaskDao;
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
public final class DatabaseModule_ProvideSubtaskDaoFactory implements Factory<SubtaskDao> {
  private final Provider<AppDatabase> dbProvider;

  public DatabaseModule_ProvideSubtaskDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public SubtaskDao get() {
    return provideSubtaskDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideSubtaskDaoFactory create(Provider<AppDatabase> dbProvider) {
    return new DatabaseModule_ProvideSubtaskDaoFactory(dbProvider);
  }

  public static SubtaskDao provideSubtaskDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideSubtaskDao(db));
  }
}
