package io.ghap.provision.vpg.guice;

import com.netflix.config.ConfigurationManager;
import com.netflix.governator.configuration.ArchaiusConfigurationProvider;
import com.netflix.governator.guice.BootstrapBinder;
import com.netflix.governator.guice.BootstrapModule;

import java.io.IOException;

public class VPGServletBootstrapModule implements BootstrapModule {
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
  }
}
