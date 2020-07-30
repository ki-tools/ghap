package io.ghap.provision.vpg.data;

import com.google.inject.persist.Transactional;
import io.ghap.provision.vpg.Activity;
import io.ghap.provision.vpg.VirtualPrivateGrid;
import io.ghap.provision.vpg.scheduler.JobInfo;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface VPGMultiFactory {
  VirtualPrivateGrid create(String username, String email, UUID user_uuid, Activity activity);

  VirtualPrivateGrid[] create(String username, String email, UUID user_uuid, Activity[] activities);

  List<VirtualPrivateGrid> get();

  List<VirtualPrivateGrid> get(UUID uuid);

  VirtualPrivateGrid getByVpgId(UUID vpgid);

  List<VirtualResource> getVirtualResources(VirtualPrivateGrid vpg);

  List<VirtualResource> getVirtualResources(UUID user_uuid);

  List<VirtualResource> getVirtualResources(Set<VirtualPrivateGrid> vpgs, boolean inSyncOnly);

  List<VirtualResource> getVirtualResources(boolean inSyncOnly);

  VirtualPrivateGrid get(UUID user_uuid, Activity activity);

  VirtualPrivateGrid get(UUID user_uuid, UUID activity_uid);

  VirtualPrivateGrid getByStackId(String stackId);

  String rdp(VirtualResource resource, String username);

  String rdp(String ipAddress, String username);

  boolean exists(UUID user_uuid, Activity activity);

  ActivityExistence[] existences(UUID user_uuid, Activity[] activities);

  String status(UUID user_uuid, Activity activity);

  ActivityStatus[] statuses(UUID user_uuid, Activity[] activities);

  String console(VirtualResource virtualResource);

  boolean pause(UUID user_uuid, Activity activity);

  void pause(VirtualPrivateGrid vpg);

  @Transactional
  void pause(VirtualPrivateGrid vpg, boolean ignoreAutoStopTag);

  boolean resume(UUID user_uuid, Activity activity);

  void resume(VirtualPrivateGrid vpg);


  boolean delete(UUID user_uuid, UUID activity_uuid);

  void delete(VirtualPrivateGrid vpg);

  void deleteKey(UUID user_uuid);

  JobInfo activateAutoStopMechanismForProvisionedResources(VirtualPrivateGrid vpg, String instanceIdentifier);

  JobInfo scheduleStopForIdleProvisionedResources(VirtualPrivateGrid vpg);

  void postponeScheduledStopForIdleProvisionedResources(VirtualPrivateGrid vpg);

  void cancelScheduledStopForIdleProvisionedResources(VirtualPrivateGrid vpg);

  JobInfo scheduleMeasurements(VirtualPrivateGrid vpg);

  void cancelMeasurements(VirtualPrivateGrid vpg);

  JobInfo scheduleTerminateForIdleProvisionedResources(VirtualPrivateGrid vpg);

  String getCurrentPublicDNSname(String instanceId);
  
}
