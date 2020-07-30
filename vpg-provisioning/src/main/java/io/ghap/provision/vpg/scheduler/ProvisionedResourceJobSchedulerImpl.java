package io.ghap.provision.vpg.scheduler;

import com.google.inject.Inject;
import com.netflix.governator.annotations.Configuration;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

import static org.quartz.DateBuilder.IntervalUnit;

/**
 * Created by arao on 9/18/15.
 */
public class ProvisionedResourceJobSchedulerImpl implements ProvisionedResourceJobScheduler {

  private Logger logger = LoggerFactory.getLogger(ProvisionedResourceJobSchedulerImpl.class);

  @Inject
  private Scheduler scheduler;

  @Configuration("idleResource.scheduledStop.timeValue")
  private String scheduledStopTimeValue;

  @Configuration("idleResource.scheduledStop.timeUnit")
  private String scheduledStopTimeUnits;

  @Configuration("idleResource.scheduledStop.postponeStopBy.timeValue")
  private String postponeScheduledStopTimeValue;

  @Configuration("idleResource.scheduledStop.postponeStopBy.timeUnit")
  private String postponeScheduledStopTimeUnits;

  @Configuration("idleResource.scheduledStop.sendNotificationBy.timeValue")
  private String idleResourceNotificationTimeValue;

  @Configuration("idleResource.scheduledStop.sendNotificationBy.timeUnit")
  private String idleResourceNotificationTimeUnits;

  @Configuration("idleResource.scheduledTerminate.timeValue")
  private String scheduledTerminateTimeValue;

  @Configuration("idleResource.scheduledTerminate.timeUnit")
  private String scheduledTerminateTimeUnits;

  private static final int DEFAULT_SCHEDULED_TIME_VALUE = 24;
  private static final IntervalUnit DEFAULT_SCHEDULED_TIME_UNITS = IntervalUnit.HOUR;

  private static final int DEFAULT_NOTIFICATION_TIME_VALUE = 24;
  private static final IntervalUnit DEFAULT_NOTIFICATION_TIME_UNITS = IntervalUnit.HOUR;

  private static final int DEFAULT_SCHEDULE_TERMINATE_TIME_VALUE = 120;
  private static final IntervalUnit DEFAULT_SCHEDULE_TERMINATE_TIME_UNITS = IntervalUnit.HOUR;



  @Override
  public JobInfo scheduleIdleResourceNotificationJob(String userIdentifier, String stackIdentifier, String instanceIdentifier) {


    logger.debug(String.format("Scheduling job to handle idle resource notifications resource %s of stack %s", instanceIdentifier, stackIdentifier));

    JobKey jobKey = createIdleResourceNotificationJobKey(stackIdentifier);

    try {

      if (!scheduler.checkExists(jobKey)) //only schedule the job if one does not already exist for the stack.
      {
        JobDetail job = createIdleResourceNotificationJob(jobKey, stackIdentifier, instanceIdentifier);

        Date scheduledExecutionTime = deriveScheduleStartTime(idleResourceNotificationTimeValue, idleResourceNotificationTimeUnits,
                DEFAULT_NOTIFICATION_TIME_VALUE, DEFAULT_NOTIFICATION_TIME_UNITS);

        Trigger trigger = createIdleResourceNotificationTrigger(job.getKey(), stackIdentifier, scheduledExecutionTime);

        scheduler.scheduleJob(job, trigger);

        logger.debug(String.format("A job has been scheduled to handle idle resource notifications on Stack <%s>", stackIdentifier));

        return new JobInfo(jobKey, scheduledExecutionTime);

      } else {
        logger.debug(String.format("A job to handle idle resource notifications on Stack <%s> already exists", stackIdentifier));

        return null;
      }


    } catch (SchedulerException e) {
      logger.error(String.format("Unable to schedule an activity evaluation job for resource <%s> on Stack <%s>",
              instanceIdentifier, stackIdentifier), e);
    }

    return null;
  }

  private JobDetail createIdleResourceNotificationJob(JobKey jobKey, String stackIdentifier, String instanceIdentifier) {

    JobDataMap jobDataMap = new JobDataMap();
    jobDataMap.put("StackId", stackIdentifier);
    jobDataMap.put("InstanceId", instanceIdentifier);

    return JobBuilder.newJob(IdleResourceNotificationJob.class)
            .withIdentity(jobKey).setJobData(jobDataMap)
            .build();
  }

  private JobKey createIdleResourceNotificationJobKey(String stackIdentifier) {
    return JobKey.jobKey(stackIdentifier, "IdleResourceNotificationJob");
  }

