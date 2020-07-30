package io.ghap.web;

import com.github.hburgmeier.jerseyoauth2.api.protocol.IRequestFactory;
import com.github.hburgmeier.jerseyoauth2.protocol.impl.RequestFactory;
import com.github.hburgmeier.jerseyoauth2.rs.api.DefaultConfiguration;
import com.github.hburgmeier.jerseyoauth2.rs.api.IRSConfiguration;
import com.github.hburgmeier.jerseyoauth2.rs.api.token.IAccessTokenVerifier;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import io.ghap.filter.*;
import io.ghap.oauth.AccessTokenVerifier;
import io.ghap.service.ElasticSearchService;
import io.ghap.service.OauthService;
import io.ghap.service.PrepareRulesService;
import io.ghap.service.ProjectService;
import io.ghap.service.bean.ElasticSearchServiceBean;
import io.ghap.service.bean.OauthServiceBean;
import io.ghap.service.bean.PrepareRulesServiceBean;
import io.ghap.service.bean.ProjectServiceBean;
import io.ghap.web.annotations.DefaultBootstrap;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.tuckey.web.filters.urlrewrite.UrlRewriteFilter;

import java.util.Map;

import static com.sun.jersey.api.core.PackagesResourceConfig.PROPERTY_PACKAGES;

@DefaultBootstrap
public class ProjectManagementModule extends JerseyServletModule {

    public static Injector injector;

    @Inject
    public ProjectManagementModule(Injector inj) {
        injector = inj;
    }

    @Override
    protected void configureServlets() {
        SLF4JBridgeHandler.removeHandlersForRootLogger();  // (since SLF4J 1.6.5)

        // add SLF4JBridgeHandler to j.u.l's root logger, should be done once during
        // the initialization phase of the application
        SLF4JBridgeHandler.install();


        // excplictly bind GuiceContainer before binding Jersey resources
        // otherwise resource won't be available for GuiceContainer
        // when using two-phased injection
        bind(GuiceContainer.class);
        bind(IAccessTokenVerifier.class).to(AccessTokenVerifier.class);
        bind(PrepareRulesService.class).to(PrepareRulesServiceBean.class);
        bind(OauthService.class).to(OauthServiceBean.class);
        bind(ProjectService.class).to(ProjectServiceBean.class);
        bind(IRequestFactory.class).to(RequestFactory.class);
        bind(IRSConfiguration.class).to(DefaultConfiguration.class);
        bind(ElasticSearchService.class).to(ElasticSearchServiceBean.class);

        Map<String, Object> props = Maps.newHashMap();

        props.put(PROPERTY_PACKAGES, "io.ghap;com.github.hburgmeier.jerseyoauth2");

        // bind Jersey resources
        PackagesResourceConfig resourceConfig = new PackagesResourceConfig(props);
        for (Class<?> resource : resourceConfig.getClasses()) {
            bind(resource);
        }

        Map<String, String> jerseyParams = Maps.newHashMap();
        serve("/*").with(GuiceContainer.class, jerseyParams);
        filter("/*").through(_01LoggingFilter.class);
        filter("/*").through(_02CheckAppFilter.class);
        filter("/*").through(_03AuthenticationFilter.class);
        filter("/*").through(_04AuthorizationFilter.class);
        Map<String, String> urlRewriteParams = Maps.newHashMap();
        urlRewriteParams.put("confPath", _01LoggingFilter.getResourcePath());
        urlRewriteParams.put("logLevel", "SLF4J");
        filter("/*").through(_05UrlRewriteFilter.class, urlRewriteParams);
    }
}