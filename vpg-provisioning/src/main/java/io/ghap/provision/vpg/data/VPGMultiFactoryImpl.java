package io.ghap.provision.vpg.data;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.*;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.*;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.Tag;
import com.amazonaws.services.cloudwatch.model.StateValue;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateKeyPairRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairResult;
import com.amazonaws.services.ec2.model.DeleteKeyPairRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeKeyPairsRequest;
import com.amazonaws.services.ec2.model.DescribeKeyPairsResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.KeyPair;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.StartInstancesResult;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.amazonaws.services.ec2.model.StopInstancesResult;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;
import com.netflix.governator.annotations.Configuration;
import io.ghap.aws.annotations.AWSApiCall;
import io.ghap.provision.vpg.*;
import io.ghap.provision.vpg.Activity;
import io.ghap.provision.vpg.scheduler.JobInfo;
import io.ghap.provision.vpg.scheduler.ProvisionedResourceJobScheduler;
import io.ghap.provision.vpg.scheduler.StackMeasurementsJobScheduler;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.*;

import static io.ghap.provision.vpg.Utils.*;

public class VPGMultiFactoryImpl implements VPGMultiFactory {
  private Logger logger = LoggerFactory.getLogger(VPGMultiFactoryImpl.class);

  private final static int MAX_TRIES = 20;
  private final static int INITIAL_SLEEP_TIME = 10;
  private final static long TIMEOUT = 1000 * 60 * 5; // 5 MINUTES

  @Configuration("availability.zone")
  private String availabilityZone;

  @Configuration("vpc.id")
  private String vpc_id;
  @Configuration("security_group.id")
  private String security_group_id;
  @Configuration("public_subnet_a.id")
  private String public_subnet_a;
  @Configuration("public_subnet_b.id")
  private String public_subnet_b;
  @Configuration("private_subnet_a.id")
  private String private_subnet_a;
  @Configuration("private_subnet_b.id")
  private String private_subnet_b;
  @Configuration("domain")
  private String domain;

  @Configuration("idleResource.sns.topic.arn")
  private String idleResourcesSNSTopicArn;

  @Configuration("compute.environment.iam.role.arn")
  private String publishEventsRole;

  @Inject
  private Provider<EntityManager> emProvider;

  @Inject
  private PersonalStorageFactory personalStorageFactory;

  @Inject
  private VPGStateFactory vpgStateFactory;

  @Inject
  private VPGFactory vpgFactory;

  @Inject
  private ProvisionedResourceJobScheduler scheduler;

  @Inject
  private StackMeasurementsJobScheduler measurementScheduler;

  @Inject
  private MonitoringResourceFactory monitoringResourceFactory;

  @Transactional
  @Override
  public VirtualPrivateGrid[] create(String username, String email, UUID user_uuid, Activity[] activities) {
    List<VirtualPrivateGrid> virtualEnvs = new ArrayList<VirtualPrivateGrid>();
    for (Activity activity : activities) {
      virtualEnvs.add(create(username, email, user_uuid, activity));
    }
    return virtualEnvs.toArray(new VirtualPrivateGrid[0]);
  }

