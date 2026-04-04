package com.mountaincrab.crabdo.di;

import androidx.work.WorkManager;
import com.mountaincrab.crabdo.data.local.dao.SubtaskDao;
import com.mountaincrab.crabdo.data.repository.SubtaskRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class RepositoryModule_ProvideSubtaskRepositoryFactory implements Factory<SubtaskRepository> {
  private final Provider<SubtaskDao> subtaskDaoProvider;

  private final Provider<WorkManager> workManagerProvider;

  public RepositoryModule_ProvideSubtaskRepositoryFactory(Provider<SubtaskDao> subtaskDaoProvider,
      Provider<WorkManager> workManagerProvider) {
    this.subtaskDaoProvider = subtaskDaoProvider;
    this.workManagerProvider = workManagerProvider;
  }

  @Override
  public SubtaskRepository get() {
    return provideSubtaskRepository(subtaskDaoProvider.get(), workManagerProvider.get());
  }

  public static RepositoryModule_ProvideSubtaskRepositoryFactory create(
      Provider<SubtaskDao> subtaskDaoProvider, Provider<WorkManager> workManagerProvider) {
    return new RepositoryModule_ProvideSubtaskRepositoryFactory(subtaskDaoProvider, workManagerProvider);
  }

  public static SubtaskRepository provideSubtaskRepository(SubtaskDao subtaskDao,
      WorkManager workManager) {
    return Preconditions.checkNotNullFromProvides(RepositoryModule.INSTANCE.provideSubtaskRepository(subtaskDao, workManager));
  }
}
