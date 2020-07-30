package io.ghap.provision.vpg;

import com.google.gson.annotations.SerializedName;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "TBL_VIRTUAL_PRIVATE_GRID", uniqueConstraints= @UniqueConstraint(columnNames = {"USER_UUID", "ACTIVITY_UUID"}))
public class VirtualPrivateGrid
{
  @Id
  @SerializedName("id")
  private UUID uuid = UUID.randomUUID();  
  @NotNull
  @Column(name = "ACTIVITY_UUID", unique = false, nullable = false)
  @SerializedName("activityId")
  private UUID activity_guid;
  @NotNull
  @Column(name = "USER_UUID", unique = false, nullable = false)
  @SerializedName("userId")
  private UUID user_uuid;
  @NotNull
  @Column(name = "STACK_ID", unique = true, nullable = false)
  private String stackId;
  @NotNull
  @Column(name = "PEM_KEY", columnDefinition = "TEXT", unique = true, nullable = false)
  private String pemKey;

  @Column(name = "EC2_INSTANCE_IDS", columnDefinition = "TEXT", nullable = true)
  private String ec2InstanceIds;

  @Column(name = "AUTO_SCALE_DESIRED_INSTANCE_COUNT")
  private Integer autoScaleDesiredInstanceCount;

  @Column(name = "AUTO_SCALE_MIN_INSTANCE_COUNT")
  private Integer autoScaleMinInstanceCount;

  @Column(name = "AUTO_SCALE_MAX_INSTANCE_COUNT")
  private Integer autoScaleMaxInstanceCount;

  public VirtualPrivateGrid()
  {
    super();
  }
  
  public void setId(UUID uuid)
  {
    this.uuid = uuid;
  }
  
  public UUID getId()
  {
    return uuid;
  }
  
  public void setActivityId(UUID activity_guid)
  {
    this.activity_guid = activity_guid;
  }
  
  public UUID getActivityId()
  {
    return activity_guid;
  }
  
  public void setUserId(UUID user_uuid)
  {
    this.user_uuid = user_uuid;
  }
  
  public UUID getUserId()
  {
    return user_uuid;
  }
  
  public void setStackId(String stackId)
  {
    this.stackId = stackId;
  }
  
  public String getStackId()
  {
    return stackId;
  }
  
  public void setPemKey(String pemKey)
  {
    this.pemKey = pemKey;
  }
  
  public String getPemKey()
  {
    return pemKey;
  }

  public String getEc2InstanceIds() {
    return ec2InstanceIds;
  }

  public void setEc2InstanceIds(String ec2InstanceIds) {
    this.ec2InstanceIds = ec2InstanceIds;
  }

  public Integer getAutoScaleDesiredInstanceCount() {
    return autoScaleDesiredInstanceCount;
  }

  public void setAutoScaleDesiredInstanceCount(Integer autoScaleDesiredInstanceCount) {
    this.autoScaleDesiredInstanceCount = autoScaleDesiredInstanceCount;
  }

  public Integer getAutoScaleMinInstanceCount() {
    return autoScaleMinInstanceCount;
  }

  public void setAutoScaleMinInstanceCount(Integer autoScaleMinInstanceCount) {
    this.autoScaleMinInstanceCount = autoScaleMinInstanceCount;
  }

  public Integer getAutoScaleMaxInstanceCount() {
    return autoScaleMaxInstanceCount;
  }

  public void setAutoScaleMaxInstanceCount(Integer autoScaleMaxInstanceCount) {
    this.autoScaleMaxInstanceCount = autoScaleMaxInstanceCount;
  }
}
