package io.ghap.reporting.data;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DurationReportItem {

	private String stackId;
	private String userId;
	private String activityId;
	private Long creationDate;
  private Long terminationDate;

  private Long durationExistence;
  private Long durationRunning;
  private Long durationStopped;

	public String getStackId() {
		return stackId;
	}

	public void setStackId(String stackId) {
		this.stackId = stackId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getActivityId() {
		return activityId;
	}

	public void setActivityId(String activityId) {
		this.activityId = activityId;
	}

	public Long getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Long creationDate) {
		this.creationDate = creationDate;
	}

  public Long getTerminationDate() {
    return terminationDate;
  }

  public void setTerminationDate(Long terminationDate) {
    this.terminationDate = terminationDate;
  }

  public Long getDurationExistence() {
    return durationExistence;
  }

  public void setDurationExistence(Long durationExistence) {
    this.durationExistence = durationExistence;
  }

  public Long getDurationRunning() {
    return durationRunning;
  }

  public void setDurationRunning(Long durationRunning) {
    this.durationRunning = durationRunning;
  }

  public Long getDurationStopped() {
    return durationStopped;
  }

  public void setDurationStopped(Long durationStopped) {
    this.durationStopped = durationStopped;
  }
}
