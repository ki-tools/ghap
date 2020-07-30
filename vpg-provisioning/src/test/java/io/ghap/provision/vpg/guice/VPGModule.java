package io.ghap.provision.vpg.guice;

import com.google.inject.AbstractModule;
import io.ghap.test.infrastructure.Infrastructure;
import io.ghap.test.infrastructure.InfrastructureImpl;

public class VPGModule extends AbstractModule {

  public void configure() {
    bind(Infrastructure.class).to(InfrastructureImpl.class);
  }
}
