package com.app2.core.data.di;

import com.app2.core.data.remote.MovementApiService;
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
public final class NetworkModule_ProvideMovementApiFactory implements Factory<MovementApiService> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideMovementApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public MovementApiService get() {
    return provideMovementApi(retrofitProvider.get());
  }

  public static NetworkModule_ProvideMovementApiFactory create(
      Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideMovementApiFactory(retrofitProvider);
  }

  public static MovementApiService provideMovementApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideMovementApi(retrofit));
  }
}
