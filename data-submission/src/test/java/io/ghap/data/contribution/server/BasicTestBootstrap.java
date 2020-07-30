package io.ghap.data.contribution.server;

import java.io.IOException;

import com.netflix.config.ConfigurationManager;
import com.netflix.governator.configuration.ArchaiusConfigurationProvider;
import com.netflix.governator.guice.BootstrapBinder;
import com.netflix.governator.guice.BootstrapModule;


public class BasicTestBootstrap implements BootstrapModule {
    @Override
    public void configure(BootstrapBinder binder) {
        try {

            System.setProperty("archaius.deployment.environment", "test");

            ConfigurationManager.loadCascadedPropertiesFromResources("application");

            binder.bindConfigurationProvider().to(ArchaiusConfigurationProvider.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
