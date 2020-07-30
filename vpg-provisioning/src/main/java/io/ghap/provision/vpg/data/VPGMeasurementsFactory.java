package io.ghap.provision.vpg.data;

import java.util.Date;
import java.util.List;

import io.ghap.provision.vpg.VirtualPrivateGridMeasurementEntry;

public interface VPGMeasurementsFactory {

  //@Override
  VirtualPrivateGridMeasurementEntry create(String stackId, String instanceId, Date time,
      VirtualPrivateGridMeasurementEntry.Type type, Double value);

  //@Override
  List<VirtualPrivateGridMeasurementEntry> get(String stackId, String instanceId, Date start, Date end);

  //@Override
  List<VirtualPrivateGridMeasurementEntry> getByStack(String stackId, Date start, Date end);

  //@Override
  List<VirtualPrivateGridMeasurementEntry> getByInstance(String instanceId, Date start, Date end);

  List<String> getInstancesByStack(String stackId, Date start, Date end);

  VirtualPrivateGridMeasurementEntry getLastMeasurementForInstance(String instanceId);

  Double getInstanceMedianOfHourlyMeasurements(String instanceId, Date start, Date end);

}