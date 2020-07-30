package io.ghap.provision.vpg.scheduler;

import com.google.inject.Inject;
import com.netflix.governator.annotations.Configuration;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
   * Quartz job scheduler to poll Amazon CloudWatch for performance related stats and store stats in our local history DB.
   * @author aanischenko
   */
public class InstanceMeasurementsJobSchedulerImpl extends MeasurementsJobSchedulerImpl implements InstanceMeasurementsJobScheduler {

    private Logger logger = LoggerFactory.getLogger(InstanceMeasurementsJobSchedulerImpl.class);

    @Inject
    private Scheduler scheduler;

    @Configuration("measurements.update.interval.seconds")
    private String updateIntervalSeconds;

    private static final int DEFAULT_UPDATE_INTERVAL_SECONDS = 24*60*60; // one day in seconds

    @Override
    public JobInfo scheduleMeasurementsUpdate(String stackId, String instanceId) {
      logger.debug("Scheduling measurements update for instance: {}", instanceId);
      JobKey jobKey = JobKey.jobKey(instanceId, "MeasurementsUpdateJob");
      try {
        if (!scheduler.checkExists(jobKey)) //only schedule the job if one does not already exist for the stack.
        {
          int interval = DEFAULT_UPDATE_INTERVAL_SECONDS;
          try {
            interval = (StringUtils.isNotBlank(updateIntervalSeconds) && StringUtils.isNumeric(updateIntervalSeconds)) ? Integer.parseInt(updateIntervalSeconds) : DEFAULT_UPDATE_INTERVAL_SECONDS;
          } catch(NumberFormatException ex) {
            logger.warn("measurements.update.interval.seconds invalid value: {}, using default {}", updateIntervalSeconds, DEFAULT_UPDATE_INTERVAL_SECONDS);
          }

          Date firstRunDate = DateUtils.addSeconds(new Date(), interval);
          
          JobDataMap jobDataMap = new JobDataMap();
          jobDataMap.put("instanceId", instanceId);
          jobDataMap.put("stackId", stackId);

          JobDetail job = JobBuilder.newJob(GetCloudWatchInstanceMeasurementsJob.class).withIdentity(jobKey).setJobData(jobDataMap).build();
          Trigger trigger = TriggerBuilder.newTrigger()
              .forJob(jobKey)
              .withIdentity(TriggerKey.triggerKey(instanceId, "MeasurementsUpdateTrigger"))
              .withSchedule(SimpleScheduleBuilder.repeatSecondlyForever(interval))
              .build(); //this trigger will repeat the job forever with the specified interval, first run is NOW (check the job why)
          scheduler.scheduleJob(job, trigger);
          logger.debug("Measurements update scheduled for instance: {}", instanceId);
          return new JobInfo(jobKey, firstRunDate);
        } else {
          logger.warn("Measurements update already scheduled for instance: {}", instanceId);
          return null;
        }
      } catch (SchedulerException e) {
        logger.error("Unable to schedule measurements update for instance: {}/n{}", instanceId, e);
      }
      return null;
    }
 
    @Override
    public void cancelMeasurementsUpdate(String instanceId) {
      JobKey jobKey = JobKey.jobKey(instanceId, "MeasurementsUpdateJob");
      cancelMeasurementsUpdate(jobKey);
    }
  }
