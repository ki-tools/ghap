package io.ghap.provision.vpg.scheduler;

public interface StackMeasurementsJobScheduler extends MeasurementsJobScheduler {
  JobInfo scheduleMeasurementsUpdate(String stackId);
  void cancelMeasurementsUpdate(String stackId);
}
