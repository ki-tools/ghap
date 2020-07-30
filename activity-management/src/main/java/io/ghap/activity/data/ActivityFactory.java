package io.ghap.activity.data;

import io.ghap.activity.Activity;

import java.util.List;
import java.util.UUID;

/**
 * Factory for obtaining an activity and managing its lifecycle.
 * 
 * @author snagy
 *
 */
public interface ActivityFactory
{
  /**
   * Create a new Activity from and Activity object
   * @param activity
   * @return
   */
  Activity create(Activity activity);
  
  /**
   * Create a new Activity from a set of parameters
   * @param activityName
   * @param defaultSize
   * @param minimumSize
   * @param maximumSize
   * @param templateUrl
   * @return
   */
  Activity create(String activityName,
                  int defaultSize,
                  int minimumSize,
                  int maximumSize,
                  String templateUrl);

  /**
   * Create a new Activity from a set of parameters
   * @param activityName
   * @param defaultSize
   * @param minimumSize
   * @param maximumSize
   * @param os
   * @param templateUrl
   * @return
   */
  Activity create(String activityName,
                  int defaultSize,
                  int minimumSize,
                  int maximumSize,
                  String os,
                  String templateUrl);

  /**
   * Get an Activity by Activity Name
   * @param activityName
   * @return
   */
  Activity get(String activityName);
  
  /**
   * Get an Activity by UUID
   * @param uuid
   * @return
   */
  Activity get(UUID uuid);
  
  /**
   * Get all of the Activities
   * @return
   */
  List<Activity> get();
  
  /**
   * Update a given Activity
   * @param activity
   * @return
   */
  Activity update(Activity activity);
  
  /**
   * Delete a given Activity
   * @param activity
   */
  void delete(Activity activity);
  
  /**
   * Delete an Activity by UUID
   * @param uuid
   */
  void delete(UUID uuid);
}
