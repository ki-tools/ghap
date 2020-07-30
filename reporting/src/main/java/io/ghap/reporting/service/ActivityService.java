package io.ghap.reporting.service;

import io.ghap.reporting.data.Activity;

import java.util.List;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
public interface ActivityService {
	List<Activity> loadActivities(String accessToken);
}
