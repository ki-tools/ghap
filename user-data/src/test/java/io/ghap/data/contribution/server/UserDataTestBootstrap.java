package io.ghap.data.contribution.server;

import static com.sun.jersey.api.core.PackagesResourceConfig.PROPERTY_PACKAGES;

import java.util.Map;

import com.github.hburgmeier.jerseyoauth2.api.protocol.IRequestFactory;
import com.github.hburgmeier.jerseyoauth2.rs.api.DefaultConfiguration;
import com.github.hburgmeier.jerseyoauth2.rs.api.IRSConfiguration;
import com.github.hburgmeier.jerseyoauth2.rs.api.token.IAccessTokenVerifier;
import com.google.common.collect.Maps;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import io.ghap.oauth.AccessTokenVerifier;
import io.ghap.oauth.GhapRequestFactory;
import io.ghap.userdata.contribution.Storage;
import io.ghap.userdata.contribution.storage.local.LocalStorage;
import io.ghap.userdata.health.HealthCheckService;
import io.ghap.userdata.health.HealthCheckServiceImpl;
import io.ghap.userdata.logevents.EventLogger;

import io.ghap.userdata.logevents.TestEventlogger;

/**
 *
 */
public class UserDataTestBootstrap extends JerseyServletModule
{
  @Override
  protected void configureServlets()
  {
    // excplictly bind GuiceContainer before binding Jersey resources
    // otherwise resource won't be available for GuiceContainer
    // when using two-phased injection
    //bind(GuiceContainer.class);

    bind(IRSConfiguration.class).to(DefaultConfiguration.class);
    bind(IAccessTokenVerifier.class).to(AccessTokenVerifier.class);
    bind(IRequestFactory.class).to(GhapRequestFactory.class);
    bind(HealthCheckService.class).to(HealthCheckServiceImpl.class);
    bind(Storage.class).to(LocalStorage.class);
    bind(EventLogger.class).to(TestEventlogger.class);

    Map<String, Object> props = Maps.newHashMap();
    props.put(PROPERTY_PACKAGES, "io.ghap;");

    // bind Jersey resources
    PackagesResourceConfig resourceConfig = new PackagesResourceConfig(props);
    for (Class<?> resource : resourceConfig.getClasses())
    {
      bind(resource);
    }

    Map<String, String> jerseyParams = Maps.newHashMap();
    jerseyParams.put("com.sun.jersey.api.json.POJOMappingFeature", "true");
    
    serve("/rest/v1/*").with(GuiceContainer.class, jerseyParams);
  }
}
