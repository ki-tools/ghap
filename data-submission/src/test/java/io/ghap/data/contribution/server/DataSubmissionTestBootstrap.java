package io.ghap.data.contribution.server;

import static com.sun.jersey.api.core.PackagesResourceConfig.PROPERTY_PACKAGES;

import java.util.Map;

import com.google.common.collect.Maps;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;

/**
 *
 */
public class DataSubmissionTestBootstrap extends JerseyServletModule
{
  @Override
  protected void configureServlets()
  {
    // excplictly bind GuiceContainer before binding Jersey resources
    // otherwise resource won't be available for GuiceContainer
    // when using two-phased injection
    bind(GuiceContainer.class);

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
