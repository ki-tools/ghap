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
 * Represents the scheduled job with the responsibility to invoke the mechanisms to stop idle resources.
 */
public class StopIdleResourcesJob implements Job {

    private Logger logger = LoggerFactory.getLogger(StopIdleResourcesJob.class);

    @Inject
    private VPGMultiFactory vpgMultiFactory;

    @com.google.inject.Inject(optional = true)
    private MonitoringResourceFactory monitoringResourceFactory;


    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobKey jobKey = context.getJobDetail().getKey();
        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();

        System.out.printf("Begin execution of job <%s / %s> at %s%n", jobKey.getGroup(), jobKey.getName(), new Date());

        String stackId = jobDataMap.getString("StackId");
        System.out.printf("Idle Stack Id = %s%n", stackId);

        VirtualPrivateGrid virtualPrivateGrid = vpgMultiFactory.getByStackId(stackId);
        if (virtualPrivateGrid != null) {
            vpgMultiFactory.pause(virtualPrivateGrid, false);

        } else {
            logger.error(String.format("Unable to find the grid associated with stackId=%s", stackId));
        }

      if (monitoringResourceFactory != null) {
        List<InstanceMonitoringState> instanceMonitoringStates = monitoringResourceFactory.getByStackId(stackId);
        if (instanceMonitoringStates != null && !instanceMonitoringStates.isEmpty()) {
            monitoringResourceFactory.delete(instanceMonitoringStates);
        }
      }
      vpgMultiFactory.scheduleTerminateForIdleProvisionedResources(virtualPrivateGrid);
    }
}
