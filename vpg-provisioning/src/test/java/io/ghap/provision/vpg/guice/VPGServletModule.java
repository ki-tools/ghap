package io.ghap.provision.vpg.guice;

import com.google.common.collect.Maps;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.persist.PersistFilter;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import io.ghap.provision.annotations.DefaultBootstrap;
import io.ghap.provision.monitoring.lambda.MonitoringResource;
import io.ghap.provision.vpg.MultiVirtualPrivateGridResource;
import io.ghap.provision.vpg.PersonalStorageResource;
import io.ghap.provision.vpg.data.*;
import io.ghap.provision.vpg.mailer.IdleResourceNotificationMailer;
import io.ghap.provision.vpg.scheduler.ProvisionedResourceJobScheduler;
import io.ghap.provision.vpg.scheduler.ProvisionedResourceJobSchedulerImpl;
import io.ghap.provision.vpg.scheduler.SchedulerModule;

import java.util.Map;
import java.util.ResourceBundle;

@Singleton
@DefaultBootstrap
public class VPGServletModule extends JerseyServletModule {

  @Override
  protected void configureServlets()
  {
    bind(VPGMultiFactory.class).to(VPGMultiFactoryMockImpl.class);
    bind(PersonalStorageFactory.class).to(PersonalStorageFactoryMockImpl.class);
    bind(MultiVirtualPrivateGridResource.class);
    bind(PersonalStorageResource.class);

    bind(GuiceContainer.class);
    Map<String, String> params = Maps.newHashMap();
    params.put(PackagesResourceConfig.PROPERTY_PACKAGES, "io.ghap.provision.vpg");

    Map<String, String> jerseyParams = Maps.newHashMap();
    jerseyParams.put("com.sun.jersey.api.json.POJOMappingFeature", "true");

    serve("/rest/v1/*").with(GuiceContainer.class, jerseyParams);
  }


  @Provides
  @Named("defaultMessages")
  @Singleton
  public ResourceBundle provideMessagesBundle() {
    return ResourceBundle.getBundle("messages");
  }

}
