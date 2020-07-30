package io.ghap.provision.vpg;

import com.google.gson.annotations.SerializedName;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "TBL_VIRTUAL_ENV_STATE_RECORDER")
/**
 * This class represents an entry into a state table.  This allows us
 * to do some basic reporting on the state of a stack as a whole rather
 * then the individual resources that make up that stack.  The underlying
 * table looks like:
 *
 * <pre>
 *      id | activity_uuid | user_uuid | stack_id | time_stamp | state
 * </pre>
 */
public class VirtualPrivateGridStateEntry {
  public enum State { CREATED, RUNNING, STOPPED, TERMINATED }
  @Id
  @SerializedName("id")
  private UUID uuid = UUID.randomUUID();
  @NotNull
  @Column(name = "ACTIVITY_UUID", unique = false, nullable = false)
  @SerializedName("activityId")
  private UUID activityId;
  @NotNull
  @Column(name = "USER_UUID", unique = false, nullable = false)
  @SerializedName("userId")
  private UUID userId;
  @NotNull
  @Column(name = "STACK_ID", unique = false, nullable = false)
  private String stackId;
 @NotNull
  @Column(name = "TIME_STAMP", unique = false, nullable = false, updatable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date timestamp = Calendar.getInstance().getTime();
  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "STATE", unique = false, nullable = false)
  private State state;

  public VirtualPrivateGridStateEntry() { super(); }

  public VirtualPrivateGridStateEntry(UUID activity_guid, UUID user_uuid, String stackId, State state)
  {
    setActivityId(activity_guid);
    setUserId(user_uuid);
    setStackId(stackId);
    setState(state);
  }

  public UUID getId() {
    return uuid;
  }

  public void setId(UUID uuid) {
    this.uuid = uuid;
  }

  public UUID getActivityId() {
    return activityId;
  }

  public void setActivityId(UUID activity_guid) {
    this.activityId = activity_guid;
  }

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID user_uuid) {
    this.userId = user_uuid;
  }

  public String getStackId() {
    return stackId;
  }

  public void setStackId(String stackId) {
    this.stackId = stackId;
  }

  public Date getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }

  public State getState() {
    return state;
  }

  public void setState(State state) {
    this.state = state;
  }
}
