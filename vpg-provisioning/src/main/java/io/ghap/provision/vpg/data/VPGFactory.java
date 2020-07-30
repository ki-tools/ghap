package io.ghap.provision.vpg.data;

import io.ghap.provision.vpg.Activity;
import io.ghap.provision.vpg.VirtualPrivateGrid;

import java.util.List;
import java.util.UUID;

public interface VPGFactory
{
  VirtualPrivateGrid create(String username, UUID user_uuid, Activity activity);

  boolean exists(UUID user_uuid);

  List<VirtualPrivateGrid> get(); 
  VirtualPrivateGrid get(UUID user_uuid);
  VirtualPrivateGrid getByVpgId(UUID vpgid);
  List<VirtualResource> getVirtualResources(VirtualPrivateGrid vpg);

  String rdp(VirtualResource resource, String username);

  String status(UUID user_uuid);

  void pause(UUID user_uuid);
  void resume(UUID user_uuid);

  void delete(UUID user_uuid);

  String getEc2InstanceIds(List<VirtualResource> virtualResources);
}
