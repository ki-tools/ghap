package io.ghap.provision.vpg;

import com.google.gson.annotations.SerializedName;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "TBL_PERSONAL_STORAGE")
public class PersonalStorage
{
  @Id
  @SerializedName("id")
  private UUID guid = UUID.randomUUID();
  @NotNull
  @Column(name = "UUID", unique = true, nullable = false)
  @SerializedName("userId")
  private UUID uuid;
  @NotNull
  @Column(name = "SIZE_IN_GIGABYTES", nullable = false)
  @SerializedName("size")
  private int g_size = 500;
  @NotNull
  @Column(name = "VOLUME_ID", unique = true, nullable = false)
  @SerializedName("volumeId")
  private String volume_id;
  @NotNull
  @Column(name = "AVAILABILITY_ZONE", nullable = false)
  private String availabilityZone;
  
  public PersonalStorage()
  {
    super();
  }
  
  public void setId(UUID guid) 
  {
    this.guid = guid;
  }
  
  public UUID getId()
  {
    return this.guid;
  }
  
  public void setUserId(UUID uuid)
  {
    this.uuid = uuid;
  }
  
  public UUID getUserId()
  {
    return uuid;
  }
  
  public void setSize(int size)
  {
    this.g_size = size;
  }
  
  public int getSize()
  {
    return this.g_size;
  }
  
  public void setVolumeId(String volume_id)
  {
    this.volume_id = volume_id;
  }
  
  public String getVolumeId()
  {
    return this.volume_id;
  }
  
  public void setAvailabilityZone(String availabilityZone)
  {
    this.availabilityZone = availabilityZone;
  }
  
  public String getAvailabilityZone()
  {
    return availabilityZone;
  }
}
