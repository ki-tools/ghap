package io.ghap.service;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
public interface ProjectService {
    boolean isUserHasGrantPermission(String accessToken, String project, String grant);

    boolean isUserHasProjectPermission(String accessToken, String project);
}
