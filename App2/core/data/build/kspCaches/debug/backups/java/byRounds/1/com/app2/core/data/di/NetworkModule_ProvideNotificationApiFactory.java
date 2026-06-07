package com.app2.core.data.di;

import com.app2.core.data.remote.NotificationApiService;
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
public final class NetworkModule_ProvideNotificationApiFactory implements Factory<NotificationApiService> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideNotificationApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public NotificationApiService get() {
    return provideNotificationApi(retrofitProvider.get());
  }

  public static NetworkModule_ProvideNotificationApiFactory create(
      Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideNotificationApiFactory(retrofitProvider);
  }

  public static NotificationApiService provideNotificationApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideNotificationApi(retrofit));
  }
}
