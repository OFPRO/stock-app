package com.app2.core.data.local.database;

import com.app2.core.data.local.dao.POSSessionDao;
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
public final class DatabaseModule_ProvidePOSSessionDaoFactory implements Factory<POSSessionDao> {
  private final Provider<AppDatabase> dbProvider;

  public DatabaseModule_ProvidePOSSessionDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public POSSessionDao get() {
    return providePOSSessionDao(dbProvider.get());
  }

  public static DatabaseModule_ProvidePOSSessionDaoFactory create(
      Provider<AppDatabase> dbProvider) {
    return new DatabaseModule_ProvidePOSSessionDaoFactory(dbProvider);
  }

  public static POSSessionDao providePOSSessionDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.providePOSSessionDao(db));
  }
}
