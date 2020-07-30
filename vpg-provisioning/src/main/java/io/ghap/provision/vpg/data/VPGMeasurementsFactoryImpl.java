package io.ghap.provision.vpg.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;

import io.ghap.provision.vpg.VirtualPrivateGridMeasurementEntry;

public class VPGMeasurementsFactoryImpl implements VPGMeasurementsFactory {
  private Logger logger = LoggerFactory.getLogger(VPGMultiFactoryImpl.class);

  @Inject
  private Provider<EntityManager> emProvider;

  @Override
  @Transactional
  public VirtualPrivateGridMeasurementEntry create(String stackId, String instanceId, Date time, VirtualPrivateGridMeasurementEntry.Type type, Double value) {
    VirtualPrivateGridMeasurementEntry entry = new VirtualPrivateGridMeasurementEntry(stackId, instanceId, time, type, value);
    emProvider.get().persist(entry);
    return entry;
  }

  @Override
  public List<VirtualPrivateGridMeasurementEntry> get(String stackId, String instanceId, Date start, Date end)
  {
    List<VirtualPrivateGridMeasurementEntry> results = null;

    TypedQuery<VirtualPrivateGridMeasurementEntry> query = emProvider.get().createQuery(
        "from VirtualPrivateGridMeasurementEntry where STACK_ID = :stackId and INSTANCE_ID = :instanceId and MEASUREMENT_TIME >= :start and MEASUREMENT_TIME <= :end order by MEASUREMENT_TIME asc", 
        VirtualPrivateGridMeasurementEntry.class);
    query.setParameter("stackId", stackId).setParameter("instanceId", instanceId).setParameter("start", start).setParameter("end", end);
    try {
      results = query.getResultList();
    } catch(NoResultException nre) {
      if (logger.isDebugEnabled()) {
              logger.debug("No measurements found, stackId: {}, instanceId: {}, date range: {} to {}", stackId, instanceId, start, end);
      }
      results = new ArrayList<VirtualPrivateGridMeasurementEntry>();
    }
    return results;
  }  


  /**
   * Get all measurements of all the instances in the specified stack, and in the specified date range
   */
  @Override
  public List<VirtualPrivateGridMeasurementEntry> getByStack(String stackId, Date start, Date end)
  {
    List<VirtualPrivateGridMeasurementEntry> results = null;
    
    TypedQuery<VirtualPrivateGridMeasurementEntry> query = emProvider.get().createQuery(
        "from VirtualPrivateGridMeasurementEntry where STACK_ID = :stackId and MEASUREMENT_TIME >= :start and MEASUREMENT_TIME <= :end  order by MEASUREMENT_TIME asc", 
        VirtualPrivateGridMeasurementEntry.class);
    query.setParameter("stackId", stackId).setParameter("start", start).setParameter("end", end);
    try {
      results = query.getResultList();
    } catch(NoResultException nre) {
      if (logger.isDebugEnabled()) {
              logger.debug("No measurements found, stackId: {}, date range: {} to {}", stackId, start, end);
      }
      results = new ArrayList<VirtualPrivateGridMeasurementEntry>();
    }
    return results;
  }  

  /**
   * Get all instance ids of the specified stack, 
   * that have measurements stored in a specified date range
   */
  @Override
  public List<String> getInstancesByStack(String stackId, Date start, Date end)
  {
    List<String> results = new ArrayList<String>();
    TypedQuery<String> query = emProvider.get().createQuery("SELECT DISTINCT v.instanceId FROM VirtualPrivateGridMeasurementEntry v WHERE v.stackId = :stackId AND v.measurementTime >= :start AND v.measurementTime <= :end", String.class);
    query.setParameter("stackId", stackId).setParameter("start", start).setParameter("end", end);
    try {
      @SuppressWarnings("rawtypes")
      List queryResults = query.getResultList();
      for(Object entry : queryResults) {
        if (entry instanceof String) {
          results.add((String) entry);
        }
      }
    } catch(NoResultException nre) {
      if (logger.isDebugEnabled()) {
              logger.debug("No measurements found, stackId: {}, date range: {} to {}", stackId, start, end);
      }
      results = Collections.emptyList();
    }
    return results;
  }  

