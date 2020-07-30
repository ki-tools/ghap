package io.ghap.provision.vpg.data;

import io.ghap.provision.monitoring.lambda.CloudWatchMessage;
import io.ghap.provision.vpg.InstanceMonitoringState;

import java.util.List;

/**
  */
public interface MonitoringResourceFactory {
    InstanceMonitoringState create(CloudWatchMessage cloudWatchMessage, String stackId);

    void create(InstanceMonitoringState monitoringState);

    List<InstanceMonitoringState> list();

    InstanceMonitoringState getByMessageId(String messageId);

    List<InstanceMonitoringState> getByStackId(String stackId);

    void delete(InstanceMonitoringState instanceMonitoringState);

    void delete(List<InstanceMonitoringState> instanceMonitoringStates);
}
