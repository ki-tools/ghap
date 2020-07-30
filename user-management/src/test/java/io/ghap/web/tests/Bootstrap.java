package io.ghap.web.tests;

import com.netflix.config.ConfigurationManager;
import com.netflix.governator.configuration.ArchaiusConfigurationProvider;
import com.netflix.governator.guice.BootstrapBinder;
import com.netflix.governator.guice.BootstrapModule;

import java.io.IOException;


public class Bootstrap implements BootstrapModule {
    @Override
    public void configure(BootstrapBinder binder) {
        try {

            String env = "test";
            System.setProperty("archaius.deployment.environment", env);

            ConfigurationManager.getDeploymentContext().setDeploymentEnvironment(env);

            ConfigurationManager.loadCascadedPropertiesFromResources(env);

            binder.bindConfigurationProvider().to(ArchaiusConfigurationProvider.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
