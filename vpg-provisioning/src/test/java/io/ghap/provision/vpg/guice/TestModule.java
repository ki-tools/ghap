package io.ghap.provision.vpg.guice;

import com.google.inject.AbstractModule;
import io.ghap.aws.guice.AWSApiHelperUtilitiesModule;
import io.ghap.provision.vpg.data.*;
import io.ghap.provision.vpg.scheduler.ProvisionedResourceJobScheduler;
import io.ghap.provision.vpg.scheduler.ProvisionedResourceJobSchedulerImpl;
import io.ghap.provision.vpg.scheduler.SchedulerModule;
import io.ghap.provision.vpg.scheduler.StackMeasurementsJobScheduler;
import io.ghap.provision.vpg.scheduler.StackMeasurementsJobSchedulerImpl;

public class TestModule extends AbstractModule {
  public void configure() {
    bind(VPGFactory.class).to(VPGFactoryImpl.class);
    bind(VPGMultiFactory.class).to(VPGMultiFactoryImpl.class);
    bind(PersonalStorageFactory.class).to(PersonalStorageFactoryImpl.class);
    bind(VPGStateFactory.class).to(VPGStateFactoryImpl.class);
    bind(ProvisionedResourceJobScheduler.class).to(ProvisionedResourceJobSchedulerImpl.class);
    bind(StackMeasurementsJobScheduler.class).to(StackMeasurementsJobSchedulerImpl.class);
    bind(MonitoringResourceFactory.class).to(MonitoringResourceFactoryImpl.class);
    install(new SchedulerModule());
    install(new AWSApiHelperUtilitiesModule());
  }
}
