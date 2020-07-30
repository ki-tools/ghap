package io.ghap.provision.vpg.data;

import java.util.UUID;

public class ActivityStatus {
  private String status;
  private UUID activityId;

  public ActivityStatus() {}

  public void setStatus(String status) {
    this.status = status;
  }

  public String getStatus() {
    return status;
  }

  public void setActivityId(UUID activityId) {
    this.activityId = activityId;
  }

  public UUID getActivityId() {
    return activityId;
  }
}
