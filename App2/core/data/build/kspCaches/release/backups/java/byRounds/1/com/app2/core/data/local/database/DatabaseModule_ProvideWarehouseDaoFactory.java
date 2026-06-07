package com.app2.core.data.local.database;

import com.app2.core.data.local.dao.WarehouseDao;
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
public final class DatabaseModule_ProvideWarehouseDaoFactory implements Factory<WarehouseDao> {
  private final Provider<AppDatabase> dbProvider;

  public DatabaseModule_ProvideWarehouseDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public WarehouseDao get() {
    return provideWarehouseDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideWarehouseDaoFactory create(Provider<AppDatabase> dbProvider) {
    return new DatabaseModule_ProvideWarehouseDaoFactory(dbProvider);
  }

  public static WarehouseDao provideWarehouseDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideWarehouseDao(db));
  }
}
