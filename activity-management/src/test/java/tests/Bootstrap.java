package tests;

import com.google.inject.persist.jpa.JpaPersistModule;
import com.netflix.config.ConfigurationManager;
import com.netflix.governator.configuration.ArchaiusConfigurationProvider;
import com.netflix.governator.guice.BootstrapBinder;
import com.netflix.governator.guice.BootstrapModule;

import java.io.IOException;
import java.util.Properties;


public class Bootstrap implements BootstrapModule {
    @Override
    public void configure(BootstrapBinder binder) {
        try {
             ConfigurationManager
                    .loadCascadedPropertiesFromResources("test");
            binder.bindConfigurationProvider().to(ArchaiusConfigurationProvider.class);
            binder.install(new JpaPersistModule("test-activity").properties(getPropertiesForDataSource()));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Properties getPropertiesForDataSource() {
        Properties properties = new Properties();
        properties.setProperty("javax.persistence.jdbc.url", ConfigurationManager.getConfigInstance().getString("javax.persistence.jdbc.url"));
        properties.setProperty("javax.persistence.jdbc.user", ConfigurationManager.getConfigInstance().getString("javax.persistence.jdbc.user"));
        properties.setProperty("javax.persistence.jdbc.password",ConfigurationManager.getConfigInstance().getString("javax.persistence.jdbc.password"));

        return properties;
    }
}
