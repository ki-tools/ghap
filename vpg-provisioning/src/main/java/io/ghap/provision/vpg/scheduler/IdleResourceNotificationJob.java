package io.ghap.provision.vpg.scheduler;

import com.netflix.governator.annotations.Configuration;
import io.ghap.provision.vpg.InstanceMonitoringState;
import io.ghap.provision.vpg.VirtualPrivateGrid;
import io.ghap.provision.vpg.data.MonitoringResourceFactory;
import io.ghap.provision.vpg.data.StackInfoRetriever;
import io.ghap.provision.vpg.data.VPGMultiFactory;
import io.ghap.provision.vpg.data.VirtualResource;
import io.ghap.provision.vpg.mailer.IdleResourceNotificationMailer;
import org.apache.commons.mail.EmailException;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.List;

/**
 * Represents the scheduled job with the responsibility to evaluate activity on a resource in order determine whether
 * a scheduled stop on the resource should be cancelled.
 */
public class IdleResourceNotificationJob implements Job {

  private Logger logger = LoggerFactory.getLogger(IdleResourceNotificationJob.class);

  @Inject
  private VPGMultiFactory vpgMultiFactory;

  @com.google.inject.Inject(optional = true)
  private MonitoringResourceFactory monitoringResourceFactory;

  @com.google.inject.Inject
  private IdleResourceNotificationMailer idleResourceNotificationMailer;

  @Configuration("vpg.provisioning.service.url")
  private String provisioningServiceRestBaseUrl;


  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    JobKey jobKey = context.getJobDetail().getKey();
    JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();

    logger.debug(String.format("Begin execution of job <%s / %s> at %s", jobKey.getGroup(), jobKey.getName(), new Date()));

    String stackId = jobDataMap.getString("StackId");
    String instanceId = jobDataMap.getString("InstanceId");

    logger.debug(String.format("Idle Instance Id=%s, Stack Id = %s", instanceId, stackId));

    VirtualPrivateGrid virtualPrivateGrid = vpgMultiFactory.getByStackId(stackId);
    if (virtualPrivateGrid != null) {
      scheduleStopForIdleProvisionedResources(instanceId, virtualPrivateGrid);

    } else {
      logger.error(String.format("Unable to find the grid associated with stackId=%s", stackId));
    }
  }


  private void scheduleStopForIdleProvisionedResources(String idleInstanceIdentifier, VirtualPrivateGrid vpg) {

    VirtualResource virtualResource = getVirtualResource(vpg, idleInstanceIdentifier);

    StackInfoRetriever stackInfoRetriever = new StackInfoRetriever(vpg.getStackId());

    if (stackInfoRetriever.isAutostopAvailableForStack()) {
      JobInfo jobInfo = vpgMultiFactory.scheduleStopForIdleProvisionedResources(vpg);
      if (jobInfo != null) {
        String userName = stackInfoRetriever.getUserName();
        String userEmail = stackInfoRetriever.getUserEmail();
        //String activityName = stackInfo.get(STACK_TAG_ACTIVITY_NAME);

        String messageTokenId = getOriginalCloudWatchMessageId(vpg.getStackId());

        sendEmailNotificationToUser(virtualResource, jobInfo.getScheduledDate(), userName, userEmail, messageTokenId);
      }

    } else {
      logger.info(String.format("Autostop is explicitly disabled for stack <%s>", vpg.getStackId()));
    }

  }

  private String getOriginalCloudWatchMessageId(String stackId) {
    if (monitoringResourceFactory != null) {
      List<InstanceMonitoringState> instanceMonitoringStates = monitoringResourceFactory.getByStackId(stackId);
      if (instanceMonitoringStates != null && !instanceMonitoringStates.isEmpty()) {

        return instanceMonitoringStates.get(0).getMessageId();
      }
    }
    return null;
  }

  private VirtualResource getVirtualResource(VirtualPrivateGrid vpg, String instanceId) {
    List<VirtualResource> virtualResources = vpgMultiFactory.getVirtualResources(vpg);
    if (virtualResources != null) {
      for (VirtualResource resource : virtualResources) {
        if (resource.getInstanceId().equals(instanceId)) {
          return resource;
        }
      }
    }
    return null;
  }

  private void sendEmailNotificationToUser(VirtualResource virtualResource, Date scheduledStopTime,
                                           String userName, String userEmail, String messageTokenId) {

    try {
      String urlPostpone =
              String.format("%s/Monitoring/scheduled-cleanup/postpone?token=%s",
                      provisioningServiceRestBaseUrl, URLEncoder.encode(messageTokenId, "UTF-8"));

      String activityName = virtualResource.getInstanceId();
      String activityOsType = virtualResource.getInstanceOsType();


      String activityIPAddress = virtualResource.getAddress();

      logger.debug(String.format("Sending email to %s regarding scheduled stop on an idle instance.", userEmail));
      logger.debug(String.format("email link %s", urlPostpone));

      if (userEmail != null) {
        idleResourceNotificationMailer.send(userName, userEmail, activityName, activityOsType,
                activityIPAddress, urlPostpone, scheduledStopTime);
      } else {
        if (logger.isErrorEnabled()) {
          logger.error(String.format(
                  "Unable to send email to user <%s> as the email address is unknown.", userName));
        }
      }

    } catch (EmailException | UnsupportedEncodingException e) {
      if (logger.isErrorEnabled()) {
        logger.error(e.getMessage(), e);
      }
    }
  }

}
