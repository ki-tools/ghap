package io.ghap.provision.vpg.scheduler;

public interface InstanceMeasurementsJobScheduler extends MeasurementsJobScheduler {
  JobInfo scheduleMeasurementsUpdate(String stackId, String instanceId);
  void cancelMeasurementsUpdate(String instanceId);
}