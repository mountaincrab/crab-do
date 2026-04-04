package com.mountaincrab.crabdo.di;

import com.mountaincrab.crabdo.data.local.AppDatabase;
import com.mountaincrab.crabdo.data.local.dao.ColumnDao;
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
public final class DatabaseModule_ProvideColumnDaoFactory implements Factory<ColumnDao> {
  private final Provider<AppDatabase> dbProvider;

  public DatabaseModule_ProvideColumnDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public ColumnDao get() {
    return provideColumnDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideColumnDaoFactory create(Provider<AppDatabase> dbProvider) {
    return new DatabaseModule_ProvideColumnDaoFactory(dbProvider);
  }

  public static ColumnDao provideColumnDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideColumnDao(db));
  }
}
