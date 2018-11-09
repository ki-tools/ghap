package io.ghap.data.contribution;

import com.github.hburgmeier.jerseyoauth2.api.protocol.IRequestFactory;
import com.github.hburgmeier.jerseyoauth2.rs.api.DefaultConfiguration;
import com.github.hburgmeier.jerseyoauth2.rs.api.IRSConfiguration;
import com.github.hburgmeier.jerseyoauth2.rs.api.token.IAccessTokenVerifier;
import com.google.common.collect.ImmutableMap;
import com.sun.jersey.api.core.ResourceConfig;
import io.ghap.auth.CorsFilter;
import io.ghap.data.annotations.DefaultBootstrap;

import java.util.Map;

import com.google.common.collect.Maps;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import io.ghap.oauth.AccessTokenVerifier;
import io.ghap.oauth.CustomOauth20FilterFactory;
import io.ghap.oauth.GhapRequestFactory;

import static com.sun.jersey.api.core.PackagesResourceConfig.PROPERTY_PACKAGES;

@DefaultBootstrap
public class DataSubmissionBootstrap extends JerseyServletModule {

  @Override
  protected void configureServlets() {

    bind(IRSConfiguration.class).to(DefaultConfiguration.class);
    bind(IAccessTokenVerifier.class).to(AccessTokenVerifier.class);
    bind(IRequestFactory.class).to(GhapRequestFactory.class);



    // bind Jersey resources
    PackagesResourceConfig resourceConfig = new PackagesResourceConfig(
            ImmutableMap.of(PROPERTY_PACKAGES, "io.ghap;com.github.hburgmeier.jerseyoauth2")
    );
    for (Class<?> resource : resourceConfig.getClasses()) {
      bind(resource);
    }

    Map<String, String> jerseyParams = Maps.newHashMap();

    jerseyParams.put("com.sun.jersey.api.json.POJOMappingFeature", "true");
    jerseyParams.put(
            "com.sun.jersey.spi.container.ContainerResponseFilters",
            CorsFilter.class.getName()
    );
    jerseyParams.put(ResourceConfig.PROPERTY_RESOURCE_FILTER_FACTORIES, CustomOauth20FilterFactory.class.getName());

    serve("/rest/v1/*").with(GuiceContainer.class, jerseyParams);



    binder().bind(GuiceContainer.class).asEagerSingleton();
  }
}

