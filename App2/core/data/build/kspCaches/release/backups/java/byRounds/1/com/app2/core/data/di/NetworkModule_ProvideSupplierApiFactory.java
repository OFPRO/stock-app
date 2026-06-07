package com.app2.core.data.di;

import com.app2.core.data.remote.SupplierApiService;
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
public final class NetworkModule_ProvideSupplierApiFactory implements Factory<SupplierApiService> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideSupplierApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public SupplierApiService get() {
    return provideSupplierApi(retrofitProvider.get());
  }

  public static NetworkModule_ProvideSupplierApiFactory create(
      Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideSupplierApiFactory(retrofitProvider);
  }

  public static SupplierApiService provideSupplierApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideSupplierApi(retrofit));
  }
}
