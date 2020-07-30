package io.ghap.provision.vpg.guice;

import com.google.inject.matcher.Matchers;
import com.netflix.config.ConfigurationManager;
import com.netflix.governator.LifecycleModule;
import com.netflix.governator.configuration.ArchaiusConfigurationProvider;
import com.netflix.governator.guice.BootstrapBinder;
import com.netflix.governator.guice.BootstrapModule;
import io.ghap.test.MethodModules;
import io.ghap.test.MethodModulesInterceptor;
import io.ghap.test.database.SetupTestDatabase;
import io.ghap.test.database.SetupTestDatabaseInterceptor;

import java.io.IOException;

public class VPGBootstrapModule implements BootstrapModule {
  @Override
  public void configure(BootstrapBinder binder) {
    try {
      System.setProperty("archaius.deployment.environment", "test");
      ConfigurationManager.loadCascadedPropertiesFromResources("application");
      binder.bindConfigurationProvider().to(ArchaiusConfigurationProvider.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    System.setProperty("aws.accessKeyId", ConfigurationManager.getConfigInstance().getString("db.aws.accessKeyId"));
    System.setProperty("aws.secretKey", ConfigurationManager.getConfigInstance().getString("db.aws.secretKey"));

    binder.install(new LifecycleModule());
    binder.install(new VPGModule());
    binder.bindInterceptor(Matchers.any(), Matchers.annotatedWith(SetupTestDatabase.class), new SetupTestDatabaseInterceptor());
    binder.bindInterceptor(Matchers.any(), Matchers.annotatedWith(MethodModules.class), new MethodModulesInterceptor());
  }
}
