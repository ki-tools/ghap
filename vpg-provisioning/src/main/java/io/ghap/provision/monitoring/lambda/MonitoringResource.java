package io.ghap.provision.monitoring.lambda;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.ec2.model.Tag;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.governator.annotations.Configuration;
import io.ghap.provision.vpg.InstanceMonitoringState;
import io.ghap.provision.vpg.VirtualPrivateGrid;
import io.ghap.provision.vpg.data.MonitoringResourceFactory;
import io.ghap.provision.vpg.data.StackInfoRetriever;
import io.ghap.provision.vpg.data.VPGMultiFactory;
import io.ghap.provision.vpg.data.VirtualResource;
import io.ghap.provision.vpg.mailer.IdleResourceNotificationMailer;
import io.ghap.provision.vpg.scheduler.JobInfo;
import io.ghap.util.ContentTemplateAccessor;
import org.apache.commons.mail.EmailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 */
@Singleton
@Path("Monitoring")
public class MonitoringResource {

  private final Logger logger = LoggerFactory.getLogger(MonitoringResource.class);


  private static final int THRESHOLD_MINUTES_FOR_SPURIOUS_CLOUDWATCH_ALARM = 5;

  @Inject
  private MonitoringResourceFactory monitoringResourceFactory;

  @Inject
  private VPGMultiFactory vpgFactory;

  @Inject
  private IdleResourceNotificationMailer idleResourceNotificationMailer;

  @Inject
  private ContentTemplateAccessor contentTemplateAccessor;

  @Configuration("admin.email")
  private String adminEmail;


  @Path("/createMetric")
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response storeMetric(CloudWatchMessage message) {
    monitoringResourceFactory.create(message, null);
    return Response.status(Response.Status.OK).build();
  }

  @Path("/get")
  @GET
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public List<InstanceMonitoringState> getStates() {
    return monitoringResourceFactory.list();
  }


  /**
   * Schedule a stop for idle provisioned resources that are associated with the CloudWatch message.
   *
   * @param message The CloudWatch message associated with the idle resouces alarm
   * @return {@link javax.ws.rs.core.Response.Status#OK} on success or {@link javax.ws.rs.core.Response.Status#NOT_MODIFIED} on error
   */
  @Path("/scheduled-cleanup/processAlarmMessage")
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response processAlarmFromIdleProvisionedResources(CloudWatchMessage message, @Context UriInfo uriInfo) {

    logger.debug("Received request to process alarm from an idle instance.");
    try {

      Instance idleInstance = findInstanceThatTriggeredAlarm(message);

      VirtualPrivateGrid vpg = findVirtualPrivateGrid(idleInstance);

      if (vpg != null) {
        if (wasCloudWatchAlarmActivated(message)) {
          activateAutoStopMechanismForProvisionedResources(idleInstance, vpg, message, uriInfo);

        } else if (wasCloudWatchAlarmDeactivated(message)) {
          cancelScheduledStopOnIdleProvisionedResource(vpg.getStackId());
        }

        return Response.ok().build();

      } else {
        //If there is no grid found for the specified stackId, assume that the stack was not provisioned
        //by the provisioning service.
        //Should we return an error in this case?

        //return Response.status(Response.Status.NOT_FOUND).build();
        return Response.ok().build();
      }


    } catch (Exception e) {
      logger.error(e.getMessage(), e);

      return Response.status(Response.Status.NOT_MODIFIED).build();
    }
  }

  private boolean wasCloudWatchAlarmActivated(CloudWatchMessage message) {
    return "ALARM".equals(message.NewStateValue);
  }

  private boolean wasCloudWatchAlarmDeactivated(CloudWatchMessage message) {
    return "ALARM".equals(message.OldStateValue) ||
            ("INSUFFICIENT_DATA".equals(message.OldStateValue) && "OK".equals(message.NewStateValue));
  }

