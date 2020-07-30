package io.ghap.util;

import org.quartz.SchedulerException;
import org.quartz.spi.InstanceIdGenerator;

import java.util.UUID;

/**
 */
public class QuartzIdGenerator implements InstanceIdGenerator {

    public String generateInstanceId() throws SchedulerException {
        try {
            return UUID.randomUUID().toString() + System.currentTimeMillis();
        } catch (Exception e) {
            throw new SchedulerException("Couldn't get host name!", e);
        }
    }
}