  /*
   * get the median value of hourly averages,
   * converted from 5-min averages we store in db,
   * for the specified instance and date range.
   * @returns -1 in case of error 
   */
  @Override
  public Double getInstanceMedianOfHourlyMeasurements(String instanceId, Date start, Date end)
  {
    List<VirtualPrivateGridMeasurementEntry> queryResults = getByInstance(instanceId, start, end);
    if (queryResults.isEmpty()) {
      return -1D;      
    }
    List<Double> hourlyAvgs = new ArrayList<Double>(queryResults.size()/5); //max size when no gaps in measurements

    Date nextDate = null;
    double currentAvg = 0;
    int divider = 1;
    for (VirtualPrivateGridMeasurementEntry entry : queryResults) {
      Date measurementTime = entry.getMeasurementTime();
      if (null == nextDate ) {
        nextDate = DateUtils.addHours(measurementTime, 1); 
      }
      if (measurementTime.after(nextDate)) { //one hour or more between mark and current date
        hourlyAvgs.add(currentAvg/divider);
        divider = 1;
        currentAvg = entry.getMeasurementValue();
        nextDate = DateUtils.addHours(measurementTime, 1); //adding one hour to mark
        while (measurementTime.after(nextDate)) { //this means we have a gap with no measurements (we do not gather stats for paused/stopped instances)
          //TODO: what if we have more than 5min but less than 1hour gaps in measurements?
          hourlyAvgs.add(0D); // filling the gap in measurements with zero avg
          nextDate = DateUtils.addHours(measurementTime, 1); //adding one hour to mark
        }
        continue;
      }
      currentAvg += entry.getMeasurementValue(); //adding current measurement
      divider++; //incrementing number of measurements
    }

    //Once the measurement loop is finished, the most recent hour's average still needs to be calculated
    hourlyAvgs.add(currentAvg/divider);


    if (hourlyAvgs.isEmpty()) {
      return -1D; //-1 means 'no data available'
    }

    hourlyAvgs.sort(null); //sorting will help to get median value
    
    //For odd number of elements, this is the median value.
    //For even number of elements, this is the right-central value
    Double median = hourlyAvgs.get(hourlyAvgs.size()/2); 
    if(hourlyAvgs.size() % 2 == 0) { //even number of elements, median is an average of the values of two central elements
      // we already have the right-central value in median, need to add the left-central value and divide the sum by 2
      median = (median + hourlyAvgs.get(hourlyAvgs.size()/2 - 1))/2;
    }

    return median;
  }  
  
  @Override
  public List<VirtualPrivateGridMeasurementEntry> getByInstance(String instanceId, Date start, Date end)
  {
    List<VirtualPrivateGridMeasurementEntry> results = null;
    
    TypedQuery<VirtualPrivateGridMeasurementEntry> query = emProvider.get().createQuery(
        "from VirtualPrivateGridMeasurementEntry where INSTANCE_ID = :instanceId and MEASUREMENT_TIME >= :start and MEASUREMENT_TIME <= :end order by MEASUREMENT_TIME asc", 
        VirtualPrivateGridMeasurementEntry.class);
    query.setParameter("instanceId", instanceId).setParameter("start", start).setParameter("end", end);
    try {
      results = query.getResultList();
    } catch(NoResultException nre) {
      if (logger.isDebugEnabled()) {
              logger.debug("No measurements found, instanceId: {}, date range: {} to {}", instanceId, start, end);
      }
      results = new ArrayList<VirtualPrivateGridMeasurementEntry>();
    }
    return results;
  }

  @Override
  public VirtualPrivateGridMeasurementEntry getLastMeasurementForInstance(String instanceId)
  {
    StringBuffer queryBuffer = new StringBuffer();
    queryBuffer.append("from VirtualPrivateGridMeasurementEntry A where A.measurementTime in (select distinct max(B.measurementTime) from ");
    queryBuffer.append("VirtualPrivateGridMeasurementEntry B where B.instanceId = :instanceId)");

    List<VirtualPrivateGridMeasurementEntry> results = null;
    TypedQuery<VirtualPrivateGridMeasurementEntry> query =
        emProvider.get().createQuery(queryBuffer.substring(0),
            VirtualPrivateGridMeasurementEntry.class);

    query.setParameter("instanceId", instanceId);
    try {
      results = query.getResultList();
      VirtualPrivateGridMeasurementEntry result = null;
      for(VirtualPrivateGridMeasurementEntry entry: results) {
        if(result == null) {
          result = entry;
        } else {
          if(entry.getMeasurementTime().getTime() > result.getMeasurementTime().getTime()) {
            result = entry;
          }
        }
      }
      return result;
    } catch(NoResultException nre) {
      logger.info("No Results Found");
    }
    return null;
  }
}
