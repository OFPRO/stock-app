package com.app2.core.data.local.database;

import com.app2.core.data.local.dao.SupplierDao;
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
public final class DatabaseModule_ProvideSupplierDaoFactory implements Factory<SupplierDao> {
  private final Provider<AppDatabase> dbProvider;

  public DatabaseModule_ProvideSupplierDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public SupplierDao get() {
    return provideSupplierDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideSupplierDaoFactory create(Provider<AppDatabase> dbProvider) {
    return new DatabaseModule_ProvideSupplierDaoFactory(dbProvider);
  }

  public static SupplierDao provideSupplierDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideSupplierDao(db));
  }
}
