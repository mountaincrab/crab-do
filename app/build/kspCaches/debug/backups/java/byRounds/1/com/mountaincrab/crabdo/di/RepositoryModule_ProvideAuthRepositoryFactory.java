package com.mountaincrab.crabdo.di;

import com.google.firebase.auth.FirebaseAuth;
import com.mountaincrab.crabdo.auth.AuthRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class RepositoryModule_ProvideAuthRepositoryFactory implements Factory<AuthRepository> {
  private final Provider<FirebaseAuth> authProvider;

  public RepositoryModule_ProvideAuthRepositoryFactory(Provider<FirebaseAuth> authProvider) {
    this.authProvider = authProvider;
  }

  @Override
  public AuthRepository get() {
    return provideAuthRepository(authProvider.get());
  }

  public static RepositoryModule_ProvideAuthRepositoryFactory create(
      Provider<FirebaseAuth> authProvider) {
    return new RepositoryModule_ProvideAuthRepositoryFactory(authProvider);
  }

  public static AuthRepository provideAuthRepository(FirebaseAuth auth) {
    return Preconditions.checkNotNullFromProvides(RepositoryModule.INSTANCE.provideAuthRepository(auth));
  }
}
