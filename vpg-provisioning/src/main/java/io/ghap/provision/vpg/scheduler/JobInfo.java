package io.ghap.provision.vpg.scheduler;

import org.quartz.JobKey;

import java.util.Date;

/**
 * Created by arao on 10/1/15.
 */
public class JobInfo {
    private JobKey jobKey;
    private Date scheduledDate;

    public JobInfo(JobKey jobKey, Date scheduledDate) {
        this.jobKey = jobKey;
        this.scheduledDate = scheduledDate;
    }

    public JobKey getJobKey() {
        return jobKey;
    }

    public Date getScheduledDate() {
        return scheduledDate;
    }
}
