package io.ghap.activity;

import com.google.gson.annotations.SerializedName;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "TBL_ACTIVITY_ROLE_ASSOC")
public class ActivityRoleAssociation
{
  @Id
  @SerializedName("id")
  private UUID uuid = UUID.randomUUID();
  
  @NotNull
  @Column(name = "ROLE_UUID", nullable = false)
  @SerializedName("roleId")
  private UUID role_uuid;

  @NotNull
  @Column(name = "ACTIVITY_UUID", nullable = false)
  @SerializedName("activityId")
  private UUID activity_uuid;
  
  public ActivityRoleAssociation()
  {
    super();
  }
  
  public void setId(UUID uuid)
  {
    this.uuid = uuid;
  }
  
  public UUID getId()
  {
    return this.uuid;
  }
  
  public void setRoleId(UUID role_uuid)
  {
    this.role_uuid = role_uuid;
  }
  
  public UUID getRoleId()
  {
    return this.role_uuid;
  }
  
  public void setActivity(Activity activity)
  {
    this.activity_uuid = activity.getId();
  }
  
  public void setActivity(UUID activity_uuid)
  {
    this.activity_uuid = activity_uuid;
  }
  
  public UUID getActivityId()
  {
    return activity_uuid;
  }
}
