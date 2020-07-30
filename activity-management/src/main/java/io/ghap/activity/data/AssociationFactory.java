package io.ghap.activity.data;

import io.ghap.activity.Activity;
import io.ghap.activity.ActivityRoleAssociation;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface AssociationFactory
{
  Set<ActivityRoleAssociation> associate(UUID role_uuid, Activity... activities);
  void delete(ActivityRoleAssociation... associations);
  List<ActivityRoleAssociation> get(UUID role_uuid);
  void delete(UUID role_uuid);
}