  private Trigger createIdleResourceNotificationTrigger(JobKey jobKey, String stackIdentifier, Date triggerStartTime) {
    return TriggerBuilder.newTrigger()
            .forJob(jobKey)
            .withIdentity(TriggerKey.triggerKey(stackIdentifier, "IdleResourceNotificationTrigger"))
            .withSchedule(SimpleScheduleBuilder.simpleSchedule().withRepeatCount(0))
            .startAt(triggerStartTime)
            .build();
  }

  @Override
  public JobInfo scheduleStopForIdleProvisionedResources(String userIdentifier, String stackIdentifier) {

    logger.debug(String.format("Scheduling stop for idle Stack %s", stackIdentifier));

    JobKey jobKey = createStopIdleResourcesJobKey(stackIdentifier);

    try {

      if (!scheduler.checkExists(jobKey)) //only schedule the job if one does not already exist for the stack.
      {
        JobDetail job = createStopIdleResourcesJob(jobKey, stackIdentifier);

        Date scheduledStopExecutionTime = deriveScheduleStartTime(scheduledStopTimeValue, scheduledStopTimeUnits,
                DEFAULT_SCHEDULED_TIME_VALUE, DEFAULT_SCHEDULED_TIME_UNITS);

        Trigger trigger = createStopIdleResourcesTrigger(job.getKey(), stackIdentifier, scheduledStopExecutionTime);

        scheduler.scheduleJob(job, trigger);

        logger.debug(String.format("A stop has been scheduled for idle Stack <%s>", stackIdentifier));

        return new JobInfo(jobKey, scheduledStopExecutionTime);

      } else {
        logger.debug(String.format("A scheduled stop already exists for idle Stack <%s>", stackIdentifier));

        return null;
      }


    } catch (SchedulerException e) {
      logger.error(String.format("Unable to schedule stop for idle Stack <%s>", stackIdentifier), e);
    }

    return null;
  }

  @Override
  public JobInfo scheduleTerminateForIdleProvisionedResources(String userIdentifier, String stackIdentifier) {

    logger.debug(String.format("Scheduling terminate for idle Stack %s", stackIdentifier));

    JobKey jobKey = createTerminateResourcesJobKey(stackIdentifier);

    try {

      if (!scheduler.checkExists(jobKey)) //only schedule the job if one does not already exist for the stack.
      {
        JobDetail job = createTerminateIdleResourcesJob(jobKey, stackIdentifier);

        Date scheduledStopExecutionTime = deriveScheduleStartTime(scheduledTerminateTimeValue, scheduledTerminateTimeUnits,
                DEFAULT_SCHEDULE_TERMINATE_TIME_VALUE, DEFAULT_SCHEDULE_TERMINATE_TIME_UNITS);

        Trigger trigger = createTerminateIdleResourcesTrigger(job.getKey(), stackIdentifier, scheduledStopExecutionTime);

        scheduler.scheduleJob(job, trigger);

        logger.info(String.format("A terminate has been scheduled for idle Stack <%s>, termination date %s%n", stackIdentifier, scheduledStopExecutionTime));

        return new JobInfo(jobKey, scheduledStopExecutionTime);

      } else {
        logger.debug(String.format("A scheduled stop already exists for idle Stack <%s>", stackIdentifier));

        return null;
      }


    } catch (SchedulerException e) {
      logger.error(String.format("Unable to schedule stop for idle Stack <%s>", stackIdentifier), e);
    }

    return null;
  }

  private JobDetail createTerminateIdleResourcesJob(JobKey jobKey, String stackIdentifier) {

    JobDataMap jobDataMap = new JobDataMap();
    jobDataMap.put("StackId", stackIdentifier);
    return JobBuilder.newJob(TerminateIdleResourcesJob.class)
            .withIdentity(jobKey).setJobData(jobDataMap)
            .build();
  }

  private Trigger createTerminateIdleResourcesTrigger(JobKey jobKey, String stackIdentifier, Date triggerStartTime) {
    return TriggerBuilder.newTrigger()
            .forJob(jobKey)
            .withIdentity(TriggerKey.triggerKey(stackIdentifier, "IdleResourceTerminateTrigger"))
            .withSchedule(SimpleScheduleBuilder.simpleSchedule().withRepeatCount(0))
            .startAt(triggerStartTime)
            .build();
  }

  private JobDetail createStopIdleResourcesJob(JobKey jobKey, String stackIdentifier) {

    JobDataMap jobDataMap = new JobDataMap();
    jobDataMap.put("StackId", stackIdentifier);

    return JobBuilder.newJob(StopIdleResourcesJob.class)
            .withIdentity(jobKey).setJobData(jobDataMap)
            .build();
  }