  @Transactional
  @Override
  public VirtualPrivateGrid create(String username, String email, UUID user_uuid, Activity activity) {
    if (exists(user_uuid, activity)) {
      throw new UnsupportedOperationException();
    }
    AWSCredentialsProvider credentials = new SystemPropertiesCredentialsProvider();
    AmazonEC2Client ec2Client = new AmazonEC2Client(credentials);

    // make sure the user doesn't already have a key pair, if they do we delete it,
    // this could orphan any instances using this key pair, but as they can only
    // have one it shouldn't be an issue.
    try {
      if (doesKeyPairExist(user_uuid, ec2Client)) {
        DeleteKeyPairRequest deleteKeyPairRequest = new DeleteKeyPairRequest();
        deleteKeyPairRequest.setKeyName(user_uuid.toString());
        ec2Client.deleteKeyPair(deleteKeyPairRequest);
      }
    } catch (Exception e) {
      if (logger.isErrorEnabled()) {
        logger.error(e.getMessage(), e);
      }
    }
    // first we create a new key pair to be used by this stack, this is unique to every
    // stack that gets created.
    CreateKeyPairRequest keyPairRequest = new CreateKeyPairRequest();
    keyPairRequest.setKeyName(user_uuid.toString());

    CreateKeyPairResult keyPairResult = ec2Client.createKeyPair(keyPairRequest);
    KeyPair keyPair = keyPairResult.getKeyPair();

    AmazonCloudFormationClient cloudformationClient = new AmazonCloudFormationClient(credentials.getCredentials());

    //validate the template, and get the parameters
    Set<String> templateParameterKeys = getTemplateParameterKeys(cloudformationClient, activity.getTemplateUrl());

    // standup the stack
    CreateStackRequest stackRequest = new CreateStackRequest();
    stackRequest.setCapabilities(Arrays.asList(new String[]{"CAPABILITY_IAM"}));

    String stack_name = getRandomAlphaNumericString(45);
    List<Tag> tags = new ArrayList<Tag>();
    tags.add(new Tag().withKey("UUID").withValue(user_uuid.toString()));

    if (activity.getActivityName() != null && activity.getActivityName().trim().length() > 0) {
      //tags.add(new Tag().withKey("GhapActivityName").withValue(activity.getActivityName().replace("(","").replace(")","")));
      tags.add(new Tag().withKey("GhapActivityUUID").withValue(activity.getId().toString()));
    } else {
      logger.error("No activity name being passed in. The GhapActivityName tag could not be created");
    }

    stackRequest.setTags(tags);
    stackRequest.setStackName(stack_name);
    stackRequest.setOnFailure(OnFailure.DELETE);
    stackRequest.setTemplateURL(activity.getTemplateUrl());

    TemplateParameterBuilder templateParameterBuilder = new TemplateParameterBuilder(templateParameterKeys);

    templateParameterBuilder.withParameter("KeyName", keyPair.getKeyName())
            .withParameter("GhapVPC", vpc_id)
            .withParameter("GhapNatSecurityGroup", security_group_id)
            .withParameter("GhapPublicSubnetA", public_subnet_a)
            .withParameter("GhapPublicSubnetB", public_subnet_b)
            .withParameter("GhapPrivateSubnetA", private_subnet_a)
            .withParameter("GhapPrivateSubnetB", private_subnet_b)
            .withParameter("Domain", domain)
            .withParameter("UniqueId", user_uuid.toString())
            .withParameter("Username", username)
            .withParameter("Email", (email == null || (email != null && email.isEmpty())) ? "Unknown" : email);


    if (personalStorageFactory.exists(user_uuid)) {
      PersonalStorage storage = personalStorageFactory.get(user_uuid);
      templateParameterBuilder.withParameter("UserVolumeId", storage.getVolumeId());
    }
    if (StringUtils.isNotBlank(idleResourcesSNSTopicArn)) {
      templateParameterBuilder.withParameter("IdleResourcesSNSTopicArn", idleResourcesSNSTopicArn);
    }
    if (StringUtils.isNotBlank(publishEventsRole)) {
      templateParameterBuilder.withParameter("SQSRole", publishEventsRole);
    }

    stackRequest.setParameters(templateParameterBuilder.getParameters());

    CreateStackResult result = cloudformationClient.createStack(stackRequest);
    // register the stack in the database
    VirtualPrivateGrid vpg = new VirtualPrivateGrid();
    vpg.setActivityId(activity.getId());
    vpg.setUserId(user_uuid);
    vpg.setStackId(result.getStackId());
    String secret = keyPair.getKeyMaterial();
    vpg.setPemKey(secret);
    emProvider.get().persist(vpg);

    //AVA:this stores just the random-char-string as stack id, without "arn:aws:cloudformation:us-east-1:..." prefix
    //VirtualPrivateGridStateEntry vpgStateEntry = vpgStateFactory.create(activity.getId(), user_uuid, stack_name, VirtualPrivateGridStateEntry.State.CREATED);

    //AVA: get stack id from VPG (received from amazon createStack request)
    VirtualPrivateGridStateEntry vpgStateEntry = vpgStateFactory.create(activity.getId(), user_uuid, vpg.getStackId(), VirtualPrivateGridStateEntry.State.CREATED);
   
    emProvider.get().persist(vpgStateEntry); //TODO: there is persist() call inside vpgStateFactory.create(), why need more? 

    scheduleMeasurements(vpg);
    return vpg;
  }

  private Set<String> getTemplateParameterKeys(AmazonCloudFormationClient cloudformationClient, String templateUrl) {
    Set<String> parameterKeys = new HashSet<>();

    ValidateTemplateRequest request = new ValidateTemplateRequest();

    request.setTemplateURL(templateUrl);

    ValidateTemplateResult result = cloudformationClient.validateTemplate(request);

    List<TemplateParameter> parameters = result.getParameters();
    if (parameters != null) {
      for (TemplateParameter parameter : parameters) {
        parameterKeys.add(parameter.getParameterKey());
      }
    }

    return parameterKeys;
  }

  private boolean doesKeyPairExist(UUID user_uuid, AmazonEC2Client ec2Client) {
    DescribeKeyPairsRequest describeKeyPairRequest = new DescribeKeyPairsRequest();
    describeKeyPairRequest.setKeyNames(Arrays.asList(new String[]{user_uuid.toString()}));

    try {
      DescribeKeyPairsResult descriptionKeyPairs = ec2Client.describeKeyPairs(describeKeyPairRequest);

      return (descriptionKeyPairs.getKeyPairs().size() > 0);

    } catch (AmazonServiceException ase) {
      if (ase.getStatusCode() == 400 && ase.getErrorCode().equals("InvalidKeyPair.NotFound")) {
        //since the exception is thrown if the key pair does not exist, lets not log it as an error
        logger.info(ase.getMessage());
      } else {
        throw ase;
      }
    }

    return false;
  }

  @Override
  public ActivityExistence[] existences(UUID user_uuid, Activity[] activities) {
    List<ActivityExistence> existences = new ArrayList<ActivityExistence>();
    for (Activity activity : activities) {

      boolean exists = exists(user_uuid, activity);

      ActivityExistence activityExistence = new ActivityExistence();
      activityExistence.setExistence(exists);
      activityExistence.setActivityId(activity.getId());
      existences.add(activityExistence);
    }
    return existences.toArray(new ActivityExistence[0]);
  }

