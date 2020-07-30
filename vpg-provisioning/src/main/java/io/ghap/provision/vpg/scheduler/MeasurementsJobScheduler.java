package io.ghap.provision.vpg.scheduler;

import org.quartz.JobKey;

public interface MeasurementsJobScheduler {
  void cancelMeasurementsUpdate(JobKey jobKey);
}
