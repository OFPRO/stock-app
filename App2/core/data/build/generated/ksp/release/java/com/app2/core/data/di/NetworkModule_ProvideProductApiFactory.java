package com.app2.core.data.di;

import com.app2.core.data.remote.ProductApiService;
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
public final class NetworkModule_ProvideProductApiFactory implements Factory<ProductApiService> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideProductApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public ProductApiService get() {
    return provideProductApi(retrofitProvider.get());
  }

  public static NetworkModule_ProvideProductApiFactory create(Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideProductApiFactory(retrofitProvider);
  }

  public static ProductApiService provideProductApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideProductApi(retrofit));
  }
}
