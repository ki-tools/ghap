package io.ghap.web;

import com.github.hburgmeier.jerseyoauth2.api.protocol.IRequestFactory;
import com.github.hburgmeier.jerseyoauth2.protocol.impl.RequestFactory;
import com.github.hburgmeier.jerseyoauth2.rs.api.DefaultConfiguration;
import com.github.hburgmeier.jerseyoauth2.rs.api.IRSConfiguration;
import com.github.hburgmeier.jerseyoauth2.rs.api.token.IAccessTokenVerifier;
import com.google.common.collect.Maps;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.matcher.Matchers;
import com.google.inject.name.Named;
import com.netflix.config.ConfigurationManager;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import io.ghap.jersey.ValidationInterceptor;
import io.ghap.oauth.AccessTokenVerifier;
import io.ghap.oauth.CustomOauth20FilterFactory;
import io.ghap.auth.CorsFilter;
import io.ghap.project.dao.*;
import io.ghap.project.dao.impl.*;
import io.ghap.project.domain.PersistFilter;
import io.ghap.project.filter.LoggingFilter;
import io.ghap.web.annotations.DefaultBootstrap;
import org.aopalliance.intercept.MethodInterceptor;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import static com.sun.jersey.api.core.PackagesResourceConfig.PROPERTY_PACKAGES;

@DefaultBootstrap
public class ProjectManagementModule extends JerseyServletModule {

    @Override
    protected void configureServlets() {

        MethodInterceptor validationInterceptor = new ValidationInterceptor();
        requestInjection(validationInterceptor);
        bindInterceptor(Matchers.any(),
                ValidationInterceptor.isValidable(), validationInterceptor);

        SLF4JBridgeHandler.removeHandlersForRootLogger();  // (since SLF4J 1.6.5)

        // add SLF4JBridgeHandler to j.u.l's root logger, should be done once during
        // the initialization phase of the application
        SLF4JBridgeHandler.install();


        // excplictly bind GuiceContainer before binding Jersey resources
        // otherwise resource won't be available for GuiceContainer
        // when using two-phased injection
        bind(GuiceContainer.class);

        if (ConfigurationManager.getConfigInstance().getBoolean("use_db")) {
            bind(StashProjectDao.class).to(DefaultStashProjectDao.class);
            bind(CommonPersistDao.class).to(DefaultCommonPersistDao.class);
            filter("/*").through(PersistFilter.class);
        }
        bind(UserManagementDao.class).to(DefaultUserManagementDao.class);
        bind(IRSConfiguration.class).to(DefaultConfiguration.class);
        bind(IAccessTokenVerifier.class).to(AccessTokenVerifier.class);
        bind(IRequestFactory.class).to(RequestFactory.class);
        bind(HealthCheckDao.class).to(DefaultHealthCheckDao.class);
        bind(CleanupDao.class).to(DefaultCleanupDao.class);

        Map<String, Object> props = Maps.newHashMap();

        props.put(PROPERTY_PACKAGES, "io.ghap;com.github.hburgmeier.jerseyoauth2");

        // bind Jersey resources
        PackagesResourceConfig resourceConfig = new PackagesResourceConfig(props);
        for (Class<?> resource : resourceConfig.getClasses()) {
            bind(resource);
        }

        Map<String, String> jerseyParams = Maps.newHashMap();
        jerseyParams.put("com.sun.jersey.api.json.POJOMappingFeature", "true");
        jerseyParams.put(
                "com.sun.jersey.spi.container.ContainerRequestFilters",
                "com.sun.jersey.api.container.filter.LoggingFilter;");
        jerseyParams.put(
                "com.sun.jersey.spi.container.ContainerResponseFilters",
                CorsFilter.class.getName());
        jerseyParams.put(ResourceConfig.PROPERTY_RESOURCE_FILTER_FACTORIES, CustomOauth20FilterFactory.class.getName());

        filter("/*").through(LoggingFilter.class);
        serve("/rest/v1/*").with(GuiceContainer.class, jerseyParams);
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
}