  @Override
  @Transactional
  public boolean exists(UUID user_uuid, Activity activity) {
    if (logger.isDebugEnabled()) {
      logger.debug(String.format("Checking if %s has a virtual environment.", user_uuid));
    }

    try {
      VirtualPrivateGrid vpg = get(user_uuid, activity);
      if (logger.isDebugEnabled()) {
        logger.debug(String.format("%s has a virtual environment: %b", user_uuid, vpg));
      }
      if (vpg != null) {
        AWSCredentialsProvider credentials = new SystemPropertiesCredentialsProvider();
        AmazonCloudFormationClient cloudformationClient = new AmazonCloudFormationClient(credentials.getCredentials());

        DescribeStacksRequest stackRequest = new DescribeStacksRequest();
        stackRequest.setStackName(vpg.getStackId());

        if (logger.isDebugEnabled()) {
          logger.debug("Describing Stacks");
        }

        DescribeStacksResult stackResults = cloudformationClient.describeStacks(stackRequest);
        List<Stack> stacks = stackResults.getStacks();

        if (logger.isDebugEnabled()) {
          logger.debug(String.format("%s has %d stacks", user_uuid, stacks.size()));
        }
        if (stacks.size() == 1) {
          Stack stack = stacks.get(0); // there can be only one
          StackStatus status = StackStatus.fromValue(stack.getStackStatus());
          if (logger.isDebugEnabled()) {
            logger.debug(String.format("%s status for %s virtual environment.", stack.getStackStatus(), user_uuid));
            logger.debug(String.format("%s has a stack name of %s", vpg.getStackId(), stack.getStackName()));
          }
          if (status == StackStatus.CREATE_IN_PROGRESS || status == StackStatus.CREATE_COMPLETE || status == StackStatus.UPDATE_COMPLETE || status == StackStatus.UPDATE_IN_PROGRESS) {
            if (logger.isDebugEnabled()) {
              logger.debug(String.format("Returning true for %s", user_uuid));
            }
            return true;
          } else {
            // delete the entry in the database
            if (logger.isDebugEnabled()) {
              logger.debug(String.format("Invalid stack for %s deleting it from the database.", user_uuid));
            }
            emProvider.get().remove(vpg);

            // Flush the pending sqls to the database.
            // Since Hibernate executes the sql commands only on transaction commits, and fires the 'insert' calls
            // before 'deletes', this results in a unique-constraint violation on the VPG table.
            emProvider.get().flush();
          }
        }
      }
      if (logger.isDebugEnabled()) {
        logger.debug(String.format("Returning false for %s", user_uuid));
      }
      return false;
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      if (logger.isDebugEnabled()) {
        logger.debug(String.format("Returning false for %s", user_uuid));
      }
      return false;
    }
  }

  @Override
  public List<VirtualPrivateGrid> get() {
    Query query = emProvider.get().createQuery("from VirtualPrivateGrid c");
    List<VirtualPrivateGrid> results = query.getResultList();
    return results;
  }

  @Override
  public VirtualPrivateGrid getByVpgId(UUID vpgid) {
    TypedQuery<VirtualPrivateGrid> query = emProvider.get().createQuery("from VirtualPrivateGrid where id = :vpgid", VirtualPrivateGrid.class);
    query.setParameter("vpgid", vpgid);
    Object vpg = null;
    try {
      vpg = query.getSingleResult();
    } catch (NoResultException nre) {
      if (logger.isDebugEnabled()) {
        logger.debug(String.format("No result found for vpg %s", vpgid));
      }
    }
    return (VirtualPrivateGrid) vpg;
  }

  /**
   * Get All VirtualResources currently allocated to a given User.
   *
   * @param user_uuid
   * @return
   */
  @Override
  public List<VirtualResource> getVirtualResources(UUID user_uuid) {
    Set<VirtualPrivateGrid> vpgs = new HashSet(get(user_uuid));
    List<VirtualResource> resources = getVirtualResources(vpgs, true);
    return resources;
  }

  @Override
  public List<VirtualResource> getVirtualResources(VirtualPrivateGrid vpg) {
    AWSCredentialsProvider credentials = new SystemPropertiesCredentialsProvider();
    AmazonEC2Client ec2Client = new AmazonEC2Client(credentials);

    List<VirtualResource> resources = new ArrayList<VirtualResource>();

    Set<String> ec2InstancesInStack = getEc2InstancesInStack(vpg.getStackId());

    for (String ec2InstanceId : ec2InstancesInStack) {
      try {
        resources.addAll(getVirtualResources(vpg, ec2InstanceId, ec2Client));

      } catch (AmazonServiceException ase) {
        if (logger.isDebugEnabled()) {
          logger.debug(ase.getMessage(), ase);
        }
      }
    }
    return resources;
  }

  @Override
  public List<VirtualResource> getVirtualResources(boolean inSyncOnly) {
    Set<VirtualPrivateGrid> vpgs = new HashSet<>();
    vpgs.addAll(get());
    List<VirtualResource> resources = getVirtualResources(vpgs, inSyncOnly);
    return resources;
  }

  @Override
  public List<VirtualResource> getVirtualResources(Set<VirtualPrivateGrid> vpgs, boolean inSyncOnly) {
    AWSCredentialsProvider credentials = new SystemPropertiesCredentialsProvider();
    AmazonEC2Client ec2Client = new AmazonEC2Client(credentials);

    List<VirtualResource> resources = new ArrayList<>();
    Set<String> stackIds = new HashSet<>();
    for(VirtualPrivateGrid vpg: vpgs) {
      stackIds.add(vpg.getStackId());
    }

    //Map<String, String> ec2InstancesInStack = getEc2InstancesInStack(stackIds);

    try {
      resources.addAll(getVirtualResources(vpgs, ec2Client, inSyncOnly));
    } catch(AmazonServiceException ase) {
      if(logger.isDebugEnabled()) {
        logger.debug(ase.getMessage(), ase);
      }
    }
    return resources;
  }

