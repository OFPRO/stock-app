package com.app2.core.data.di;

import com.app2.core.data.remote.WarehouseApiService;
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
public final class NetworkModule_ProvideWarehouseApiFactory implements Factory<WarehouseApiService> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideWarehouseApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public WarehouseApiService get() {
    return provideWarehouseApi(retrofitProvider.get());
  }

  public static NetworkModule_ProvideWarehouseApiFactory create(
      Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideWarehouseApiFactory(retrofitProvider);
  }

  public static WarehouseApiService provideWarehouseApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideWarehouseApi(retrofit));
  }
}
