package io.ghap.provision.vpg.data;

import com.google.inject.Singleton;
import io.ghap.provision.vpg.Activity;
import io.ghap.provision.vpg.VirtualPrivateGrid;
import io.ghap.provision.vpg.scheduler.JobInfo;

import java.util.*;

@Singleton
public class VPGMultiFactoryMockImpl implements VPGMultiFactory {

  public final static String STACK_ID = "arn:aws:cloudformation:us-east-1:931961322697:stack/jOqgVILqyYspOPVzAzqrDJcIprcCTAhFPYAdldxKbJvar/%s";
  public final static String PEM_KEY = "-----BEGIN RSA PRIVATE KEY-----\n" +
      " Some cert goes here     \n" +
      "-----END RSA PRIVATE KEY-----\n";

  public final static String RDP = "auto connect:i:1\n" +
      "full address:s:52.23.253.79\n" +
      "prompt for credentials on client:i:1\n" +
      "username:s:PROD\\stephan.nagy";

  private List<VirtualPrivateGrid> envs = new ArrayList<>();
  private static final String TEMPLATE_ANALYSIS_VPG_ACTIVITY = "https://s3.amazonaws.com/ghap-provisioning-templates-us-east-1-devtest/analysis-vpg-activity.json";
  private static final String TEMPLATE_ANALYSIS_WINDOW_ACTIVITY_TEST_V8E = "https://s3.amazonaws.com/ghap-provisioning-templates-us-east-1-devtest/analysis-windows-activity_testv8e.json";
  private List<VirtualResource> resources = new ArrayList<>();

  @Override
  public VirtualPrivateGrid create(String username, String email, UUID user_uuid, Activity activity) {
    return create(username, email, user_uuid, new Activity[]{activity})[0];
  }

  @Override
  public VirtualPrivateGrid[] create(String username, String email, UUID user_uuid, Activity[] activities) {
    List<VirtualPrivateGrid> vpgs = new ArrayList<>();
    for (Activity activity : activities) {
      VirtualPrivateGrid vpg = new VirtualPrivateGrid();
      vpg.setActivityId(activity.getId());
      vpg.setStackId(String.format(STACK_ID, UUID.randomUUID().toString()));
      vpg.setId(UUID.randomUUID());
      vpg.setUserId(user_uuid);
      vpg.setPemKey(PEM_KEY);
      vpgs.add(vpg);

      if(activity.getTemplateUrl().equals(TEMPLATE_ANALYSIS_VPG_ACTIVITY)) {
        VirtualResource virtualResourceOne = new VirtualResource();
        virtualResourceOne.setStatus("running");
        virtualResourceOne.setAddress("10.10.12.2");
        virtualResourceOne.setStackId(vpg.getStackId());
        virtualResourceOne.setCoreCount(4);
        virtualResourceOne.setInstanceId("i-12345");
        virtualResourceOne.setVpgId(vpg.getId());
        virtualResourceOne.setInstanceOsType("Linux");

        resources.add(virtualResourceOne);

        VirtualResource virtualResourceTwo = new VirtualResource();
        virtualResourceTwo.setStatus("running");
        virtualResourceTwo.setAddress("10.10.12.3");
        virtualResourceTwo.setStackId(vpg.getStackId());
        virtualResourceTwo.setCoreCount(4);
        virtualResourceTwo.setInstanceId("i-23456");
        virtualResourceTwo.setVpgId(vpg.getId());
        virtualResourceTwo.setInstanceOsType("Linux");

        resources.add(virtualResourceTwo);
      } else {
        VirtualResource virtualResource = new VirtualResource();
        virtualResource.setStatus("running");
        virtualResource.setAddress("10.10.12.1");
        virtualResource.setStackId(vpg.getStackId());
        virtualResource.setCoreCount(4);
        virtualResource.setInstanceId("i-12345");
        virtualResource.setVpgId(vpg.getId());
        virtualResource.setInstanceOsType("Windows");
        resources.add(virtualResource);
      }
    }
    envs.addAll(vpgs);
    return envs.toArray(new VirtualPrivateGrid[envs.size()]);
  }

  @Override
  public List<VirtualPrivateGrid> get() {
    return envs;
  }

  @Override
  public List<VirtualPrivateGrid> get(UUID uuid) {
    List<VirtualPrivateGrid> vpgs = new ArrayList<>();
    for(VirtualPrivateGrid env: envs) {
      if(env.getUserId().equals(uuid)) {
        vpgs.add(env);
      }
    }
    return vpgs;
  }

  @Override
  public VirtualPrivateGrid getByVpgId(UUID vpgid) {
    for(VirtualPrivateGrid env: envs) {
      if(env.getId().equals(vpgid)) { return env; }
    }
    return null;
  }

  @Override
  public List<VirtualResource> getVirtualResources(VirtualPrivateGrid vpg) {
    List<VirtualResource> filteredResources = new ArrayList<>();
    for(VirtualResource resource: resources) {
      if(resource.getVpgId().equals(vpg.getId())) {
        filteredResources.add(resource);
      }
    }
    return filteredResources;
  }

