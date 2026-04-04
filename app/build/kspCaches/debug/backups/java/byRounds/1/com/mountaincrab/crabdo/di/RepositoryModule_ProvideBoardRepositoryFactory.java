package com.mountaincrab.crabdo.di;

import androidx.work.WorkManager;
import com.mountaincrab.crabdo.data.local.dao.BoardDao;
import com.mountaincrab.crabdo.data.local.dao.ColumnDao;
import com.mountaincrab.crabdo.data.repository.BoardRepository;
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
public final class RepositoryModule_ProvideBoardRepositoryFactory implements Factory<BoardRepository> {
  private final Provider<BoardDao> boardDaoProvider;

  private final Provider<ColumnDao> columnDaoProvider;

  private final Provider<WorkManager> workManagerProvider;

  public RepositoryModule_ProvideBoardRepositoryFactory(Provider<BoardDao> boardDaoProvider,
      Provider<ColumnDao> columnDaoProvider, Provider<WorkManager> workManagerProvider) {
    this.boardDaoProvider = boardDaoProvider;
    this.columnDaoProvider = columnDaoProvider;
    this.workManagerProvider = workManagerProvider;
  }

  @Override
  public BoardRepository get() {
    return provideBoardRepository(boardDaoProvider.get(), columnDaoProvider.get(), workManagerProvider.get());
  }

  public static RepositoryModule_ProvideBoardRepositoryFactory create(
      Provider<BoardDao> boardDaoProvider, Provider<ColumnDao> columnDaoProvider,
      Provider<WorkManager> workManagerProvider) {
    return new RepositoryModule_ProvideBoardRepositoryFactory(boardDaoProvider, columnDaoProvider, workManagerProvider);
  }

  public static BoardRepository provideBoardRepository(BoardDao boardDao, ColumnDao columnDao,
      WorkManager workManager) {
    return Preconditions.checkNotNullFromProvides(RepositoryModule.INSTANCE.provideBoardRepository(boardDao, columnDao, workManager));
  }
}