  @AWSApiCall
  List<VirtualResource> getVirtualResources(Set<VirtualPrivateGrid> vpgs, AmazonEC2Client ec2Client, boolean inSyncOnly) {
    long start = Calendar.getInstance().getTimeInMillis();
    List<VirtualResource> resources = new ArrayList<>();

    DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest();
    //describeInstancesRequest.setInstanceIds(ec2InstanceIdMap.keySet());

    //Filter the instances to only get those with the UUID tag, to ensure we get provisioned compute resources only
    com.amazonaws.services.ec2.model.Filter filter1 =
            new com.amazonaws.services.ec2.model.Filter("tag-key", Collections.singletonList("UUID"));
    describeInstancesRequest.setFilters(Collections.singleton(filter1));

    DescribeInstancesResult describeInstancesResult = ec2Client.describeInstances(describeInstancesRequest);

    List<Reservation> reservations = describeInstancesResult.getReservations();
    for (Reservation reservation:reservations) {
      List<Instance> instances = reservation.getInstances();
      for (Instance instance:instances) {
        String stackId = getStackId(instance);

        boolean exists = false;
        for(VirtualPrivateGrid vpg: vpgs) {
          if(vpg.getStackId().equals(stackId)) {
            VirtualResource resource = getVirtualResource(vpg, instance, ec2Client);
            resources.add(resource);
            exists = true;
            break;
          }
        }
        if(!exists && !inSyncOnly){
          VirtualPrivateGrid vpg = new VirtualPrivateGrid();
          vpg.setStackId(stackId);
          String userId = getUserId(instance);
          if(userId != null && !userId.isEmpty()) try {
            vpg.setUserId(UUID.fromString(userId));
          } catch (IllegalArgumentException ex){
            // userId is not UUID
          }

          VirtualResource resource = getVirtualResource(vpg, instance, ec2Client);
          resource.setVpgId(null);
          resources.add(resource);
        }
      }
    }
    long end = Calendar.getInstance().getTimeInMillis();
    if(logger.isDebugEnabled()) {
      logger.debug(String.format("getVirtualResources took %.3f seconds.", getSeconds(start,end)));
    }
    return resources;
  }

  @AWSApiCall
  List<VirtualResource> getVirtualResources(VirtualPrivateGrid vpg, String ec2InstanceId, AmazonEC2Client ec2Client) {

    List<VirtualResource> resources = new ArrayList<>();

    DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest();
    describeInstancesRequest.setInstanceIds(Collections.singletonList(ec2InstanceId));
    DescribeInstancesResult describeInstancesResult = ec2Client.describeInstances(describeInstancesRequest);

    List<Reservation> reservations = describeInstancesResult.getReservations();
    for (int reserved = 0; reserved < reservations.size(); reserved++) {
      Reservation reservation = reservations.get(reserved);
      List<Instance> instances = reservation.getInstances();
      for (int instance_location = 0; instance_location < instances.size(); instance_location++) {
        Instance instance = instances.get(instance_location);

        VirtualResource resource = getVirtualResource(vpg, instance, ec2Client);
        resources.add(resource);
      }
    }

    return resources;
  }

  @Override
  public String rdp(VirtualResource resource, String username) {
    String addr = resource.getDnsname() ;
    if (StringUtils.isBlank(addr)) {
      logger.warn("VPGMultiFactoryImpl.rdp: blank dns name in resource, trying to get it from ec2client");
      addr = getCurrentPublicDNSname(resource.getInstanceId());
    }
    if (StringUtils.isBlank(addr)) {
      logger.warn("VPGMultiFactoryImpl.rdp: blank dns name, setting IP address");
      addr = resource.getAddress();
    }
    return rdp(addr, username);
  }

  @Override
  public String rdp(String ipAddress, String username) {
    String rdp = String.format(
            "auto connect:i:1\nfull address:s:%s\n" +
                    "prompt for credentials on client:i:1\nusername:s:PROD\\%s",
            ipAddress,
            username);

    return rdp;
  }

  @Override
  public List<VirtualPrivateGrid> get(UUID user_uuid) {
    TypedQuery<VirtualPrivateGrid> query = emProvider.get().createQuery("from VirtualPrivateGrid where USER_UUID = :uuid", VirtualPrivateGrid.class);
    query.setParameter("uuid", user_uuid);
    List<VirtualPrivateGrid> results = null;
    try {
      results = query.getResultList();
    } catch (NoResultException nre) {
      if (logger.isDebugEnabled()) {
        logger.debug(String.format("No result found for user %s", user_uuid));
      }
    }
    return results;
  }

  @Override
  public VirtualPrivateGrid get(UUID user_uuid, Activity activity) {
    TypedQuery<VirtualPrivateGrid> query = emProvider.get().createQuery("from VirtualPrivateGrid where USER_UUID = :uuid and ACTIVITY_UUID = :auid", VirtualPrivateGrid.class);
    query.setParameter("uuid", user_uuid);
    query.setParameter("auid", activity.getId());
    Object vpg = null;
    try {
      vpg = query.getSingleResult();
    } catch (NoResultException nre) {
      if (logger.isDebugEnabled()) {
        logger.debug(String.format("No result found for user %s", user_uuid));
      }
    }
    return (VirtualPrivateGrid) vpg;
  }

  @Override
  public VirtualPrivateGrid get(UUID user_uuid, UUID activity_uuid) {
    TypedQuery<VirtualPrivateGrid> query = emProvider.get().createQuery("from VirtualPrivateGrid where USER_UUID = :uuid and ACTIVITY_UUID = :auid", VirtualPrivateGrid.class);
    query.setParameter("uuid", user_uuid);
    query.setParameter("auid", activity_uuid);
    Object vpg = null;
    try {
      vpg = query.getSingleResult();
    } catch (NoResultException nre) {
      if (logger.isDebugEnabled()) {
        logger.debug(String.format("No result found for user %s", user_uuid));
      }
    }
    return (VirtualPrivateGrid) vpg;
  }

  @Override
  public VirtualPrivateGrid getByStackId(String stackId) {
    TypedQuery<VirtualPrivateGrid> query =
            emProvider.get().createQuery("from VirtualPrivateGrid where STACK_ID = :stackId",
                    VirtualPrivateGrid.class);

    query.setParameter("stackId", stackId);

    Object vpg = null;
    try {
      vpg = query.getSingleResult();
    } catch (NoResultException nre) {
      if (logger.isDebugEnabled()) {
        logger.debug(String.format("No result found for stack id %s", stackId));
      }
    }
    return (VirtualPrivateGrid) vpg;
  }

