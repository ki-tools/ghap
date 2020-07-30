package io.ghap.web;

import com.google.common.collect.Maps;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.throwingproviders.ThrowingProviderBinder;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import io.ghap.auth.CorsFilter;
import io.ghap.auth.LdapConfiguration;
import io.ghap.auth.OauthCient;
import io.ghap.auth.SecurityFilter;
import io.ghap.job.SchedulerModule;
import io.ghap.ldap.DefaultLdapConnectionProvider;
import io.ghap.ldap.LdapConnectionFactory;
import io.ghap.ldap.LdapConnectionProvider;
import io.ghap.mailer.*;
import io.ghap.user.dao.DomainDao;
import io.ghap.user.dao.GroupDao;
import io.ghap.user.dao.PasswordGenerator;
import io.ghap.user.dao.UserDao;
import io.ghap.user.dao.UserStorageDao;
import io.ghap.user.dao.impl.DefaultDomainDao;
import io.ghap.user.dao.impl.DefaultGroupDao;
import io.ghap.user.dao.impl.DefaultUserDao;
import io.ghap.user.dao.impl.DefaultUserStorageDao;
import io.ghap.user.model.validation.*;
import io.ghap.user.model.validation.validators.LdapPasswordLengthValidator;
import io.ghap.user.model.validation.validators.PasswordLengthValidator;
import io.ghap.user.model.validation.validators.PasswordPatternListValidator;
import io.ghap.user.model.validation.validators.PasswordPatternValidator;
import io.ghap.validation.DefaultConstraintValidatorFactory;
import io.ghap.validation.ValidationModule;
import io.ghap.web.annotations.DefaultBootstrap;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.util.Map;
import java.util.ResourceBundle;

import static com.sun.jersey.api.core.PackagesResourceConfig.PROPERTY_PACKAGES;

@DefaultBootstrap
public class UserManagementModule extends JerseyServletModule {

    @Override
    protected void configureServlets() {

        // TODO check real performance for jlu-to-slf4g bridge

        // Optionally remove existing handlers attached to j.u.l root logger
        SLF4JBridgeHandler.removeHandlersForRootLogger();  // (since SLF4J 1.6.5)

        // add SLF4JBridgeHandler to j.u.l's root logger, should be done once during
        // the initialization phase of the application
        SLF4JBridgeHandler.install();

        // excplictly bind GuiceContainer before binding Jersey resources
        // otherwise resource won't be available for GuiceContainer
        // when using two-phased injection
        bind(GuiceContainer.class).asEagerSingleton();

        // for validation
        bind(LdapConfiguration.class);
        bind(LdapConnectionFactory.class);
        bind(PasswordGenerator.class);

        bind(DefaultConstraintValidatorFactory.class);

        bind(PasswordLengthValidator.class).to(LdapPasswordLengthValidator.class);
        bind(PasswordPatternListValidator.class);
        bind(PasswordPatternValidator.class);

        bind(OnCreate.class).to(OnCreateImpl.class);
        bind(OnUpdate.class).to(OnUpdateImpl.class);
        bind(OnResetPassword.class).to(OnResetPasswordImpl.class);


        bind(MailConfiguration.class);

        install(new SchedulerModule());
        install(new ValidationModule());


        bind(UserDao.class).to(DefaultUserDao.class);
        bind(GroupDao.class).to(DefaultGroupDao.class);

        bind(UserStorageDao.class).to(DefaultUserStorageDao.class);

        bind(NewUserMailer.class);
        bind(PasswordExpirationMailer.class);
        bind(PasswordUpdatedMailer.class);
        bind(PasswordResetMailer.class);
        bind(UpdateUserMailer.class);

        // see: https://github.com/google/guice/wiki/ThrowingProviders
        ThrowingProviderBinder.create(binder())
                .bind(LdapConnectionProvider.class, LdapConnection.class)
                .to(DefaultLdapConnectionProvider.class);

        bind(DomainDao.class).to(DefaultDomainDao.class);

        bind(OauthCient.class);

        Map<String, Object> props = Maps.newHashMap();
        props.put(PROPERTY_PACKAGES, "io.ghap;");

        // bind Jersey resources
        PackagesResourceConfig resourceConfig = new PackagesResourceConfig(props);
        for (Class<?> resource : resourceConfig.getClasses()) {
            bind(resource);
        }

        Map<String, String> jerseyParams = Maps.newHashMap();
        jerseyParams.put("com.sun.jersey.api.json.POJOMappingFeature", "true");
        jerseyParams.put(
                "com.sun.jersey.spi.container.ContainerRequestFilters",
                "com.sun.jersey.api.container.filter.LoggingFilter;" + SecurityFilter.class.getName()
        );
        jerseyParams.put(
                "com.sun.jersey.spi.container.ContainerResponseFilters",
                CorsFilter.class.getName()
        );

        serve("/*").with(GuiceContainer.class, jerseyParams);
    }

    @Provides
    @Named("defaultMessages")
    @Singleton
    public ResourceBundle provideMessagesBundle() {
        return ResourceBundle.getBundle("messages");
    }


}