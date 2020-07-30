package io.ghap.reporting.data;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.UUID;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Activity implements Serializable, Comparable<Activity>{

	private UUID id;
	private String activityName;
	private Integer minimumComputationalUnits;
	private Integer maximumComputationalUnits;
	private Integer defaultComputationalUnits;
	private String templateUrl;
	private String os;

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getActivityName() {
		return activityName;
	}

	public void setActivityName(String activityName) {
		this.activityName = activityName;
	}

	public Integer getMinimumComputationalUnits() {
		return minimumComputationalUnits;
	}

	public void setMinimumComputationalUnits(Integer minimumComputationalUnits) {
		this.minimumComputationalUnits = minimumComputationalUnits;
	}

	public Integer getMaximumComputationalUnits() {
		return maximumComputationalUnits;
	}

	public void setMaximumComputationalUnits(Integer maximumComputationalUnits) {
		this.maximumComputationalUnits = maximumComputationalUnits;
	}

	public Integer getDefaultComputationalUnits() {
		return defaultComputationalUnits;
	}

	public void setDefaultComputationalUnits(Integer defaultComputationalUnits) {
		this.defaultComputationalUnits = defaultComputationalUnits;
	}

	public String getTemplateUrl() {
		return templateUrl;
	}

	public void setTemplateUrl(String templateUrl) {
		this.templateUrl = templateUrl;
	}

	public String getOs() {
		return os;
	}

	public void setOs(String os) {
		this.os = os;
	}

	@Override
	public int compareTo(Activity o) {
		return id.compareTo(o.getId());
	}
}
