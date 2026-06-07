package com.app2.core.data.di;

import com.app2.core.data.remote.LocationApiService;
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
public final class NetworkModule_ProvideLocationApiFactory implements Factory<LocationApiService> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideLocationApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public LocationApiService get() {
    return provideLocationApi(retrofitProvider.get());
  }

  public static NetworkModule_ProvideLocationApiFactory create(
      Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideLocationApiFactory(retrofitProvider);
  }

  public static LocationApiService provideLocationApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideLocationApi(retrofit));
  }
}