  @Override
  public ActivityStatus[] statuses(UUID user_uuid, Activity[] activities) {
    List<ActivityStatus> statuses = new ArrayList<ActivityStatus>();
    long sleep_time = INITIAL_SLEEP_TIME;
    for (int position = 0; position < activities.length; position++) {
      Activity activity = activities[position];
      String status = status(user_uuid, activity);
      ActivityStatus activityStatus = new ActivityStatus();
      activityStatus.setStatus(status);
      activityStatus.setActivityId(activity.getId());

      statuses.add(activityStatus);
    }
    return statuses.toArray(new ActivityStatus[statuses.size()]);
  }

  @Override
  @Transactional
  public String status(UUID user_uuid, Activity activity) {
    VirtualPrivateGrid vpg = get(user_uuid, activity);

    String status = "UNKNOWN";
    try {
      status = getStackStatus(vpg.getStackId());

      if ("CREATE_COMPLETE".equals(status) && StringUtils.isEmpty(vpg.getEc2InstanceIds())) {
        //find ec2 instances and store ids
        List<VirtualResource> virtualResources = getVirtualResources(vpg);
        String instanceIds = vpgFactory.getEc2InstanceIds(virtualResources);
        if (!instanceIds.isEmpty()) {
          vpg.setEc2InstanceIds(instanceIds);
          emProvider.get().merge(vpg);
        }
      }

    } catch (Exception e) {
      logger.error("Unable to retrieve status", e);
    }

    return status;
  }

  @AWSApiCall
  protected String getStackStatus(String stackId) {
    DescribeStacksRequest stackRequest = new DescribeStacksRequest();
    stackRequest.setStackName(stackId);

    AWSCredentialsProvider credentials = new SystemPropertiesCredentialsProvider();
    AmazonCloudFormationClient cloudformationClient = new AmazonCloudFormationClient(credentials.getCredentials());

    DescribeStacksResult stackResults = cloudformationClient.describeStacks(stackRequest);
    List<Stack> stacks = stackResults.getStacks();

    return stacks.get(0).getStackStatus();
  }

  @Override
  public String console(VirtualResource virtualResource) {
    AWSCredentialsProvider credentials = new SystemPropertiesCredentialsProvider();
    AmazonEC2Client ec2Client = new AmazonEC2Client(credentials);

    return Utils.console(virtualResource.getInstanceId(), ec2Client);
  }


  @Override
  @Transactional
  public boolean pause(UUID user_uuid, Activity activity) {
    VirtualPrivateGrid vpg = get(user_uuid, activity);
    if(vpg == null){
      return false;
    } else {
      pause(vpg);
      return true;
    }
  }

  @Override
  @Transactional
  public void pause(VirtualPrivateGrid vpg) {
    pause(vpg, true);
  }

  @Override
  @Transactional
  public void pause(VirtualPrivateGrid vpg, boolean ignoreAutoStopTag) {
    AWSCredentialsProvider credentials = new SystemPropertiesCredentialsProvider();

    AmazonEC2Client amazonEC2Client = new AmazonEC2Client(credentials.getCredentials());

    Set<String> runningInstanceIds = getStackInstancesInSpecifiedState(vpg.getStackId(), "running", amazonEC2Client, ignoreAutoStopTag);

    if (!runningInstanceIds.isEmpty()) {
      StopInstancesRequest stopInstancesRequest = new StopInstancesRequest();
      stopInstancesRequest.setInstanceIds(runningInstanceIds);

      StopInstancesResult stopInstancesResult = amazonEC2Client.stopInstances(stopInstancesRequest);
    }

    //Do not stop autoscaling group and do not create stopped entry.
    if (!ignoreAutoStopTag && runningInstanceIds.isEmpty()) {
      return;
    }

    AutoScalingGroup scalingGroup = getAutoscalingGroupInStack(vpg.getStackId());
    if (scalingGroup != null) {
      vpg.setAutoScaleDesiredInstanceCount(scalingGroup.getDesiredCapacity());
      vpg.setAutoScaleMaxInstanceCount(scalingGroup.getMaxSize());
      vpg.setAutoScaleMinInstanceCount(scalingGroup.getMinSize());
      vpg = emProvider.get().merge(vpg);

      pauseAutoScalingGroup(scalingGroup);
    }

    VirtualPrivateGridStateEntry vpgStateEntry =
            vpgStateFactory.create(vpg.getActivityId(), vpg.getUserId(), vpg.getStackId(),
                    VirtualPrivateGridStateEntry.State.STOPPED);

    emProvider.get().persist(vpgStateEntry); //TODO: there is persist() call inside stateEntry.create() why need more?
    
    cancelMeasurements(vpg); //stop scheduled update of CloudWatch performance stats

    //If there is a pending stop scheduled for the stack, cancel it
    cancelScheduledStopForIdleProvisionedResources(vpg);
  }

  private Set<String> getStackInstancesInSpecifiedState(String stackId, String instanceStateToMatchOn,
                                                        AmazonEC2Client amazonEC2Client) {
    return getStackInstancesInSpecifiedState(stackId, instanceStateToMatchOn, amazonEC2Client, true);
  }

  private Set<String> getStackInstancesInSpecifiedState(String stackId, String instanceStateToMatchOn,
                                                        AmazonEC2Client amazonEC2Client, boolean ignoreAutoStopTag) {

    Set<String> instancesInStack = getEc2InstancesInStack(stackId);
    return getInstancesInSpecifiedState(instancesInStack, instanceStateToMatchOn, amazonEC2Client, ignoreAutoStopTag);
  }

  private Set<String> getEc2InstancesInStack(String stackId) {

    Set<String> instanceIds = new HashSet<String>();

    List<StackResource> stackResources = getStackResources(stackId);
    for (StackResource stackResource : stackResources) {
      if (stackResource.getResourceType().equals("AWS::EC2::Instance")) {
        //Add the instace Id only if it is not empty
        if (stackResource.getPhysicalResourceId() != null) {
          instanceIds.add(stackResource.getPhysicalResourceId());
        }
      }
    }

    return instanceIds;
  }

