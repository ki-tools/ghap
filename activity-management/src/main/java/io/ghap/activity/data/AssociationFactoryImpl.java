package io.ghap.activity.data;

import io.ghap.activity.Activity;
import io.ghap.activity.ActivityRoleAssociation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;

public class AssociationFactoryImpl implements AssociationFactory
{
  @Inject Provider<EntityManager> emProvider;
  
  @Override
  @Transactional
  public Set<ActivityRoleAssociation> associate(UUID role_uuid,
                                                Activity... activities)
  {
    Set<ActivityRoleAssociation> associations = new HashSet<ActivityRoleAssociation>();
    
    for(Activity activity: activities) {
      ActivityRoleAssociation association = new ActivityRoleAssociation();
      association.setRoleId(role_uuid);
      association.setActivity(activity.getId());
      emProvider.get().persist(association);
      associations.add(association);
    }
    return associations;
  }

  public List<ActivityRoleAssociation> get(UUID role_uuid)
  {
    TypedQuery<ActivityRoleAssociation> query = 
        emProvider.get().createQuery("from ActivityRoleAssociation c where c.role_uuid = :uuid", ActivityRoleAssociation.class);
    query.setParameter("uuid", role_uuid);
    List<ActivityRoleAssociation> results = query.getResultList(); 
    return results;
  }
  
  @Override
  @Transactional
  public void delete(ActivityRoleAssociation... associations)
  {
    for(ActivityRoleAssociation association : associations) 
    {
      emProvider.get().remove(association);
    }
  }
  
  @Override
  @Transactional
  public void delete(UUID role_uuid)
  {
    List<ActivityRoleAssociation> associations = get(role_uuid);
    for(ActivityRoleAssociation association : associations) 
    {
      emProvider.get().remove(association);
    }
  }
}
