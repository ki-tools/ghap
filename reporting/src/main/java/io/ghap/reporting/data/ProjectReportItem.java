package io.ghap.reporting.data;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.List;
import java.util.Set;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectReportItem {

	private Grant grant;
	private Project project;
	private ProjectUser user;
	private List<PermissionRule> rules;

	public Grant getGrant() {
		return grant;
	}

	public void setGrant(Grant grant) {
		this.grant = grant;
	}

	public Project getProject() {
		return project;
	}

	public void setProject(Project project) {
		this.project = project;
	}

	public ProjectUser getUser() {
		return user;
	}

	public void setUser(ProjectUser user) {
		this.user = user;
	}

	public List<PermissionRule> getRules() {
		return rules;
	}

	public void setRules(List<PermissionRule> rules) {
		this.rules = rules;
	}
}
