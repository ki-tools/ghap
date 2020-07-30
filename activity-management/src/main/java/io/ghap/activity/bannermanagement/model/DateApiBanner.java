package io.ghap.activity.bannermanagement.model;

import java.util.Date;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
public interface DateApiBanner extends ApiBanner {
    Date getStartTime();

    void setStartTime(Date startTime);

    Date getEndTime();

    void setEndTime(Date endTime);
}
