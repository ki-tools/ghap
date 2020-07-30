package io.ghap.web.tests;

import com.google.common.collect.Maps;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.throwingproviders.ThrowingProviderBinder;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import io.ghap.auth.LdapConfiguration;
import io.ghap.auth.SecurityFilter;
import io.ghap.job.SchedulerModule;
import io.ghap.ldap.LdapConnectionProvider;
import io.ghap.mailer.MailConfiguration;
import io.ghap.user.manager.GroupService;
import io.ghap.user.manager.UserService;
import io.ghap.user.manager.impl.DefaultGroupService;
import io.ghap.user.manager.impl.DefaultUserService;
import io.ghap.user.model.validation.*;
import io.ghap.user.model.validation.validators.PasswordLengthValidator;
import io.ghap.validation.ValidationModule;
import io.ghap.web.tests.ldap.TestLdapConnectionProvider;
import io.ghap.web.tests.validation.TestPasswordLengthValidator;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.util.Map;
import java.util.ResourceBundle;

import static com.sun.jersey.api.core.PackagesResourceConfig.PROPERTY_PACKAGES;

/**
 *
 */
public class UserManagementModule extends JerseyServletModule {
    @Override
    protected void configureServlets() {;

        // Optionally remove existing handlers attached to j.u.l root logger
        SLF4JBridgeHandler.removeHandlersForRootLogger();  // (since SLF4J 1.6.5)

        // add SLF4JBridgeHandler to j.u.l's root logger, should be done once during
        // the initialization phase of the application
        SLF4JBridgeHandler.install();

        // excplictly bind GuiceContainer before binding Jersey resources
        // otherwise resource won't be available for GuiceContainer
        // when using two-phased injection
        bind(GuiceContainer.class);
        bind(LdapConfiguration.class);
        bind(MailConfiguration.class);

        // for validation
        bind(OnCreate.class).to(OnCreateImpl.class);
        bind(OnUpdate.class).to(OnUpdateImpl.class);
        bind(OnResetPassword.class).to(OnResetPasswordImpl.class);
        bind(PasswordLengthValidator.class).to(TestPasswordLengthValidator.class);

        install(new SchedulerModule());
        install(new ValidationModule());

        ThrowingProviderBinder.create(binder())
                .bind(LdapConnectionProvider.class, LdapConnection.class)
                .to(TestLdapConnectionProvider.class);


        bind(UserService.class).to(DefaultUserService.class);
        bind(GroupService.class).to(DefaultGroupService.class);


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

        serve("*").with(GuiceContainer.class, jerseyParams);
    }

    @Provides
    @Named("defaultMessages")
    @Singleton
    public ResourceBundle provideMessagesBundle() {
        return ResourceBundle.getBundle("messages");
    }
}
