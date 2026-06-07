package com.app2.core.data.di;

import com.app2.core.data.remote.InvoiceApiService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import retrofit2.Retrofit;

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
public final class NetworkModule_ProvideInvoiceApiFactory implements Factory<InvoiceApiService> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideInvoiceApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public InvoiceApiService get() {
    return provideInvoiceApi(retrofitProvider.get());
  }

  public static NetworkModule_ProvideInvoiceApiFactory create(Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideInvoiceApiFactory(retrofitProvider);
  }

  public static InvoiceApiService provideInvoiceApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideInvoiceApi(retrofit));
  }
}
