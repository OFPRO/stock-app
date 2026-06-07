package com.app2.core.data.di;

import com.app2.core.data.remote.MainAccountApiService;
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
public final class NetworkModule_ProvideMainAccountApiFactory implements Factory<MainAccountApiService> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideMainAccountApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public MainAccountApiService get() {
    return provideMainAccountApi(retrofitProvider.get());
  }

  public static NetworkModule_ProvideMainAccountApiFactory create(
      Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideMainAccountApiFactory(retrofitProvider);
  }

  public static MainAccountApiService provideMainAccountApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideMainAccountApi(retrofit));
  }
}
