package io.ghap.provision.vpg.data;

import java.util.UUID;

public class ActivityExistence {
  private boolean existence;
  private UUID activityId;

  public ActivityExistence() {}

  public void setExistence(boolean exist) {
    this.existence = exist;
  }

  public boolean getExistence() {
    return existence;
  }

  public void setActivityId(UUID activityId) {
    this.activityId = activityId;
  }

  public UUID getActivityId() {
    return activityId;
  }}
