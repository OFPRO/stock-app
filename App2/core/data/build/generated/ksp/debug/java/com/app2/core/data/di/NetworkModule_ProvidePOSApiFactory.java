package com.app2.core.data.di;

import com.app2.core.data.remote.POSApiService;
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
public final class NetworkModule_ProvidePOSApiFactory implements Factory<POSApiService> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvidePOSApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public POSApiService get() {
    return providePOSApi(retrofitProvider.get());
  }

  public static NetworkModule_ProvidePOSApiFactory create(Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvidePOSApiFactory(retrofitProvider);
  }

  public static POSApiService providePOSApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.providePOSApi(retrofit));
  }
}
