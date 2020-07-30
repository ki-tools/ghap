package io.ghap.web;

import com.google.inject.persist.jpa.JpaPersistModule;
import com.netflix.config.ConfigurationManager;
import com.netflix.governator.configuration.ArchaiusConfigurationProvider;
import com.netflix.governator.guice.BootstrapBinder;
import com.netflix.governator.guice.BootstrapModule;

import java.io.IOException;
import java.util.Properties;

/**
 *
 */
public class Bootstrap implements BootstrapModule {
    @Override
    public void configure(BootstrapBinder binder) {
        try {

            /*
            System.setProperty( "extra.controls",
                    "org.apache.directory.api.ldap.extras.controls.ppolicy_impl.PasswordPolicyFactory"
            );
            */

            String env = System.getProperty("archaius.deployment.environment");
            if( env == null) {
                System.setProperty("archaius.deployment.environment", env = "dev");
            }

            ConfigurationManager.getDeploymentContext().setDeploymentEnvironment(env);

            // For some reason(bug) cascaded properties doesn't work with "config" file name...
            ConfigurationManager.loadCascadedPropertiesFromResources("application");
            S3Configuration.init(
                    "config/banner-" + env + ".properties",
                    "config/application-" + env + ".properties"
            );

            // init configuration provider
            binder.bindConfigurationProvider().to(ArchaiusConfigurationProvider.class);
            if (ConfigurationManager.getConfigInstance().getBoolean("use_db")){
                binder.install(new JpaPersistModule("banner-manager").properties(getPropertiesForDataSource()));
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Properties getPropertiesForDataSource() {
        Properties properties=new Properties();
        properties.setProperty("javax.persistence.jdbc.url",ConfigurationManager.getConfigInstance().getString("javax.persistence.jdbc.url"));
        properties.setProperty("javax.persistence.jdbc.user",ConfigurationManager.getConfigInstance().getString("javax.persistence.jdbc.user"));
        properties.setProperty("javax.persistence.jdbc.password",ConfigurationManager.getConfigInstance().getString("javax.persistence.jdbc.password"));

        return properties;
    }
}
