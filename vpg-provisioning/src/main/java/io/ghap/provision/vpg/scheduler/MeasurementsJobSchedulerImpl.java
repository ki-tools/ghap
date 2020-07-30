package io.ghap.provision.vpg.scheduler;

import com.google.inject.Inject;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.slf4j.LoggerFactory;

import java.util.logging.Logger;

public abstract class MeasurementsJobSchedulerImpl implements MeasurementsJobScheduler {

  private org.slf4j.Logger logger = LoggerFactory.getLogger(MeasurementsJobSchedulerImpl.class);

  @Inject
  private Scheduler scheduler;

  @Override
  public void cancelMeasurementsUpdate(JobKey jobKey) {
    try {
      if (scheduler.checkExists(jobKey)) {
        scheduler.triggerJob(jobKey); //trigger the job now to get all measurements since the last update
        scheduler.deleteJob(jobKey); // stack is stopped, no need to update measurements stats
        logger.debug("Deleted measurement scheduler job {}", jobKey.getName());

      } else {
        logger.warn("Unable to find scheduled job for recording measurement updates");
      }
    } catch (SchedulerException e) {
      logger.error("Unable to cancel scheduled measurements/n{}", e);
    }
  }
}