  private Map<String,String> getEc2InstancesInStack(Set<String> stackIds) {
    long start = Calendar.getInstance().getTimeInMillis();
    Map<String,String> instanceMap = new HashMap<>();
    for(String stackId: stackIds) {
      List<StackResource> stackResources = getStackResources(stackId);
      for(StackResource stackResource: stackResources) {
        if(stackResource.getResourceType().equals("AWS::EC2::Instance")) {
          if(stackResource.getPhysicalResourceId() != null) {
            instanceMap.put(stackResource.getPhysicalResourceId(), stackId);
          }
        }
      }
    }
    long end = Calendar.getInstance().getTimeInMillis();
    if(logger.isDebugEnabled()) {
      logger.debug(String.format("getEc2InstancesInStack took %.3f seconds.", getSeconds(start,end)));
    }
    return instanceMap;
  }

  @AWSApiCall
  List<StackResource> getStackResources(String stackId) {
    AWSCredentialsProvider credentials = new SystemPropertiesCredentialsProvider();
    AmazonCloudFormationClient cloudformationClient = new AmazonCloudFormationClient(credentials.getCredentials());

    long start = Calendar.getInstance().getTimeInMillis();
    DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest();
    describeStacksRequest.setStackName(stackId);
    DescribeStacksResult describeStacksResult = cloudformationClient.describeStacks(describeStacksRequest);
    List<StackResource> stackResources = new ArrayList<>();
    if(describeStacksResult.getStacks().size() == 1) {
      StackStatus stackStatus = StackStatus.fromValue(describeStacksResult.getStacks().get(0).getStackStatus());
      if(stackStatus == StackStatus.CREATE_COMPLETE) {
        DescribeStackResourcesRequest describeStackResources = new DescribeStackResourcesRequest();
        describeStackResources.setStackName(stackId);
        try {
          DescribeStackResourcesResult describeStackResourcesResult = cloudformationClient.describeStackResources(describeStackResources);
          stackResources.addAll(describeStackResourcesResult.getStackResources());
        } catch(AmazonServiceException ase) {
          if(logger.isDebugEnabled()) {
            logger.debug(ase.getMessage(), ase);
          }
        }
      }
    }
    long end = Calendar.getInstance().getTimeInMillis();
    if(logger.isDebugEnabled()) {
      logger.debug(String.format("getStackResources took %.3f seconds.", getSeconds(start,end)));
    }
    return stackResources;
  }

  private Set<String> getInstancesInSpecifiedState(Set<String> instancesToExamine, String instanceStateToMatchOn,
                                                   AmazonEC2Client amazonEC2Client) {
    return getInstancesInSpecifiedState(instancesToExamine, instanceStateToMatchOn, amazonEC2Client, true);
  }

  private Set<String> getInstancesInSpecifiedState(Set<String> instancesToExamine, String instanceStateToMatchOn,
                                                   AmazonEC2Client amazonEC2Client, boolean ignoreAutoStopTag) {

    Set<String> matchedInstanceIds = new HashSet<String>();

    if (!instancesToExamine.isEmpty()) {
      for (String instanceId : instancesToExamine) {
        if (isInstanceInSpecifiedState(instanceId, instanceStateToMatchOn, amazonEC2Client, ignoreAutoStopTag)) {
          matchedInstanceIds.add(instanceId);
        }
      }
    }

    return matchedInstanceIds;
  }

  @AWSApiCall
  boolean isInstanceInSpecifiedState(String instanceId, String instanceStateToMatchOn,
                                     AmazonEC2Client amazonEC2Client) {
    return isInstanceInSpecifiedState(instanceId, instanceStateToMatchOn, amazonEC2Client, true);
  }

  @AWSApiCall
  boolean isInstanceInSpecifiedState(String instanceId, String instanceStateToMatchOn,
                                     AmazonEC2Client amazonEC2Client, boolean ignoreAutoStopTag) {

    DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest();
    describeInstancesRequest.setInstanceIds(Collections.singletonList(instanceId));
    DescribeInstancesResult describeInstancesResult = amazonEC2Client.describeInstances(describeInstancesRequest);
    for (Reservation reservation : describeInstancesResult.getReservations()) {
      for (Instance instance : reservation.getInstances()) {
        if (instance.getInstanceId().equals(instanceId)) {
          String status = instance.getState().getName();
          if (instanceStateToMatchOn.equalsIgnoreCase(status)) {
            //check autostop idle tag
            if (!ignoreAutoStopTag) {
              List<com.amazonaws.services.ec2.model.Tag> tags = instance.getTags();
              if (tags != null) {
                for (com.amazonaws.services.ec2.model.Tag tag : tags) {
                  if ("AutostopIdleResources".equals(tag.getKey()) && "false".equalsIgnoreCase(tag.getValue())) {
                    return false;
                  }
                }
              }
            }
            return true;
          }
        }
      }
    }

    return false;
  }


  @Override
  @Transactional
  public boolean resume(UUID user_uuid, Activity activity) {
    VirtualPrivateGrid vpg = get(user_uuid, activity);
    if(vpg == null){
      return false;
    } else {
      resume(vpg);
      return true;
    }
  }