  /**
   * Schedule a stop for idle provisioned resources that are associated with the CloudWatch message.
   *
   * @param message The CloudWatch message associated with the idle resouces alarm
   * @return {@link javax.ws.rs.core.Response.Status#OK} on success or {@link javax.ws.rs.core.Response.Status#NOT_MODIFIED} on error
   */
  @Path("/scheduled-cleanup/schedule")
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response scheduleStopForIdleProvisionedResources(CloudWatchMessage message, @Context UriInfo uriInfo) {
    logger.debug("Received request to schedule a stop on an idle instance.");
    try {

      Instance idleInstance = findInstanceThatTriggeredAlarm(message);

      VirtualPrivateGrid vpg = findVirtualPrivateGrid(idleInstance);

      if (vpg != null) {
        scheduleStopForIdleProvisionedResources(idleInstance, vpg, message, uriInfo);

        return Response.ok().build();

      } else {
        //If there is no grid found for the specified stackId, assume that the stack was not provisioned
        //by the provisioning service.
        //Should we return an error in this case?

        //return Response.status(Response.Status.NOT_FOUND).build();
        return Response.ok().build();
      }


    } catch (Exception e) {
      if (logger.isErrorEnabled()) {
        logger.error(e.getMessage(), e);
      }
      return Response.status(Response.Status.NOT_MODIFIED).build();
    }
  }

  private void scheduleStopForIdleProvisionedResources(Instance idleInstance, VirtualPrivateGrid vpg,
                                                       CloudWatchMessage message, UriInfo uriInfo) {

    VirtualResource virtualResource = getVirtualResource(vpg, idleInstance.getInstanceId());
    if (canScheduleStopForInstance(idleInstance)) {
      StackInfoRetriever stackInfoRetriever = new StackInfoRetriever(vpg.getStackId());
      if (stackInfoRetriever.isAutostopAvailableForStack()) {
        JobInfo jobInfo = vpgFactory.scheduleStopForIdleProvisionedResources(vpg);
        if (jobInfo != null) {
          monitoringResourceFactory.create(message, vpg.getStackId());



          String userName = stackInfoRetriever.getUserName();
          String userEmail = stackInfoRetriever.getUserEmail();
          //String activityName = stackInfo.get(STACK_TAG_ACTIVITY_NAME);

          sendEmailNotificationToUser(message, virtualResource, uriInfo.getBaseUri(),
                  jobInfo.getScheduledDate(), userName, userEmail);
        }

      } else {
        logger.info(String.format("Autostop is explicitly disabled for stack <%s>", vpg.getStackId()));
      }

    } else {
      logger.info(String.format("Ignore Autostop for instance <%s> since the CloudWatch alarm was likely a misfire.",
              idleInstance.getInstanceId()));
    }

  }

  private void activateAutoStopMechanismForProvisionedResources(Instance idleInstance, VirtualPrivateGrid vpg,
                                                                CloudWatchMessage message, UriInfo uriInfo) {

    VirtualResource virtualResource = getVirtualResource(vpg, idleInstance.getInstanceId());
    if (canScheduleStopForInstance(idleInstance)) {

      JobInfo jobInfo = vpgFactory.activateAutoStopMechanismForProvisionedResources(vpg, virtualResource.getInstanceId());
      if (jobInfo != null) {
        monitoringResourceFactory.create(message, vpg.getStackId());
      }

    } else {
      logger.info(String.format("Ignore Autostop for instance <%s> since the CloudWatch alarm was likely a misfire.",
              idleInstance.getInstanceId()));
    }

  }

