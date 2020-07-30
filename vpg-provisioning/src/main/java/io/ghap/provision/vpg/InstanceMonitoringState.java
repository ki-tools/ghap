package io.ghap.provision.vpg;

import com.google.gson.annotations.SerializedName;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.UUID;

/**
 */
@Entity
@Table(name = "TBL_MONITORED_INSTANCE_STATE")
@JsonIgnoreProperties(ignoreUnknown = true)
public class InstanceMonitoringState {

    @Id
    @SerializedName("id")
    private UUID guid = UUID.randomUUID();

    @NotNull
    @Column(name = "INSTANCE_ID", nullable = false, length = 20)
    private String instanceId;

    @Column(name = "STACK_ID", length = 255)
    private String stackId;

    @NotNull
    @Column(name = "JSON_MESSAGE", nullable = false, columnDefinition = "TEXT")
    private String jsonMessage;

    @NotNull
    @Column(name = "NEW_STATE_VALUE", nullable = false, length = 50)
    private String newStateValue;

    @NotNull
    @Column(name = "OLD_STATE_VALUE", nullable = false, length = 50)
    private String oldStateValue;

    @NotNull
    @Column(name = "METRIC", nullable = false, length = 50)
    private String metric;

    @NotNull
    @Column(name = "ALARM_NAME", nullable = false, length = 255)
    private String alarmName;

    @NotNull
    @Column(name = "ALARM_DESCRIPTION", nullable = false, length = 512)
    private String alarmDescription;

    @Column(name = "DATE_CREATED")
    private Date dateCreated;

    @Column(name = "MESSAGE_DATE")
    private Date messageDate;

    @NotNull
    @Column(name = "MESSAGE_ID", nullable = false, length = 255)
    private String messageId;

    public UUID getGuid() {
        return guid;
    }

    public void setGuid(UUID guid) {
        this.guid = guid;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getStackId() {
        return stackId;
    }

    public void setStackId(String stackId) {
        this.stackId = stackId;
    }

    public String getJsonMessage() {
        return jsonMessage;
    }

    public void setJsonMessage(String jsonMessage) {
        this.jsonMessage = jsonMessage;
    }

    public String getNewStateValue() {
        return newStateValue;
    }

    public void setNewStateValue(String newStateValue) {
        this.newStateValue = newStateValue;
    }

    public String getOldStateValue() {
        return oldStateValue;
    }

    public void setOldStateValue(String oldStateValue) {
        this.oldStateValue = oldStateValue;
    }

    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
        this.metric = metric;
    }

    public String getAlarmName() {
        return alarmName;
    }

    public void setAlarmName(String alarmName) {
        this.alarmName = alarmName;
    }

    public String getAlarmDescription() {
        return alarmDescription;
    }

    public void setAlarmDescription(String alarmDescription) {
        this.alarmDescription = alarmDescription;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    @PrePersist
    public void beforeInsert() {
        setDateCreated(new Date());
    }

    public Date getMessageDate() {
        return messageDate;
    }

    public void setMessageDate(Date messageDate) {
        this.messageDate = messageDate;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    @Override
    public String toString() {
        return "InstanceMonitoringState{" +
                "guid=" + guid +
                ", instanceId='" + instanceId + '\'' +
                ", stackId='" + stackId + '\'' +
                ", jsonMessage='" + jsonMessage + '\'' +
                ", newStateValue='" + newStateValue + '\'' +
                ", oldStateValue='" + oldStateValue + '\'' +
                ", metric='" + metric + '\'' +
                ", alarmName='" + alarmName + '\'' +
                ", alarmDescription='" + alarmDescription + '\'' +
                ", dateCreated=" + dateCreated +
                ", messageDate=" + messageDate +
                ", messageId='" + messageId + '\'' +
                '}';
    }
}