  @Override
  @Transactional
  public void resume(VirtualPrivateGrid vpg) {
    AWSCredentialsProvider credentials = new SystemPropertiesCredentialsProvider();
    AmazonEC2Client amazonEC2Client = new AmazonEC2Client(credentials.getCredentials());


    Set<String> stoppedInstanceIds = getStackInstancesInSpecifiedState(vpg.getStackId(), "stopped", amazonEC2Client);

    if (!stoppedInstanceIds.isEmpty()) {
      StartInstancesRequest startInstancesRequest = new StartInstancesRequest();
      startInstancesRequest.setInstanceIds(stoppedInstanceIds);
      StartInstancesResult startInstancesResult = amazonEC2Client.startInstances(startInstancesRequest);
    }

    AutoScalingGroup scalingGroup = getAutoscalingGroupInStack(vpg.getStackId());
    if (scalingGroup != null) {
      resumeAutoScalingGroup(scalingGroup, vpg);
    }

    VirtualPrivateGridStateEntry vpgStateEntry =
            vpgStateFactory.create(vpg.getActivityId(), vpg.getUserId(), vpg.getStackId(),
                    VirtualPrivateGridStateEntry.State.RUNNING);

    emProvider.get().persist(vpgStateEntry);
    scheduleMeasurements(vpg); //start polling CloudWatch for perf.metrics and storing data in our DB

    if (!stoppedInstanceIds.isEmpty()) {
      String instanceIdForAutostop = stoppedInstanceIds.iterator().next();
      resumeAutostopMechanismForProvisionedResources(vpg, instanceIdForAutostop);
    }
  }

  private void resumeAutostopMechanismForProvisionedResources(VirtualPrivateGrid vpg, String instanceIdForAutostop) {

    JobInfo jobInfo = activateAutoStopMechanismForProvisionedResources(vpg, instanceIdForAutostop);
    if (jobInfo != null) {
      // Simulate a cloudwatch alarm state change, since this might not happen if the instance is restarted soon after
      // it was stopped. This is needed later by the 'postpone' functionality to locate the stack based on the
      // messageId token in the 'postpone' URL.
      InstanceMonitoringState monitoringState = new InstanceMonitoringState();
      monitoringState.setMessageId(UUID.randomUUID().toString());
      monitoringState.setStackId(vpg.getStackId());
      monitoringState.setInstanceId(instanceIdForAutostop);

      monitoringState.setAlarmDescription("Send notifications regarding Idle Instances provisioned by Analysts");
      monitoringState.setAlarmName("CostControlsMechanisms-CloudWatchIdleInstanceAlarm-SimulatedByProvisioningService");
      monitoringState.setMetric("CPUUtilization");
      monitoringState.setNewStateValue(StateValue.ALARM.name());
      monitoringState.setOldStateValue(StateValue.INSUFFICIENT_DATA.name());
      monitoringState.setMessageDate(new Date());


      StringBuilder buffJsonMessage = new StringBuilder();
      buffJsonMessage.append("{\"AlarmName\":\"").append(monitoringState.getAlarmName()).append("\",")
              .append("\"AlarmDescription\":\"").append(monitoringState.getAlarmDescription()).append("\",")
              .append("\"NewStateValue\":\"").append(monitoringState.getNewStateValue()).append("\",")
              .append("\"OldStateValue\":\"").append(monitoringState.getOldStateValue()).append("\"")
              .append("}");

      monitoringState.setJsonMessage(buffJsonMessage.toString());

      monitoringResourceFactory.create(monitoringState);
    }
  }

  @Transactional
  @Override
  public boolean delete(UUID user_uuid, UUID activity_uuid) {
    VirtualPrivateGrid vpg = get(user_uuid, activity_uuid);
    deleteKey(user_uuid);
    if(vpg == null){
      return false;
    }

    delete(vpg);

    VirtualPrivateGridStateEntry vpgStateEntry = vpgStateFactory.create(activity_uuid, user_uuid, vpg.getStackId(), VirtualPrivateGridStateEntry.State.TERMINATED);
    emProvider.get().persist(vpgStateEntry);
    return true;
  }

  @Transactional
  @Override
  public void delete(VirtualPrivateGrid vpg) {
    cancelMeasurements(vpg);
    deleteStack(vpg.getStackId());
    // delete the entry in the database
    emProvider.get().remove(vpg);
  }

  @AWSApiCall
  void deleteStack(String stackId) {
    AWSCredentialsProvider credentials = new SystemPropertiesCredentialsProvider();
    AmazonCloudFormationClient cloudformationClient = new AmazonCloudFormationClient(credentials.getCredentials());

    DeleteStackRequest stackRequest = new DeleteStackRequest();
    stackRequest.setStackName(stackId);
    // delete the stack
    cloudformationClient.deleteStack(stackRequest);
  }

  @Override
  @AWSApiCall
  public void deleteKey(UUID user_uuid) {
    AWSCredentialsProvider credentials = new SystemPropertiesCredentialsProvider();
    AmazonEC2Client ec2Client = new AmazonEC2Client(credentials);

    // delete the key
    DeleteKeyPairRequest keyPairRequest = new DeleteKeyPairRequest();
    keyPairRequest.setKeyName(user_uuid.toString());
    ec2Client.deleteKeyPair(keyPairRequest);
  }

  @Override
  @Transactional
  public JobInfo activateAutoStopMechanismForProvisionedResources(VirtualPrivateGrid vpg, String instanceIdentifier) {

    StackInfoRetriever stackInfoRetriever = new StackInfoRetriever(vpg.getStackId());
    if (stackInfoRetriever.isAutostopAvailableForStack()) {
      return scheduler.scheduleIdleResourceNotificationJob(vpg.getUserId().toString(), vpg.getStackId(), instanceIdentifier);

    } else {
      logger.info(String.format("Autostop is explicitly disabled for stack <%s>", vpg.getStackId()));
      return null;
    }
  }

  @Override
  @Transactional
  public JobInfo scheduleStopForIdleProvisionedResources(VirtualPrivateGrid vpg) {
    return scheduler.scheduleStopForIdleProvisionedResources(vpg.getUserId().toString(), vpg.getStackId());
  }

  @Override
  @Transactional
  public void postponeScheduledStopForIdleProvisionedResources(VirtualPrivateGrid vpg) {
    scheduler.postponeScheduledStopForIdleProvisionedResources(vpg.getUserId().toString(), vpg.getStackId());
  }