  private JobKey createStopIdleResourcesJobKey(String stackIdentifier) {
    return JobKey.jobKey(stackIdentifier, "IdleResourceStopJob");
  }

  private Trigger createStopIdleResourcesTrigger(JobKey jobKey, String stackIdentifier, Date triggerStartTime) {
    return TriggerBuilder.newTrigger()
            .forJob(jobKey)
            .withIdentity(TriggerKey.triggerKey(stackIdentifier, "IdleResourceStopTrigger"))
            .withSchedule(SimpleScheduleBuilder.simpleSchedule().withRepeatCount(0))
            .startAt(triggerStartTime)
            .build();
  }

  private Date deriveScheduleStartTime(String timeValue, String timeUnits, int defaultTimeValue, IntervalUnit defaultTimeUnits) {
    int interval = (timeValue != null) ? Integer.valueOf(timeValue) : defaultTimeValue;

    IntervalUnit intervalUnits = (timeUnits != null) ? IntervalUnit.valueOf(timeUnits) : defaultTimeUnits;

    return DateBuilder.futureDate(interval, intervalUnits);
  }

  @Override
  public void cancelScheduledStopForIdleProvisionedResources(String userIdentifier, String stackIdentifier) {
    //logger.debug(String.format("Cancelling scheduled stop for idle Stack %s", stackIdentifier));


    cancelIdleResourceNotificationJob(stackIdentifier);
    cancelStopIdleResourcesJob(stackIdentifier);
    cancelTerminateIdleResourcesJob(stackIdentifier);
  }

  private void cancelIdleResourceNotificationJob(String stackIdentifier) {

    JobKey jobKey = createIdleResourceNotificationJobKey(stackIdentifier);

    try {
      if (scheduler.checkExists(jobKey)) {
        scheduler.deleteJob(jobKey);
        logger.debug(String.format("Cancelled idle resources notification for Stack %s", stackIdentifier));
      }

    } catch (SchedulerException e) {
      logger.error(String.format("Unable to cancel idle resources notification for Stack Id <%s>", stackIdentifier), e);
    }
  }

  private void cancelStopIdleResourcesJob(String stackIdentifier) {

    JobKey jobKey = createStopIdleResourcesJobKey(stackIdentifier);

    try {
      if (scheduler.checkExists(jobKey)) {
        scheduler.deleteJob(jobKey);
        logger.debug(String.format("Cancelled scheduled stop for Stack %s", stackIdentifier));
      }

    } catch (SchedulerException e) {
      logger.error(String.format("Unable to cancel scheduled stop for Stack Id <%s>", stackIdentifier), e);
    }
  }

  private void cancelTerminateIdleResourcesJob(String stackIdentifier) {

    JobKey jobKey = createTerminateResourcesJobKey(stackIdentifier);

    try {
      if (scheduler.checkExists(jobKey)) {
        scheduler.deleteJob(jobKey);
        logger.debug(String.format("Cancelled scheduled terminate for Stack %s", stackIdentifier));
      }

    } catch (SchedulerException e) {
      logger.error(String.format("Unable to cancel scheduled terminate for Stack Id <%s>", stackIdentifier), e);
    }
  }

  private JobKey createTerminateResourcesJobKey(String stackIdentifier) {
    return JobKey.jobKey(stackIdentifier, "IdleResourceTerminateJob");
  }

  @Override
  public void postponeScheduledStopForIdleProvisionedResources(String userIdentifier, String stackIdentifier) {
    logger.debug(String.format("Postponing scheduled stop for idle Stack %s", stackIdentifier));

    JobKey jobKey = createStopIdleResourcesJobKey(stackIdentifier);


    try {
      if (scheduler.checkExists(jobKey)) {

        Date scheduledStopExecutionTime =
                deriveScheduleStartTime(postponeScheduledStopTimeValue, postponeScheduledStopTimeUnits, DEFAULT_SCHEDULED_TIME_VALUE, DEFAULT_SCHEDULED_TIME_UNITS);

        Trigger trigger = createStopIdleResourcesTrigger(jobKey, stackIdentifier, scheduledStopExecutionTime);

        Date rescheduledDateTime = scheduler.rescheduleJob(trigger.getKey(), trigger);

        logger.debug(String.format("Postponed scheduled stop for idle Stack %s to <%s>",
                stackIdentifier, rescheduledDateTime));

      } else {
        logger.debug(String.format("Could not find a scheduled stop for idle Stack %s", stackIdentifier));
      }

    } catch (SchedulerException e) {
      logger.error(String.format("Unable to cancel scheduled stop for idle Stack Id <%s>", stackIdentifier), e);
    }
  }
}
