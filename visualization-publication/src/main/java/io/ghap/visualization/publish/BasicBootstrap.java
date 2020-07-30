package io.ghap.visualization.publish;

import com.netflix.config.ConfigurationManager;
import com.netflix.governator.configuration.ArchaiusConfigurationProvider;
import com.netflix.governator.guice.BootstrapBinder;
import com.netflix.governator.guice.BootstrapModule;

import java.io.IOException;

public class BasicBootstrap implements BootstrapModule
{
  @Override
  public void configure(BootstrapBinder binder)
  {
    try
    {
      String env = System.getProperty("archaius.deployment.environment");
      if( env == null) {
        System.setProperty("archaius.deployment.environment", env = "dev");
      }

      ConfigurationManager.getDeploymentContext().setDeploymentEnvironment(env);

      // For some reason(bug) cascaded properties doesn't work with "config" file name...
      ConfigurationManager.loadCascadedPropertiesFromResources("application");
      S3Configuration.init(
              "config/visualization-publication-" + env + ".properties",
              "config/application-" + env + ".properties"
      );

      // init configuration provider
      binder.bindConfigurationProvider().to(ArchaiusConfigurationProvider.class);
    } 
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }
}
