package tests;

import com.google.common.collect.Maps;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.persist.PersistFilter;
import com.netflix.config.ConfigurationManager;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import io.ghap.banner.dao.CommonPersistDao;
import io.ghap.banner.dao.impl.*;
import io.ghap.banner.manager.DefaultBannerService;
import io.ghap.banner.manager.BannerService;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import static com.sun.jersey.api.core.PackagesResourceConfig.PROPERTY_PACKAGES;

/**
 *
 */
public class ProjectServiceModule extends JerseyServletModule {
    @Override

    protected void configureServlets() {
        ;

        // Optionally remove existing handlers attached to j.u.l root logger
        SLF4JBridgeHandler.removeHandlersForRootLogger();  // (since SLF4J 1.6.5)

        // add SLF4JBridgeHandler to j.u.l's root logger, should be done once during
        // the initialization phase of the application
        SLF4JBridgeHandler.install();


        // excplictly bind GuiceContainer before binding Jersey resources
        // otherwise resource won't be available for GuiceContainer
        // when using two-phased injection
        bind(GuiceContainer.class);

        if (ConfigurationManager.getConfigInstance().getBoolean("use_db")) {
            bind(CommonPersistDao.class).to(DefaultCommonPersistDao.class);
            filter("/*").through(PersistFilter.class);
            bind(PersistInitializer.class).asEagerSingleton();
        }
        bind(BannerService.class).to(DefaultBannerService.class);
        requestStaticInjection(BannerServiceModuleTest.class);


        Map<String, Object> props = Maps.newHashMap();
        props.put(PROPERTY_PACKAGES, "io.ghap;");

        Map<String, String> jerseyParams = Maps.newHashMap();
        jerseyParams.put("com.sun.jersey.api.json.POJOMappingFeature", "true");

        serve("*").with(GuiceContainer.class, jerseyParams);
        filter("/*").through(PersistFilter.class);
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
