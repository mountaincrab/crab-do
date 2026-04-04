package com.mountaincrab.crabdo.data.repository;

import androidx.work.WorkManager;
import com.mountaincrab.crabdo.data.local.dao.SubtaskDao;
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
public final class SubtaskRepository_Factory implements Factory<SubtaskRepository> {
  private final Provider<SubtaskDao> subtaskDaoProvider;

  private final Provider<WorkManager> workManagerProvider;

  public SubtaskRepository_Factory(Provider<SubtaskDao> subtaskDaoProvider,
      Provider<WorkManager> workManagerProvider) {
    this.subtaskDaoProvider = subtaskDaoProvider;
    this.workManagerProvider = workManagerProvider;
  }

  @Override
  public SubtaskRepository get() {
    return newInstance(subtaskDaoProvider.get(), workManagerProvider.get());
  }

  public static SubtaskRepository_Factory create(Provider<SubtaskDao> subtaskDaoProvider,
      Provider<WorkManager> workManagerProvider) {
    return new SubtaskRepository_Factory(subtaskDaoProvider, workManagerProvider);
  }

  public static SubtaskRepository newInstance(SubtaskDao subtaskDao, WorkManager workManager) {
    return new SubtaskRepository(subtaskDao, workManager);
  }
}
