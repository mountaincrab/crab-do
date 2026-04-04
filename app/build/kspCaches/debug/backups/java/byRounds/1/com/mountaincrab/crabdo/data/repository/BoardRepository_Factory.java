package com.mountaincrab.crabdo.data.repository;

import androidx.work.WorkManager;
import com.mountaincrab.crabdo.data.local.dao.BoardDao;
import com.mountaincrab.crabdo.data.local.dao.ColumnDao;
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
public final class BoardRepository_Factory implements Factory<BoardRepository> {
  private final Provider<BoardDao> boardDaoProvider;

  private final Provider<ColumnDao> columnDaoProvider;

  private final Provider<WorkManager> workManagerProvider;

  public BoardRepository_Factory(Provider<BoardDao> boardDaoProvider,
      Provider<ColumnDao> columnDaoProvider, Provider<WorkManager> workManagerProvider) {
    this.boardDaoProvider = boardDaoProvider;
    this.columnDaoProvider = columnDaoProvider;
    this.workManagerProvider = workManagerProvider;
  }

  @Override
  public BoardRepository get() {
    return newInstance(boardDaoProvider.get(), columnDaoProvider.get(), workManagerProvider.get());
  }

  public static BoardRepository_Factory create(Provider<BoardDao> boardDaoProvider,
      Provider<ColumnDao> columnDaoProvider, Provider<WorkManager> workManagerProvider) {
    return new BoardRepository_Factory(boardDaoProvider, columnDaoProvider, workManagerProvider);
  }

  public static BoardRepository newInstance(BoardDao boardDao, ColumnDao columnDao,
      WorkManager workManager) {
    return new BoardRepository(boardDao, columnDao, workManager);
  }
}
