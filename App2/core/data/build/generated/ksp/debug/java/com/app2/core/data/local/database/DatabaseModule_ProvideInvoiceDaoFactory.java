package com.app2.core.data.local.database;

import com.app2.core.data.local.dao.InvoiceDao;
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
public final class DatabaseModule_ProvideInvoiceDaoFactory implements Factory<InvoiceDao> {
  private final Provider<AppDatabase> dbProvider;

  public DatabaseModule_ProvideInvoiceDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public InvoiceDao get() {
    return provideInvoiceDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideInvoiceDaoFactory create(Provider<AppDatabase> dbProvider) {
    return new DatabaseModule_ProvideInvoiceDaoFactory(dbProvider);
  }

  public static InvoiceDao provideInvoiceDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideInvoiceDao(db));
  }
}
