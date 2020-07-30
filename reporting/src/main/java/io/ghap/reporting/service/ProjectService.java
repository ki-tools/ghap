package io.ghap.reporting.service;

import io.ghap.reporting.data.PermissionMismatchItem;
import io.ghap.reporting.data.ProjectReportItem;

import java.util.List;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
public interface ProjectService {
	List<ProjectReportItem> loadProjects(String accessToken);

	List<ProjectReportItem> loadGrants(String accessToken);

	List<PermissionMismatchItem> loadPermissions(String accessToken);
}
