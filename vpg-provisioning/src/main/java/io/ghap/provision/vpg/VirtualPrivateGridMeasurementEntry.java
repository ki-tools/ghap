package io.ghap.provision.vpg;

import java.util.Date;
import java.util.UUID;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

import com.google.gson.annotations.SerializedName;

@Entity
@Table(name = "TBL_VIRTUAL_ENV_MEASUREMENTS_RECORDER",uniqueConstraints= @UniqueConstraint(columnNames = {"STACK_ID", "INSTANCE_ID", "MEASUREMENT_TIME"}))
/**
 * This class represents an entry into a performance data measurements table.
 * The underlying table looks like:
 *
 * <pre>
 *      id | stack_id | instance_id | measurement_time | measurement_type | measurement_value
 * </pre>
 */
public class VirtualPrivateGridMeasurementEntry {
  public enum Type { CPUUTILIZATION } //more measurement types to be probably added in future 
  @Id
  @SerializedName("id")
  private UUID uuid = UUID.randomUUID();
  
  @NotNull
  @Column(name = "STACK_ID", unique = false, nullable = false)
  @SerializedName("stackId")
  private String stackId;

  @NotNull
  @Column(name = "INSTANCE_ID", unique = false, nullable = false)
  @SerializedName("instanceId")
  private String instanceId;
  
  @NotNull
  @Column(name = "MEASUREMENT_TIME", unique = false, nullable = false)
  @SerializedName("measurementTime")
  @Temporal(TemporalType.TIMESTAMP)
  private Date measurementTime;

  
  @NotNull
  @Enumerated(EnumType.ORDINAL)
  @Column(name = "MEASUREMENT_TYPE", unique = false, nullable = false)
  @SerializedName("measurementType")
  private Type measurementType;
  
  @NotNull
  @Column(name = "MEASUREMENT_VALUE", unique = false, nullable = false)
  @SerializedName("MEASUREMENT_VALUE")
  private Double measurementValue;

  
  public VirtualPrivateGridMeasurementEntry() { super(); }

  public VirtualPrivateGridMeasurementEntry(String stackId, String instanceId, Date measurementTime, Type measurementType,
      Double measurementValue) {
    this.stackId = stackId;
    this.instanceId = instanceId;
    this.measurementTime = measurementTime;
    this.measurementType = measurementType;
    this.measurementValue = measurementValue;
  }


  public String getStackId() {
    return stackId;
  }


  public void setStackId(String stackId) {
    this.stackId = stackId;
  }


  public String getInstanceId() {
    return instanceId;
  }


  public void setInstanceId(String instanceId) {
    this.instanceId = instanceId;
  }


  public Date getMeasurementTime() {
    return measurementTime;
  }


  public void setMeasurementTime(Date measurementTime) {
    this.measurementTime = measurementTime;
  }


  public Double getMeasurementValue() {
    return measurementValue;
  }


  public void setMeasurementValue(Double measurementValue) {
    this.measurementValue = measurementValue;
  }

}
