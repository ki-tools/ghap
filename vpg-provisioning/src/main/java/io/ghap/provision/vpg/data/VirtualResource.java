package io.ghap.provision.vpg.data;

import java.util.Date;
import java.util.UUID;

/**
 * Created by snagy on 5/7/15.
 */
public class VirtualResource
{
  private int coreCount;
  private String address;
  private String dnsname; 
  private String instanceId;
  private String status;
  private String instanceOsType;
  private UUID vpgId;
  private String stackId;
  private UUID userId;
  private Date launchTime;
  private String imageId;
  private Integer autoScaleDesiredInstanceCount;
  private Integer autoScaleMaxInstanceCount;
  private boolean isTopLevelNode;

  public VirtualResource()
  {
  }

  public void setVpgId(UUID vpgId) { this.vpgId = vpgId; }
  public UUID getVpgId() { return vpgId; }

  public void setStackId(String stackId) { this.stackId = stackId; }
  public String getStackId() { return stackId; }

  public void setCoreCount(int coreCount)
  {
    this.coreCount = coreCount;
  }

  public int getCoreCount()
  {
    return coreCount;
  }

  public String getInstanceId()
  {
    return instanceId;
  }

  public void setInstanceId(String instanceId)
  {
    this.instanceId = instanceId;
  }

  public String getAddress()
  {
    return address;
  }

  public void setAddress(String address)
  {
    this.address = address;
  }

  public String getStatus()
  {
    return status;
  }

  public void setStatus(String status)
  {
    this.status = status;
  }

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public String getInstanceOsType() { return instanceOsType; }

  public void setInstanceOsType(String instanceOsType) {
    if(instanceOsType != null && instanceOsType.equalsIgnoreCase("windows")) {
      this.instanceOsType = "Windows";
    } else {
      this.instanceOsType = instanceOsType;
    }
  }

  public String getDnsname() {
    return dnsname;
  }

  public void setDnsname(String dnsname) {
    this.dnsname = dnsname;
  }

  public void setLaunchTime(Date launchTime) {
    this.launchTime = launchTime;
  }

  public Date getLaunchTime() {
    return launchTime;
  }

  public void setImageId(String imageId) {
    this.imageId = imageId;
  }

  public String getImageId() {
    return imageId;
  }

  public Integer getAutoScaleDesiredInstanceCount() {
    return autoScaleDesiredInstanceCount;
  }

  public void setAutoScaleDesiredInstanceCount(Integer autoScaleDesiredInstanceCount) {
    this.autoScaleDesiredInstanceCount = autoScaleDesiredInstanceCount;
  }

  public Integer getAutoScaleMaxInstanceCount() {
    return autoScaleMaxInstanceCount;
  }

  public void setAutoScaleMaxInstanceCount(Integer autoScaleMaxInstanceCount) {
    this.autoScaleMaxInstanceCount = autoScaleMaxInstanceCount;
  }

  public boolean isTopLevelNode() {
    return isTopLevelNode;
  }

  public void setIsTopLevelNode(boolean isTopLevelNode) {
    this.isTopLevelNode = isTopLevelNode;
  }
}
