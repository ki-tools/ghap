package io.ghap.activity;

import com.google.gson.annotations.SerializedName;

import java.net.URL;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "TBL_ACTIVITIES")
public class Activity
{
  @Id
  @SerializedName("id")
  private UUID uuid = UUID.randomUUID();
  @Column(name = "ACTIVITY_NAME", unique = true, nullable = false)
  @NotNull
  private String activityName;
  @Column(name = "MINIMUM", nullable = false)
  @NotNull
  private int minimumComputationalUnits;
  @Column(name = "MAXIMUM", nullable = false)
  @NotNull
  private int maximumComputationalUnits;
  @Column(name = "INITIAL", nullable = false)
  @NotNull
  private int defaultComputationalUnits;
  @Column(name = "TEMPLATE_URL", nullable = false)
  @NotNull
  private String templateUrl;
  @Column(name = "OS")
  private String os;
  
  public Activity() {
    super();
  }
  
  public UUID getId() 
  {
    return uuid;
  }
  
  public void setId(UUID id)
  {
    this.uuid = id;
  }
  
  public String getActivityName()
  {
    return activityName;
  }
  
  public void setActivityName(String name)
  {
    this.activityName = name;
  }
  
  public void setMinimumComputationalUnits(int size)
  {
    this.minimumComputationalUnits = size;
  }
  
  public int getMinimumComputationalUnits()
  {
    return minimumComputationalUnits;
  }
  
  public void setMaximumComputationalUnits(int size)
  {
    this.maximumComputationalUnits = size;
  }
  
  public int getMaximumComputationalUnits()
  {
    return maximumComputationalUnits;
  }
  
  public void setDefaultComputationalUnits(int size)
  {
    this.defaultComputationalUnits = size;
  }
  
  public int getDefaultComputationalUnits()
  {
    return defaultComputationalUnits;
  }
  
  public void setTemplateUrl(String templateUrl)
  {
    this.templateUrl = templateUrl;
  }
  
  public String getTemplateUrl()
  {
    return templateUrl;
  }

  public void setOs(String os) { this.os = os; }
  public String getOs() { return os; }
}
