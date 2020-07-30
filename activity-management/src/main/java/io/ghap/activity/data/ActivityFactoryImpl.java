package io.ghap.activity.data;

import io.ghap.activity.Activity;

import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;

public class ActivityFactoryImpl implements ActivityFactory
{
  @Inject Provider<EntityManager> emProvider;
  
  @Transactional
  public Activity create(String activityName,
                                int defaultSize,
                                int minimumSize,
                                int maximumSize,
                                String templateUrl)
  {
    Activity activity = new Activity();
    activity.setActivityName(activityName);
    activity.setDefaultComputationalUnits(defaultSize);
    activity.setMaximumComputationalUnits(maximumSize);
    activity.setMinimumComputationalUnits(minimumSize);
    activity.setTemplateUrl(templateUrl);
    emProvider.get().persist(activity);
    return activity;
  }

  @Transactional
  public Activity create(String activityName,
                         int defaultSize,
                         int minimumSize,
                         int maximumSize,
                         String os,
                         String templateUrl)
  {
    Activity activity = new Activity();
    activity.setActivityName(activityName);
    activity.setDefaultComputationalUnits(defaultSize);
    activity.setMaximumComputationalUnits(maximumSize);
    activity.setMinimumComputationalUnits(minimumSize);
    activity.setOs(os);
    activity.setTemplateUrl(templateUrl);
    emProvider.get().persist(activity);
    return activity;
  }

  @Transactional
  public Activity create(Activity activity)
  {
    emProvider.get().persist(activity);
    return activity;
  }

  public Activity get(UUID uuid)
  {
    TypedQuery<Activity> query = emProvider.get().createQuery("from Activity where id = :uuid", Activity.class);
    query.setParameter("uuid", uuid);
    Activity activity = query.getSingleResult();
    return activity;
  }
  
  public Activity get(String activityName)
  {
    TypedQuery<Activity> query = emProvider.get().createQuery("from Activity where activityName = :name", Activity.class);
    query.setParameter("name", activityName);
    Activity activity = query.getSingleResult();
    return activity;
  }
  
  @SuppressWarnings("unchecked")
  public List<Activity> get()
  {
    Query query = emProvider.get().createQuery("from Activity c order by c.activityName");
    List<Activity> results = query.getResultList();
    return results;
  }
  
  @Transactional
  public Activity update(Activity activity)
  {
    return emProvider.get().merge(activity);
  }
  
  @Transactional
  public void delete(Activity activity)
  {
    emProvider.get().remove(activity);
  }
  
  @Transactional
  public void delete(UUID uuid)
  {
    Activity activity = get(uuid);
    emProvider.get().remove(activity);;
  }
}
