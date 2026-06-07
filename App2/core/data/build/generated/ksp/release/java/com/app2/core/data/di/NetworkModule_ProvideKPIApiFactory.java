package com.app2.core.data.di;

import com.app2.core.data.remote.KPIApiService;
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
public final class NetworkModule_ProvideKPIApiFactory implements Factory<KPIApiService> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideKPIApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public KPIApiService get() {
    return provideKPIApi(retrofitProvider.get());
  }

  public static NetworkModule_ProvideKPIApiFactory create(Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideKPIApiFactory(retrofitProvider);
  }

  public static KPIApiService provideKPIApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideKPIApi(retrofit));
  }
}
