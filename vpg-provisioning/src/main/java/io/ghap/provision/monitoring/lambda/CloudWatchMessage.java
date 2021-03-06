package io.ghap.provision.monitoring.lambda;

import org.joda.time.DateTime;

/**
 */
public class CloudWatchMessage {

    public String AlarmName;
    public String AlarmDescription;
    public String AWSAccountId;
    public String NewStateValue;
    public String NewStateReason;
    public String StateChangeTime;
    public String Region;
    public String OldStateValue;
    public Trigger Trigger;
    public DateTime MessageTime;
    public String MessageId;
}
