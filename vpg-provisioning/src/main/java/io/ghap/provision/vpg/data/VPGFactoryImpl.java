package io.ghap.provision.vpg.data;

import com.amazonaws.services.cloudformation.model.*;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.Tag;
import com.amazonaws.services.ec2.model.*;
import com.netflix.config.ConfigurationManager;
import io.ghap.provision.vpg.Activity;
import io.ghap.provision.vpg.PersonalStorage;
import io.ghap.provision.vpg.VirtualPrivateGrid;

import java.util.*;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;
import com.netflix.governator.annotations.Configuration;
import io.ghap.provision.vpg.VirtualPrivateGridStateEntry;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VPGFactoryImpl implements VPGFactory
{
  private Logger logger = LoggerFactory.getLogger(VPGFactoryImpl.class);

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

  @Transactional
  @Override
  public VirtualPrivateGrid create(String username, UUID user_uuid, Activity activity)
  {
    if(exists(user_uuid)) 
    {
      throw new UnsupportedOperationException();
    }
    AWSCredentialsProvider credentials = new SystemPropertiesCredentialsProvider();
    AmazonEC2Client ec2Client = new AmazonEC2Client(credentials);

    // make sure the user doesn't already have a key pair, if they do we delete it,
    // this could orphan any instances using this key pair, but as they can only
    // have one it shouldn't be an issue.
    try {
      DescribeKeyPairsRequest describeKeyPairRequest = new DescribeKeyPairsRequest();
      describeKeyPairRequest.setKeyNames(Arrays.asList(new String[]{user_uuid.toString()}));
      DescribeKeyPairsResult descriptionKeyPairs = ec2Client.describeKeyPairs(describeKeyPairRequest);
      if (descriptionKeyPairs.getKeyPairs().size() > 0) {
        DeleteKeyPairRequest deleteKeyPairRequest = new DeleteKeyPairRequest();
        deleteKeyPairRequest.setKeyName(user_uuid.toString());
        ec2Client.deleteKeyPair(deleteKeyPairRequest);
      }
    }
    catch(Exception e)
    {
      // don't really care eat the exception
    }
    // first we create a new key pair to be used by this stack, this is unique to every
    // stack that gets created.
    CreateKeyPairRequest keyPairRequest = new CreateKeyPairRequest();
    keyPairRequest.setKeyName(user_uuid.toString());
    
    CreateKeyPairResult keyPairResult = ec2Client.createKeyPair(keyPairRequest);
    KeyPair keyPair = keyPairResult.getKeyPair();
    
    // standup the stack
    AmazonCloudFormationClient cloudformationClient = new AmazonCloudFormationClient(credentials.getCredentials());
    CreateStackRequest stackRequest = new CreateStackRequest();
    stackRequest.setCapabilities(Arrays.asList(new String[] {"CAPABILITY_IAM"}));

    //validate the template, and get the parameters
    Set<String> templateParameterKeys = getTemplateParameterKeys(cloudformationClient, activity.getTemplateUrl());

    String stack_name = getRandomAlphaNumericString(45);
    List<Tag> tags = new ArrayList<Tag>();
    tags.add(new Tag().withKey("UUID").withValue(user_uuid.toString()));
    stackRequest.setTags(tags);
    stackRequest.setStackName(stack_name);
    stackRequest.setOnFailure(OnFailure.DELETE);
    stackRequest.setTemplateURL(activity.getTemplateUrl());

    String email = null;

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
      templateParameterBuilder.withParameter("SQSRole", idleResourcesSNSTopicArn);
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
    vpgStateFactory.create(activity.getId(), user_uuid, vpg.getStackId(), VirtualPrivateGridStateEntry.State.CREATED);
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

  @Override
  @Transactional
  public boolean exists(UUID user_uuid)
  {
    if(logger.isDebugEnabled()) { logger.debug(String.format("Checking if %s has a virtual environment.", user_uuid)); }

    try 
    {
      VirtualPrivateGrid vpg = get(user_uuid);
      if(logger.isDebugEnabled()) { logger.debug(String.format("%s has a virtual environment: %b", user_uuid, vpg)); }
      if(vpg != null)
      {
        AWSCredentialsProvider credentials = new SystemPropertiesCredentialsProvider();
        AmazonCloudFormationClient cloudformationClient = new AmazonCloudFormationClient(credentials.getCredentials());

        DescribeStacksRequest stackRequest = new DescribeStacksRequest();
        stackRequest.setStackName(vpg.getStackId());

        if(logger.isDebugEnabled()) { logger.debug("Describing Stacks"); }

        DescribeStacksResult stackResults = cloudformationClient.describeStacks(stackRequest);
        List<Stack> stacks = stackResults.getStacks();

        if(logger.isDebugEnabled()) { logger.debug(String.format("%s has %d stacks", user_uuid, stacks.size())); }
        if(stacks.size() == 1)
        {
          Stack stack = stacks.get(0); // there can be only one
          StackStatus status = StackStatus.fromValue(stack.getStackStatus());
          if(logger.isDebugEnabled())
          {
            logger.debug(String.format("%s status for %s virtual environment.", stack.getStackStatus(), user_uuid));
            logger.debug(String.format("%s has a stack name of %s", vpg.getStackId(), stack.getStackName()));
          }
          if(status == StackStatus.CREATE_IN_PROGRESS || status == StackStatus.CREATE_COMPLETE || status == StackStatus.UPDATE_COMPLETE || status == StackStatus.UPDATE_IN_PROGRESS)
          {
            if(logger.isDebugEnabled()) { logger.debug(String.format("Returning true for %s", user_uuid)); }
            return true;
          }
          else
          {
            // delete the entry in the database
            if(logger.isDebugEnabled()) { logger.debug(String.format("Invalid stack for %s deleting it from the database.", user_uuid)); }
            emProvider.get().remove(vpg);
          }
        }
      }
      if(logger.isDebugEnabled()) { logger.debug(String.format("Returning false for %s", user_uuid)); }
      return false;
    } 
    catch(Exception e)
    {
      logger.error(e.getMessage(), e);
      if(logger.isDebugEnabled()) { logger.debug(String.format("Returning false for %s", user_uuid)); }
      return false;
    }
  }

  @Override
  public List<VirtualPrivateGrid> get()
  {
    Query query = emProvider.get().createQuery("from VirtualPrivateGrid c");
    List<VirtualPrivateGrid> results = query.getResultList();
    return results;
  }

  @Override
  public VirtualPrivateGrid get(UUID user_uuid)
  {
    TypedQuery<VirtualPrivateGrid> query = emProvider.get().createQuery("from VirtualPrivateGrid where USER_UUID = :uuid", VirtualPrivateGrid.class);
    query.setParameter("uuid", user_uuid);
    Object vpg = null;
    try
    {
      vpg = query.getSingleResult();
    }
    catch(NoResultException nre)
    {
      if(logger.isDebugEnabled())
      {
        logger.debug(String.format("No result found for user %s", user_uuid));
      }
    }
    return (VirtualPrivateGrid)vpg;
  }

  @Override
  public VirtualPrivateGrid getByVpgId(UUID vpgid)
  {
    TypedQuery<VirtualPrivateGrid> query = emProvider.get().createQuery("from VirtualPrivateGrid where id = :vpgid", VirtualPrivateGrid.class);
    query.setParameter("vpgid", vpgid);
    Object vpg = null;
    try
    {
      vpg = query.getSingleResult();
    }
    catch(NoResultException nre)
    {
      if(logger.isDebugEnabled())
      {
        logger.debug(String.format("No result found for vpg %s", vpgid));
      }
    }
    return (VirtualPrivateGrid)vpg;
  }

  @Override
  @Transactional
  public String status(UUID user_uuid)
  {
    VirtualPrivateGrid vpg = get(user_uuid);
    DescribeStacksRequest stackRequest = new DescribeStacksRequest();
    stackRequest.setStackName(vpg.getStackId());

    AWSCredentialsProvider credentials = new SystemPropertiesCredentialsProvider();
    AmazonCloudFormationClient cloudformationClient = new AmazonCloudFormationClient(credentials.getCredentials());

    DescribeStacksResult stackResults = cloudformationClient.describeStacks(stackRequest);
      List<Stack> stacks = stackResults.getStacks();

    String stackStatus = stacks.get(0).getStackStatus();
    if ("CREATE_COMPLETE".equals(stackStatus) && StringUtils.isEmpty(vpg.getEc2InstanceIds())) {
      //find ec2 instances and store ids
      List<VirtualResource> virtualResources = getVirtualResources(vpg);
      String instanceIds = getEc2InstanceIds(virtualResources);
      if (!instanceIds.isEmpty()) {
        vpg.setEc2InstanceIds(instanceIds);
        emProvider.get().merge(vpg);
      }
    }
    return stackStatus;

  }

  @Override
  public List<VirtualResource> getVirtualResources(VirtualPrivateGrid vpg)
  {
    List<VirtualResource> resources = new ArrayList<VirtualResource>();

    DescribeStackResourcesRequest describeStackResources = new DescribeStackResourcesRequest();
    describeStackResources.setStackName(vpg.getStackId());
    AWSCredentialsProvider credentials = new SystemPropertiesCredentialsProvider();

    AmazonCloudFormationClient cloudformationClient = new AmazonCloudFormationClient(credentials.getCredentials());
    DescribeStackResourcesResult stackResourcesResult = cloudformationClient.describeStackResources(describeStackResources);
    for(StackResource stackResource : stackResourcesResult.getStackResources())
    {
      if(stackResource.getResourceType().equals("AWS::EC2::Instance")) {
        VirtualResource resource = new VirtualResource();
        resource.setInstanceId(stackResource.getPhysicalResourceId());
        AmazonEC2Client ec2Client = new AmazonEC2Client(credentials);

        DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest();
        describeInstancesRequest.setInstanceIds(Arrays.asList(new String[]{resource.getInstanceId()}));
        DescribeInstancesResult describeInstancesResult = ec2Client.describeInstances(describeInstancesRequest);
        for (Reservation reservation : describeInstancesResult.getReservations()) {
          for (Instance instance : reservation.getInstances()) {
            int cores = ConfigurationManager.getConfigInstance().getInt(instance.getInstanceType(), 0);
            resource.setCoreCount(cores);
            resource.setAddress(instance.getPublicIpAddress());
            resource.setDnsname(instance.getPublicDnsName());
            resource.setInstanceOsType(instance.getPlatform() == null ? "Linux" : instance.getPlatform());
            String name = instance.getState().getName();
            resource.setStatus(name);
            if ("running".equalsIgnoreCase(name)) {
              try {
                //additional checks. instance may be initializing
                AmazonEC2Client amazonEC2Client = new AmazonEC2Client(credentials.getCredentials());
                DescribeInstanceStatusRequest describeInstanceStatusRequest = new DescribeInstanceStatusRequest();
                describeInstanceStatusRequest.setInstanceIds(Arrays.asList(instance.getInstanceId()));
                DescribeInstanceStatusResult result = amazonEC2Client.describeInstanceStatus(describeInstanceStatusRequest);
                InstanceStatusSummary instanceStatusSummary = result.getInstanceStatuses().get(0).getInstanceStatus();

                if (!"ok".equalsIgnoreCase(instanceStatusSummary.getStatus())) {
                  resource.setStatus(instanceStatusSummary.getStatus());
                }
              } catch (Throwable e) {
                logger.error("can't read instance status", e);
              }
            }
            resources.add(resource);
          }
        }
      }
    }
    return resources;
  }

  @Transactional
  @Override
  public void delete(UUID user_uuid)
  {
    AWSCredentialsProvider credentials = new SystemPropertiesCredentialsProvider();
    
    AmazonCloudFormationClient cloudformationClient = new AmazonCloudFormationClient(credentials.getCredentials());
    VirtualPrivateGrid vpg = get(user_uuid);
    DeleteStackRequest stackRequest = new DeleteStackRequest();
    stackRequest.setStackName(vpg.getStackId());
    // delete the stack
    cloudformationClient.deleteStack(stackRequest);
    // delete the key
    DeleteKeyPairRequest keyPairRequest = new DeleteKeyPairRequest();
    keyPairRequest.setKeyName(user_uuid.toString());
    
    AmazonEC2Client ec2Client = new AmazonEC2Client(credentials);
    ec2Client.deleteKeyPair(keyPairRequest);
    
    // delete the entry in the database
    emProvider.get().remove(vpg);
    vpgStateFactory.create(vpg.getActivityId(), user_uuid, vpg.getStackId(), VirtualPrivateGridStateEntry.State.TERMINATED);
  }

  @Override
  public void pause(UUID user_uuid)
  {
    AWSCredentialsProvider credentials = new SystemPropertiesCredentialsProvider();
    AmazonCloudFormationClient cloudformationClient = new AmazonCloudFormationClient(credentials.getCredentials());
    VirtualPrivateGrid vpg = get(user_uuid);

    DescribeStackResourcesRequest describeStackResources = new DescribeStackResourcesRequest();
    describeStackResources.setStackName(vpg.getStackId());

    DescribeStackResourcesResult stackResourcesResult = cloudformationClient.describeStackResources(describeStackResources);
    Set<String> instanceIds = new HashSet<String>();

    for(StackResource stackResource : stackResourcesResult.getStackResources())
    {
      if(stackResource.getResourceType().equals("AWS::EC2::Instance")) {
        VirtualResource resource = new VirtualResource();
        resource.setInstanceId(stackResource.getPhysicalResourceId());
        AmazonEC2Client ec2Client = new AmazonEC2Client(credentials);

        DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest();
        describeInstancesRequest.setInstanceIds(Arrays.asList(new String[]{resource.getInstanceId()}));
        DescribeInstancesResult describeInstancesResult = ec2Client.describeInstances(describeInstancesRequest);
        for (Reservation reservation : describeInstancesResult.getReservations()) {
          for (Instance instance : reservation.getInstances()) {
            String status = instance.getState().getName();
            if ("running".equalsIgnoreCase(status)) {
              instanceIds.add(instance.getInstanceId());
            }
          }
        }
      }
    }
    AmazonEC2Client amazonEC2Client = new AmazonEC2Client(credentials.getCredentials());
    StopInstancesRequest stopInstancesRequest = new StopInstancesRequest();
    stopInstancesRequest.setInstanceIds(instanceIds);
    StopInstancesResult stopInstancesResult = amazonEC2Client.stopInstances(stopInstancesRequest);
    vpgStateFactory.create(vpg.getActivityId(), user_uuid, vpg.getStackId(), VirtualPrivateGridStateEntry.State.STOPPED);
  }

  @Override
  public String rdp(VirtualResource resource, String username)
  {
    String addr = resource.getDnsname() ;
    if (StringUtils.isBlank(addr)) {
     addr = resource.getAddress();
    }
    String rdp = String.format(
        "auto connect:i:1\nfull address:s:%s\n" +
            "prompt for credentials on client:i:1\nusername:s:PROD\\%s",
        addr,
        username);

    return rdp;
  }

  @Override
  public void resume(UUID user_uuid)
  {
    AWSCredentialsProvider credentials = new SystemPropertiesCredentialsProvider();
    AmazonCloudFormationClient cloudformationClient = new AmazonCloudFormationClient(credentials.getCredentials());
    VirtualPrivateGrid vpg = get(user_uuid);

    DescribeStackResourcesRequest describeStackResources = new DescribeStackResourcesRequest();
    describeStackResources.setStackName(vpg.getStackId());

    DescribeStackResourcesResult stackResourcesResult = cloudformationClient.describeStackResources(describeStackResources);
    Set<String> instanceIds = new HashSet<String>();

    for(StackResource stackResource : stackResourcesResult.getStackResources()) {
      if (stackResource.getResourceType().equals("AWS::EC2::Instance")) {
        VirtualResource resource = new VirtualResource();
        resource.setInstanceId(stackResource.getPhysicalResourceId());
        AmazonEC2Client ec2Client = new AmazonEC2Client(credentials);

        DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest();
        describeInstancesRequest.setInstanceIds(Arrays.asList(new String[]{resource.getInstanceId()}));
        DescribeInstancesResult describeInstancesResult = ec2Client.describeInstances(describeInstancesRequest);
        for (Reservation reservation : describeInstancesResult.getReservations()) {
          for (Instance instance : reservation.getInstances()) {
            String status = instance.getState().getName();
            if ("stopped".equalsIgnoreCase(status)) {
              instanceIds.add(instance.getInstanceId());
            }
          }
        }
      }
    }
    AmazonEC2Client amazonEC2Client = new AmazonEC2Client(credentials.getCredentials());
    StartInstancesRequest startInstancesRequest = new StartInstancesRequest();
    startInstancesRequest.setInstanceIds(instanceIds);
    StartInstancesResult startInstancesResult = amazonEC2Client.startInstances(startInstancesRequest);
    vpgStateFactory.create(vpg.getActivityId(), user_uuid, vpg.getStackId(), VirtualPrivateGridStateEntry.State.RUNNING);
  }

  @Override
  public String getEc2InstanceIds(List<VirtualResource> virtualResources) {
    if (virtualResources != null && !virtualResources.isEmpty()) {
      StringBuilder sb = new StringBuilder();
      for (Iterator<VirtualResource> it = virtualResources.iterator(); it.hasNext(); ) {
        sb.append(it.next().getInstanceId());
        if (it.hasNext()) {
          sb.append(",");
        }
      }
      return sb.toString();
    }
    return StringUtils.EMPTY;
  }

  private String getRandomAlphaNumericString(int size){
    String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    String ret = "";
    int length = chars.length();
    for (int i = 0; i < size; i ++){
        ret += chars.split("")[ (int) (Math.random() * (length - 1)) ];
    }
    return ret;
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

}
