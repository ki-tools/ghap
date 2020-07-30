package io.ghap.provision.vpg.scheduler;

import io.ghap.provision.vpg.InstanceMonitoringState;
import io.ghap.provision.vpg.VirtualPrivateGrid;
import io.ghap.provision.vpg.data.MonitoringResourceFactory;
import io.ghap.provision.vpg.data.VPGMultiFactory;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;

/**
 * Represents the scheduled job with the responsibility to invoke the mechanisms to terminate idle resources.
 */
public class TerminateIdleResourcesJob implements Job {

    private Logger logger = LoggerFactory.getLogger(TerminateIdleResourcesJob.class);

    @Inject
    private VPGMultiFactory vpgMultiFactory;

    @Inject
    private MonitoringResourceFactory monitoringResourceFactory;


    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobKey jobKey = context.getJobDetail().getKey();
        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();

        logger.info("Begin execution of job <%s / %s> at %s%n", jobKey.getGroup(), jobKey.getName(), new Date());

        String stackId = jobDataMap.getString("StackId");
        logger.info("Idle Stack Id = %s%n", stackId);
        List<InstanceMonitoringState> byStackId = monitoringResourceFactory.getByStackId(stackId);
        if (byStackId != null && !byStackId.isEmpty()) {
            logger.info("Stack id {} was used after stop. Dismiss stack termination. Monitoring states are:", stackId);
            for(InstanceMonitoringState state : byStackId) {
                logger.info("state = {}", state);
            }
            return;
        }

        VirtualPrivateGrid virtualPrivateGrid = vpgMultiFactory.getByStackId(stackId);
        if (virtualPrivateGrid != null) {
            vpgMultiFactory.delete(virtualPrivateGrid);
        } else {
            logger.error(String.format("Unable to find the grid associated with stackId=%s", stackId));
        }
    }
}