  @Override
  public List<VirtualResource> getVirtualResources(UUID user_uuid) {
    List<VirtualResource> filteredResources = new ArrayList<>();
    for(VirtualResource resource: resources) {
      VirtualPrivateGrid vpg = getByVpgId(resource.getVpgId());
      if(vpg.getUserId().equals(user_uuid)) {
        filteredResources.add(resource);
      }
    }
    return filteredResources;
  }

  @Override
  public VirtualPrivateGrid get(UUID user_uuid, Activity activity) {
    for(VirtualPrivateGrid env: envs) {
      if(env.getUserId().equals(user_uuid) && env.getActivityId().equals(activity.getId())) { return env; }
    }
    return null;
  }

  @Override
  public VirtualPrivateGrid get(UUID user_uuid, UUID activity_uid) {
    for(VirtualPrivateGrid env: envs) {
      if(env.getUserId().equals(user_uuid) && env.getActivityId().equals(activity_uid)) { return env; }
    }
    return null;
  }

  @Override
  public VirtualPrivateGrid getByStackId(String stackId) {
    for(VirtualPrivateGrid env: envs) {
      if(env.getStackId().equals(stackId)) { return env; }
    }
    return null;
  }

  @Override
  public String rdp(VirtualResource resource, String username) {
    return RDP;
  }

  @Override
  public String rdp(String ipAddress, String username) {
    return RDP;
  }

  @Override
  public boolean exists(UUID user_uuid, Activity activity) {
    return get(user_uuid, activity) != null;
  }

  @Override
  public ActivityExistence[] existences(UUID user_uuid, Activity[] activities) {
    List<ActivityExistence> activityExistences = new ArrayList<>();
    for(Activity activity: activities) {
      ActivityExistence activityExistence = new ActivityExistence();
      activityExistence.setActivityId(activity.getId());
      activityExistence.setExistence(exists(user_uuid, activity));
      activityExistences.add(activityExistence);
    }
    return activityExistences.toArray(new ActivityExistence[activityExistences.size()]);
  }

  @Override
  public String status(UUID user_uuid, Activity activity) {
    return "CREATE_COMPLETE";
  }

  @Override
  public ActivityStatus[] statuses(UUID user_uuid, Activity[] activities) {
    List<ActivityStatus> statuses = new ArrayList<>();
    for(Activity activity: activities) {
      VirtualPrivateGrid grid = get(user_uuid, activity);
      ActivityStatus status = new ActivityStatus();
      status.setActivityId(activity.getId());
      status.setStatus(status(user_uuid, activity));
      statuses.add(status);
    }
    return statuses.toArray(new ActivityStatus[statuses.size()]);
  }

  @Override
  public String console(VirtualResource virtualResource) {
    return "windows is ready to use";
  }

  @Override
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
  public void pause(VirtualPrivateGrid vpg) {
    pause(vpg, true);
  }

  @Override
  public void pause(VirtualPrivateGrid vpg, boolean ignoreAutoStopTag) {
    List<VirtualResource> resources = getVirtualResources(vpg);
    for(VirtualResource resource: resources) {
      resource.setStatus("stopped");
    }
  }

  @Override
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
  public void resume(VirtualPrivateGrid vpg) {
    List<VirtualResource> resources = getVirtualResources(vpg);
    for(VirtualResource resource: resources) {
      resource.setStatus("running");
    }
  }

  @Override
  public boolean delete(UUID user_uuid, UUID activity_uuid) {
    VirtualPrivateGrid vpg = get(user_uuid, activity_uuid);
    if(vpg == null){
      return false;
    } else {
      delete(vpg);
      return true;
    }
  }

  @Override
  public void delete(VirtualPrivateGrid vpg) {
    List<VirtualResource> removeResources = getVirtualResources(vpg);
    resources.removeAll(removeResources);
    envs.remove(vpg);
  }

  @Override
  public void deleteKey(UUID user_uuid) {
    // no op
  }

  @Override
  public JobInfo activateAutoStopMechanismForProvisionedResources(VirtualPrivateGrid vpg, String instanceIdentifier) {
    return null;
  }

  @Override
  public JobInfo scheduleStopForIdleProvisionedResources(VirtualPrivateGrid vpg) {
    return null;
  }

  @Override
  public void postponeScheduledStopForIdleProvisionedResources(VirtualPrivateGrid vpg) {

  }

  @Override
  public void cancelScheduledStopForIdleProvisionedResources(VirtualPrivateGrid vpg) {

  }

  @Override
  public JobInfo scheduleMeasurements(VirtualPrivateGrid vpg) {
    return null;
  }

  @Override
  public void cancelMeasurements(VirtualPrivateGrid vpg) {

  }

  @Override
  public List<VirtualResource> getVirtualResources(boolean inSyncOnly) {
    return null;
  }

  @Override
  public List<VirtualResource> getVirtualResources(Set<VirtualPrivateGrid> vpgs, boolean inSyncOnly) {
    return null;
  }

  @Override
  public JobInfo scheduleTerminateForIdleProvisionedResources(VirtualPrivateGrid vpg) {
    return null;
  }

  public void cleanup()
  {
    envs = null;
    resources = null;
  }

  public void setup()
  {
    envs = new ArrayList<>();
    resources = new ArrayList<>();
  }

  @Override
  public String getCurrentPublicDNSname(String instanceId) {
    return null;
  }
}
