package com.app2.core.data.di;

import com.app2.core.data.remote.ReorderRuleApiService;
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
public final class NetworkModule_ProvideReorderRuleApiFactory implements Factory<ReorderRuleApiService> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideReorderRuleApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public ReorderRuleApiService get() {
    return provideReorderRuleApi(retrofitProvider.get());
  }

  public static NetworkModule_ProvideReorderRuleApiFactory create(
      Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideReorderRuleApiFactory(retrofitProvider);
  }

  public static ReorderRuleApiService provideReorderRuleApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideReorderRuleApi(retrofit));
  }
}
