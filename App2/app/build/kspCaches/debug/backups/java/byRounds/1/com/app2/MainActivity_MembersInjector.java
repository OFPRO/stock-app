package com.app2;

import com.app2.feature.auth.PinManager;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class MainActivity_MembersInjector implements MembersInjector<MainActivity> {
  private final Provider<PinManager> pinManagerProvider;

  public MainActivity_MembersInjector(Provider<PinManager> pinManagerProvider) {
    this.pinManagerProvider = pinManagerProvider;
  }

  public static MembersInjector<MainActivity> create(Provider<PinManager> pinManagerProvider) {
    return new MainActivity_MembersInjector(pinManagerProvider);
  }

  @Override
  public void injectMembers(MainActivity instance) {
    injectPinManager(instance, pinManagerProvider.get());
  }

  @InjectedFieldSignature("com.app2.MainActivity.pinManager")
  public static void injectPinManager(MainActivity instance, PinManager pinManager) {
    instance.pinManager = pinManager;
  }
}
