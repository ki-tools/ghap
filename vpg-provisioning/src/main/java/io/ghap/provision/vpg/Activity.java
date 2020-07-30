package io.ghap.provision.vpg;

import com.google.gson.annotations.SerializedName;

import java.util.UUID;

public class Activity
{
  @SerializedName("id")
  private UUID uuid;
  private String activityName;
  private int minimumComputationalUnits;
  private int maximumComputationalUnits;
  private int defaultComputationalUnits;
  private String templateUrl;
  private String os;

  public Activity(){

  }

  public Activity(final String uuid){
    this.uuid = UUID.fromString(uuid);
  }

  public UUID getId()
  {
    return uuid;
  }

  public void setId(UUID uuid)
  {
    this.uuid = uuid;
  }

  public String getActivityName()
  {
    return activityName;
  }

  public void setActivityName(String activityName)
  {
    this.activityName = activityName;
  }

  public int getMinimumComputationalUnits()
  {
    return minimumComputationalUnits;
  }

  public void setMinimumComputationalUnits(int minimumComputationalUnits)
  {
    this.minimumComputationalUnits = minimumComputationalUnits;
  }

  public int getMaximumComputationalUnits()
  {
    return maximumComputationalUnits;
  }

  public void setMaximumComputationalUnits(int maximumComputationalUnits)
  {
    this.maximumComputationalUnits = maximumComputationalUnits;
  }

  public int getDefaultComputationalUnits()
  {
    return defaultComputationalUnits;
  }

  public void setDefaultComputationalUnits(int defaultComputationalUnits)
  {
    this.defaultComputationalUnits = defaultComputationalUnits;
  }
  
  public void setTemplateUrl(String templateUrl)
  {
    this.templateUrl = templateUrl;
  }
  
  public String getTemplateUrl()
  {
    return templateUrl;
  }

  public String getOs() { return os; }
  public void setOs(String os) { this.os = os; }
}
