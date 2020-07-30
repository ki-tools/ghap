package io.ghap.provision.vpg.scheduler;

/**
 * Created by arao on 9/18/15.
 */
public interface ProvisionedResourceJobScheduler {

  JobInfo scheduleIdleResourceNotificationJob(String userIdentifier, String stackIdentifier, String instanceIdentifier);

  JobInfo scheduleStopForIdleProvisionedResources(String userIdentifier, String stackIdentifier);

  JobInfo scheduleTerminateForIdleProvisionedResources(String userIdentifier, String stackIdentifier);

  void cancelScheduledStopForIdleProvisionedResources(String userIdentifier, String stackIdentifier);

  void postponeScheduledStopForIdleProvisionedResources(String userIdentifier, String stackIdentifier);
}
