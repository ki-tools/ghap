package io.ghap.provision.vpg;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import java.io.IOException;
import com.netflix.config.ConfigurationManager;
import com.netflix.governator.configuration.ArchaiusConfigurationProvider;
import com.netflix.governator.guice.BootstrapBinder;
import com.netflix.governator.guice.BootstrapModule;
import org.slf4j.LoggerFactory;

public class BasicBootstrap implements BootstrapModule
{

  static { //runs when the main class is loaded.
    System.setProperty("org.jboss.logging.provider", "slf4j");
  }

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
      initLogger(env);

      ConfigurationManager.loadCascadedPropertiesFromResources("application");
      S3Configuration.init(
              "config/provisioning-" + env + ".properties",
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

  private void initLogger(String env) {
    String file = "/logback-" + env + ".xml";

    // assume SLF4J is bound to logback in the current environment
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

    try {
      JoranConfigurator configurator = new JoranConfigurator();
      configurator.setContext(context);
      // Call context.reset() to clear any previous configuration, e.g. default
      // configuration. For multi-step configuration, omit calling context.reset().
      //context.reset();
      configurator.doConfigure(this.getClass().getResourceAsStream(file));
    } catch (JoranException je) {
      // StatusPrinter will handle this
    }
  }
}
