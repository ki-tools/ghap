package io.ghap.activity;

import com.github.hburgmeier.jerseyoauth2.api.protocol.IRequestFactory;
import com.github.hburgmeier.jerseyoauth2.protocol.impl.RequestFactory;
import com.github.hburgmeier.jerseyoauth2.rs.api.DefaultConfiguration;
import com.github.hburgmeier.jerseyoauth2.rs.api.IRSConfiguration;
import com.github.hburgmeier.jerseyoauth2.rs.api.token.IAccessTokenVerifier;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.matcher.Matchers;
import com.google.inject.name.Named;
import com.google.inject.persist.jpa.JpaPersistModule;
import com.netflix.config.ConfigurationManager;
import com.netflix.governator.annotations.Configuration;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import io.ghap.activity.annotations.DefaultBootstrap;
import io.ghap.activity.auth.CorsFilter;
import io.ghap.activity.bannermanagement.dao.CommonPersistDao;
import io.ghap.activity.bannermanagement.dao.HealthCheckDao;
import io.ghap.activity.bannermanagement.dao.impl.DefaultCommonPersistDao;
import io.ghap.activity.bannermanagement.dao.impl.DefaultHealthCheckDao;
import io.ghap.activity.data.ActivityFactory;
import io.ghap.activity.data.ActivityFactoryImpl;
import io.ghap.activity.data.AssociationFactory;
import io.ghap.activity.data.AssociationFactoryImpl;
import io.ghap.activity.filter.LoggingFilter;
import io.ghap.activity.filter.PersistFilter;
import io.ghap.activity.jersey.ValidationInterceptor;
import io.ghap.oauth.AccessTokenVerifier;
import io.ghap.oauth.CustomOauth20FilterFactory;
import org.aopalliance.intercept.MethodInterceptor;

import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

import static com.sun.jersey.api.core.PackagesResourceConfig.PROPERTY_PACKAGES;

@DefaultBootstrap
public class ActivityManagerBootstrap extends JerseyServletModule {

  @Configuration("javax.persistence.jdbc.url")
  private String jdbc_url;
  @Configuration("javax.persistence.jdbc.user")
  private String jdbc_user;
  @Configuration("javax.persistence.jdbc.password")
  private String jdbc_password;
  
  @Override
  protected void configureServlets() {

    MethodInterceptor validationInterceptor = new ValidationInterceptor();
    requestInjection(validationInterceptor);
    bindInterceptor(Matchers.any(),
            ValidationInterceptor.isValidable(), validationInterceptor);

    Properties props = getPersistenceProperties();
    install(new JpaPersistModule("activity-manager").properties(props));
    bind(ActivityFactory.class).to(ActivityFactoryImpl.class);
    bind(AssociationFactory.class).to(AssociationFactoryImpl.class);
    bind(IRSConfiguration.class).to(DefaultConfiguration.class);
    bind(IAccessTokenVerifier.class).to(AccessTokenVerifier.class);
    bind(IRequestFactory.class).to(RequestFactory.class);
    bind(CommonPersistDao.class).to(DefaultCommonPersistDao.class);
    bind(HealthCheckDao.class).to(DefaultHealthCheckDao.class);
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

    filter("/*").through(LoggingFilter.class);
    serve("/rest/v1/*").with(GuiceContainer.class, jerseyParams);
    binder().bind(GuiceContainer.class).asEagerSingleton();
  }

  @Provides
  @Named("defaultMessages")
  @Singleton
  public ResourceBundle provideMessagesBundle() {
    return ResourceBundle.getBundle("messages");
  }

  @Provides
  @Named("defaultMessageCodes")
  @Singleton
  public ResourceBundle provideMessageCodesBundle() {
    return ResourceBundle.getBundle("messages", new Locale("codes"));
  }


  private Properties getPersistenceProperties()
  {
    Properties properties = new Properties();
    properties.setProperty("javax.persistence.jdbc.url", ConfigurationManager.getConfigInstance().getString("javax.persistence.jdbc.url"));
    properties.setProperty("javax.persistence.jdbc.user", ConfigurationManager.getConfigInstance().getString("javax.persistence.jdbc.user"));
    properties.setProperty("javax.persistence.jdbc.password",ConfigurationManager.getConfigInstance().getString("javax.persistence.jdbc.password"));

    return properties;
  }
}