  @Override
  @Transactional
  public void cancelScheduledStopForIdleProvisionedResources(VirtualPrivateGrid vpg) {
    scheduler.cancelScheduledStopForIdleProvisionedResources(vpg.getUserId().toString(), vpg.getStackId());
  }

  @Override
  @Transactional
  public JobInfo scheduleMeasurements(VirtualPrivateGrid vpg) {
    String stackId = vpg.getStackId();
    logger.debug("Scheduling Measurements job for Stack : {}", stackId);
    JobInfo info = measurementScheduler.scheduleMeasurementsUpdate(stackId);
    return info;
  }

  @Override
  @Transactional
  public void cancelMeasurements(VirtualPrivateGrid vpg) {
    String stackId = vpg.getStackId();
    logger.debug("Canceling Measurements job for Stack : {}", stackId);
    measurementScheduler.cancelMeasurementsUpdate(stackId);
  }

  @Override
  @Transactional
  public JobInfo scheduleTerminateForIdleProvisionedResources(VirtualPrivateGrid vpg) {
    return scheduler.scheduleTerminateForIdleProvisionedResources(vpg.getUserId().toString(), vpg.getStackId());
  }
  
  private String getRandomAlphaNumericString(int size) {
    String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    String ret = "";
    int length = chars.length();
    for (int i = 0; i < size; i++) {
      ret += chars.split("")[(int) (Math.random() * (length - 1))];
    }
    return ret;
  }

  @AWSApiCall
  private AutoScalingGroup getAutoscalingGroupInStack(String stackId) {

    List<StackResource> stackResources = getStackResources(stackId);
    for (StackResource stackResource : stackResources) {
      if ("AWS::AutoScaling::AutoScalingGroup".equals(stackResource.getResourceType())) {
        DescribeAutoScalingGroupsRequest request = new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(stackResource.getPhysicalResourceId());
        AWSCredentialsProvider credentials = new SystemPropertiesCredentialsProvider();
        AmazonAutoScalingClient client = new AmazonAutoScalingClient(credentials.getCredentials());
        DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult = client.describeAutoScalingGroups(request);
        List<AutoScalingGroup> autoScalingGroups = describeAutoScalingGroupsResult.getAutoScalingGroups();
        if (autoScalingGroups != null && !autoScalingGroups.isEmpty()) {
          return autoScalingGroups.get(0);
        }
      }
    }
    return null;
  }

  @AWSApiCall
  private void pauseAutoScalingGroup(AutoScalingGroup group) {
    AWSCredentialsProvider credentials = new SystemPropertiesCredentialsProvider();
    AmazonAutoScalingClient client = new AmazonAutoScalingClient(credentials.getCredentials());
    UpdateAutoScalingGroupRequest request = new UpdateAutoScalingGroupRequest()
            .withAutoScalingGroupName(group.getAutoScalingGroupName())
            .withDesiredCapacity(0)
            .withMinSize(0)
            .withMaxSize(0);
    client.updateAutoScalingGroup(request);
  }

  @AWSApiCall
  private void resumeAutoScalingGroup(AutoScalingGroup group, VirtualPrivateGrid vpg) {
    AWSCredentialsProvider credentials = new SystemPropertiesCredentialsProvider();
    AmazonAutoScalingClient client = new AmazonAutoScalingClient(credentials.getCredentials());
    UpdateAutoScalingGroupRequest request = new UpdateAutoScalingGroupRequest()
            .withAutoScalingGroupName(group.getAutoScalingGroupName())
            .withDesiredCapacity(vpg.getAutoScaleDesiredInstanceCount() == null ? 1 : vpg.getAutoScaleDesiredInstanceCount())
            .withMinSize(vpg.getAutoScaleMinInstanceCount() == null ? 1 : vpg.getAutoScaleMinInstanceCount())
            .withMaxSize(vpg.getAutoScaleMaxInstanceCount() == null ? 3 : vpg.getAutoScaleMaxInstanceCount());
    client.updateAutoScalingGroup(request);
  }


  private static class TemplateParameterBuilder {
    private Logger logger = LoggerFactory.getLogger(TemplateParameterBuilder.class);

    private Set<String> allowedParameterKeys;

    private List<Parameter> parameters = new ArrayList<>();


    public TemplateParameterBuilder(Set<String> allowedParameterKeys) {
      this.allowedParameterKeys = allowedParameterKeys;
    }

    public TemplateParameterBuilder withParameter(String parameterKey, String parameterValue) {
      if (allowedParameterKeys.contains(parameterKey)) {
        parameters.add(new Parameter().withParameterKey(parameterKey).withParameterValue(parameterValue));
      } else {
        logger.info(String.format("Ignoring parameter <%s> since the template does not have that parameter defined", parameterKey));
      }
      return this;
    }

    public List<Parameter> getParameters() {
      return parameters;
    }
  }


  @Override
  public String getCurrentPublicDNSname(String instanceId) {
    AWSCredentialsProvider credentials = new SystemPropertiesCredentialsProvider();
    AmazonEC2Client ec2Client = new AmazonEC2Client(credentials);
    DescribeInstancesResult ec2result = ec2Client.describeInstances(new DescribeInstancesRequest().withInstanceIds(instanceId));
    for(Reservation res: ec2result.getReservations()) {
      for (Instance inst: res.getInstances()) {
        if (inst.getInstanceId().equals(instanceId)) {
          String dnsName = inst.getPublicDnsName();
          logger.info("EC2client returned DNS name {} for {} instance {}", dnsName, inst.getState().getName(), instanceId);
          return dnsName;
        }
      }
    }
    logger.warn("EC2client did not find instance {}", instanceId);
    return null;
  }
}
