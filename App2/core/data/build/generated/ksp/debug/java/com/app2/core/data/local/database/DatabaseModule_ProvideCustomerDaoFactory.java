package com.app2.core.data.local.database;

import com.app2.core.data.local.dao.CustomerDao;
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
public final class DatabaseModule_ProvideCustomerDaoFactory implements Factory<CustomerDao> {
  private final Provider<AppDatabase> dbProvider;

  public DatabaseModule_ProvideCustomerDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public CustomerDao get() {
    return provideCustomerDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideCustomerDaoFactory create(Provider<AppDatabase> dbProvider) {
    return new DatabaseModule_ProvideCustomerDaoFactory(dbProvider);
  }

  public static CustomerDao provideCustomerDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideCustomerDao(db));
  }
}
