package com.app2.core.data.di;

import com.app2.core.data.remote.ReportApiService;
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
public final class NetworkModule_ProvideReportApiFactory implements Factory<ReportApiService> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideReportApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public ReportApiService get() {
    return provideReportApi(retrofitProvider.get());
  }

  public static NetworkModule_ProvideReportApiFactory create(Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideReportApiFactory(retrofitProvider);
  }

  public static ReportApiService provideReportApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideReportApi(retrofit));
  }
}