  private boolean canScheduleStopForInstance(Instance ec2Instance) {
    InstanceState instanceState = ec2Instance.getState();
    if (instanceState != null) {
      if (! instanceState.getName().equalsIgnoreCase(InstanceStateName.Running.name())) {
        logger.info(String.format("The instance <%s> is not currently in a running state and is instead %s.",
                ec2Instance.getInstanceId(), instanceState.getName()));

        return false;
      }
    }

    boolean checkForFalseAlarmsOnRestart = false;
    if (checkForFalseAlarmsOnRestart) {
      Date instanceLaunchTime = ec2Instance.getLaunchTime();
      if (instanceLaunchTime!= null) {
        Date currentTime = new Date();
        long timeIntervalInMinutes = TimeUnit.MILLISECONDS.toMinutes(currentTime.getTime() - instanceLaunchTime.getTime());

        return (timeIntervalInMinutes > THRESHOLD_MINUTES_FOR_SPURIOUS_CLOUDWATCH_ALARM);
      }
      return false;

    } else {
      return true;
    }

  }

  private void sendEmailNotificationToUser(CloudWatchMessage message,
                                           VirtualResource virtualResource, URI baseUri,
                                           Date scheduledStopTime, String userName, String userEmail) {

    try {
      String urlPostpone =
              String.format("%sMonitoring/scheduled-cleanup/postpone?token=%s",
                      baseUri, URLEncoder.encode(message.MessageId, "UTF-8"));

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


  /**
   * Postpone a scheduled stop for idle provisioned resources that are associated with a given message token.
   *
   * @return {@link javax.ws.rs.core.Response.Status#OK} on success or {@link javax.ws.rs.core.Response.Status#NOT_MODIFIED} on error
   */
  @Path("/scheduled-cleanup/postpone")
  @GET
  @Produces(MediaType.TEXT_HTML)
  public Response postponeScheduledStopForIdleProvisionedResources(
          @QueryParam("token") String messageId) {

    logger.debug(String.format("Begin process to postpone scheduled stop. TokenId=<%s>.", messageId));

    try {
      InstanceMonitoringState monitoringState = monitoringResourceFactory.getByMessageId(messageId);

      if (monitoringState != null) {
        String stackId = monitoringState.getStackId();

        if (stackId != null && stackId.trim().length() > 0) {
          VirtualPrivateGrid vpg = vpgFactory.getByStackId(stackId);
          vpgFactory.postponeScheduledStopForIdleProvisionedResources(vpg);

          String successTemplate =
                  contentTemplateAccessor.getTemplate("postpone-idle-resources-success",
                          Collections.singletonMap("adminEmail", adminEmail));

          return Response.ok(successTemplate).build();


        } else {
          if (logger.isErrorEnabled()) {
            logger.error("The stackId on the monitored instance state is empty");
          }

          String failureTemplate =
                  contentTemplateAccessor.getTemplate("postpone-idle-resources-failure",
                          Collections.singletonMap("adminEmail", adminEmail));
          return Response.status(Response.Status.NOT_FOUND).entity(failureTemplate).build();
        }

      } else {
        if (logger.isErrorEnabled()) {
          logger.error(String.format("Unable to find a monitored instance state with message id %s", messageId));
        }
        String failureTemplate =
                contentTemplateAccessor.getTemplate("postpone-idle-resources-failure",
                        Collections.singletonMap("adminEmail", adminEmail));

        return Response.status(Response.Status.NOT_FOUND).entity(failureTemplate).build();
      }



    } catch (Exception e) {
      if (logger.isErrorEnabled()) {
        logger.error(e.getMessage(), e);
      }
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Cancel a scheduled stop for idle provisioned resources that are associated with a given message token.
   *
   * @return {@link javax.ws.rs.core.Response.Status#OK} on success or {@link javax.ws.rs.core.Response.Status#NOT_MODIFIED} on error
   */
  @Path("/scheduled-cleanup/cancel")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response cancelScheduledStopForIdleProvisionedResources(
          @QueryParam("token") String messageId) {

    logger.debug(String.format("Begin process to cancel scheduled stop. TokenId=<%s>.", messageId));

    try {
      InstanceMonitoringState monitoringState = monitoringResourceFactory.getByMessageId(messageId);

      if (monitoringState != null) {
        cancelScheduledStopOnIdleProvisionedResource(monitoringState.getStackId());

      } else {
        if (logger.isErrorEnabled()) {
          logger.error(String.format("Unable to find a monitored instance state with message id %s", messageId));
        }

      }


      return Response.ok().build();
    } catch (Exception e) {
      if (logger.isErrorEnabled()) {
        logger.error(e.getMessage(), e);
      }
      return Response.status(Response.Status.NOT_MODIFIED).build();
    }
  }

  private void cancelScheduledStopOnIdleProvisionedResource(String stackId) {

    if (stackId != null && stackId.trim().length() > 0) {
      VirtualPrivateGrid vpg = vpgFactory.getByStackId(stackId);

      List<InstanceMonitoringState> monitoringStates = monitoringResourceFactory.getByStackId(stackId);

      if (monitoringStates != null && (!monitoringStates.isEmpty())) {
        vpgFactory.cancelScheduledStopForIdleProvisionedResources(vpg);

        //Once the scheduled stop is cancelled, we can remove the monitored resource states too.
        monitoringResourceFactory.delete(monitoringStates);
      }

    } else {
      if (logger.isErrorEnabled()) {
        logger.error("The stackId on the monitored instance state is empty");
      }

    }
  }

  private Instance findInstanceThatTriggeredAlarm(CloudWatchMessage cloudWatchMessage) {
    Instance ec2Instance = null;

    String instanceId = findInstanceIdThatTriggeredAlarm(cloudWatchMessage);
    if (instanceId != null) {
      ec2Instance = getInstanceInformation(instanceId);
    }

    return ec2Instance;
  }

  private String findInstanceIdThatTriggeredAlarm(CloudWatchMessage cloudWatchMessage) {
    for (Dimension dimension : cloudWatchMessage.Trigger.Dimensions) {
      if (dimension.name.equals("InstanceId")) {
        return dimension.value;
      }
    }

    return null;
  }

  private VirtualPrivateGrid findVirtualPrivateGrid(Instance instance) {
    VirtualPrivateGrid vpg = null;

    if (instance != null) {
      String stackId = getStackId(instance);

      vpg = vpgFactory.getByStackId(stackId);
      if (vpg == null) {
        //If there is no grid found for the specified stackId, assume that the stack was not provisioned
        //by the provisioning service.
        //Should we return an error in this case?
        if (logger.isErrorEnabled()) {
          logger.error(String.format("The stack id %s was not provisioned by the GHAP provisioning service", stackId));
        }
      }
    }

    return vpg;
  }

  private String getStackId(Instance ec2Instance) {
    List<Tag> tags = ec2Instance.getTags();
    for (Tag tag : tags) {
      if (tag.getKey().equals("aws:cloudformation:stack-id")) {
        return tag.getValue();
      }
    }
    return null;
  }

  private Instance getInstanceInformation(String instanceId) {
    AmazonEC2Client ec2Client = new AmazonEC2Client();

    DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest();
    describeInstancesRequest.setInstanceIds(Arrays.asList(instanceId));


    DescribeInstancesResult describeInstancesResult = ec2Client.describeInstances(describeInstancesRequest);

    if (!describeInstancesResult.getReservations().isEmpty()) {
      Reservation reservation = describeInstancesResult.getReservations().get(0);
      return getInstanceInformation(reservation, instanceId);
    }

    return null;
  }

  private Instance getInstanceInformation(Reservation reservation, String instanceId) {
    for (Instance instance : reservation.getInstances()) {
      if (instance.getInstanceId().equals(instanceId)) {
        return instance;
      }
    }
    return null;
  }


  private VirtualResource getVirtualResource(VirtualPrivateGrid vpg, String instanceId) {
    List<VirtualResource> virtualResources = vpgFactory.getVirtualResources(vpg);
    if (virtualResources != null) {
      for (VirtualResource resource : virtualResources) {
        if (resource.getInstanceId().equals(instanceId)) {
          return resource;
        }
      }
    }
    return null;
  }

}
