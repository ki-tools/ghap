package io.ghap.provision.vpg.data;

import io.ghap.provision.vpg.VirtualPrivateGridStateEntry;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public interface VPGStateFactory {

  VirtualPrivateGridStateEntry create(UUID activity_guid,
                                      UUID user_uuid,
                                      String stackId,
                                      VirtualPrivateGridStateEntry.State state);

  List<VirtualPrivateGridStateEntry> get(UUID user_uuid);
  List<VirtualPrivateGridStateEntry> get(UUID user_uuid, Date start, Date end);

  List<VirtualPrivateGridStateEntry> get(UUID user_uuid, UUID activity_guid);
  List<VirtualPrivateGridStateEntry> get(UUID user_uuid, UUID activity_guid, Date start, Date end);

  List<VirtualPrivateGridStateEntry> get(String stackId);
  List<VirtualPrivateGridStateEntry> get(String stackId, Date start, Date end);

  List<VirtualPrivateGridStateEntry> get();
  List<VirtualPrivateGridStateEntry> get(Date start, Date end);

  List<String> getStackNames();
  List<String> getStackNames(Date start, Date end);

  List<UUID> getUsers();
  List<UUID> getUsers(Date start, Date end);

  List<UUID> getActivities();
  List<UUID> getActivities(UUID user_uuid);
  List<UUID> getActivities(Date start, Date end);
  List<UUID> getActivities(UUID user_uuid, Date start, Date end);

  Date getCreationDate(String stackId, UUID userId);

  VirtualPrivateGridStateEntry getActiveState(String stackId, UUID userId, Date limit);
  
}
