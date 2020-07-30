package io.ghap.provision.vpg;

import com.github.hburgmeier.jerseyoauth2.api.protocol.IRequestFactory;
import com.github.hburgmeier.jerseyoauth2.rs.api.DefaultConfiguration;
import com.github.hburgmeier.jerseyoauth2.rs.api.IRSConfiguration;
import com.github.hburgmeier.jerseyoauth2.rs.api.token.IAccessTokenVerifier;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.sun.jersey.api.core.ResourceConfig;
import io.ghap.aws.guice.AWSApiHelperUtilitiesModule;
import io.ghap.oauth.AccessTokenVerifier;
import io.ghap.oauth.CustomOauth20FilterFactory;
import io.ghap.oauth.GhapRequestFactory;
import io.ghap.provision.annotations.DefaultBootstrap;
import io.ghap.provision.health.HealthCheckService;
import io.ghap.provision.health.HealthCheckServiceImpl;
import io.ghap.provision.vpg.auth.CorsFilter;
import io.ghap.provision.vpg.data.*;

import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

import com.google.common.collect.Maps;
import com.google.inject.persist.PersistFilter;
import com.google.inject.persist.jpa.JpaPersistModule;
import com.netflix.config.ConfigurationManager;
import com.netflix.governator.annotations.Configuration;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import io.ghap.provision.vpg.mailer.IdleResourceNotificationMailer;
import io.ghap.provision.vpg.scheduler.*;

import static com.sun.jersey.api.core.PackagesResourceConfig.PROPERTY_PACKAGES;

@DefaultBootstrap
public class VPGManagerBootstrap extends JerseyServletModule {

  @Configuration("javax.persistence.jdbc.url")
  private String jdbc_url;
  @Configuration("javax.persistence.jdbc.user")
  private String jdbc_user;
  @Configuration("javax.persistence.jdbc.password")
  private String jdbc_password;
  
  @Override
  protected void configureServlets() {   
    Properties props = getPersistenceProperties();
    install(new JpaPersistModule("vpg-provisioning").properties(props));

    install(new AWSApiHelperUtilitiesModule());

    install(new SchedulerModule());

    bind(VPGMeasurementsFactory.class).to(VPGMeasurementsFactoryImpl.class);
    bind(StackMeasurementsJobScheduler.class).to(StackMeasurementsJobSchedulerImpl.class);

    bind(VPGFactory.class).to(VPGFactoryImpl.class);
    bind(VPGMultiFactory.class).to(VPGMultiFactoryImpl.class);
    bind(VPGStateFactory.class).to(VPGStateFactoryImpl.class);

    bind(ProvisionedResourceJobScheduler.class).to(ProvisionedResourceJobSchedulerImpl.class);
    bind(PersonalStorageFactory.class).to(PersonalStorageFactoryImpl.class);
    bind(MonitoringResourceFactory.class).to(MonitoringResourceFactoryImpl.class);
    bind(IRSConfiguration.class).to(DefaultConfiguration.class);
    bind(IAccessTokenVerifier.class).to(AccessTokenVerifier.class);
    bind(IRequestFactory.class).to(GhapRequestFactory.class);
    bind(HealthCheckService.class).to(HealthCheckServiceImpl.class);

    bind(IdleResourceNotificationMailer.class);

    filter("/*").through(PersistFilter.class);

    // bind Jersey resources
    PackagesResourceConfig resourceConfig = new PackagesResourceConfig(ImmutableMap.of(PROPERTY_PACKAGES, (Object)"io.ghap;"));
    for (Class<?> resource : resourceConfig.getClasses()) {
      bind(resource);
    }

    Map<String, String> jerseyParams = Maps.newHashMap();
    jerseyParams.put(PackagesResourceConfig.PROPERTY_PACKAGES, "io.ghap;com.github.hburgmeier.jerseyoauth2");

    jerseyParams.put("com.sun.jersey.api.json.POJOMappingFeature", "true");
    jerseyParams.put(
            "com.sun.jersey.spi.container.ContainerResponseFilters",
            CorsFilter.class.getName()
    );
    jerseyParams.put(ResourceConfig.PROPERTY_RESOURCE_FILTER_FACTORIES, CustomOauth20FilterFactory.class.getName());
    
    serve("/rest/v1/*").with(GuiceContainer.class, jerseyParams);
    binder().bind(GuiceContainer.class).asEagerSingleton();;
  }

  private Properties getPersistenceProperties()
  {
    Properties properties = new Properties();
    properties.setProperty("javax.persistence.jdbc.url", ConfigurationManager.getConfigInstance().getString("javax.persistence.jdbc.url"));
    properties.setProperty("javax.persistence.jdbc.user", ConfigurationManager.getConfigInstance().getString("javax.persistence.jdbc.user"));
    properties.setProperty("javax.persistence.jdbc.password",ConfigurationManager.getConfigInstance().getString("javax.persistence.jdbc.password"));

    return properties;
  }

  @Provides
  @Named("defaultMessages")
  @Singleton
  public ResourceBundle provideMessagesBundle() {
    return ResourceBundle.getBundle("messages");
  }


}

