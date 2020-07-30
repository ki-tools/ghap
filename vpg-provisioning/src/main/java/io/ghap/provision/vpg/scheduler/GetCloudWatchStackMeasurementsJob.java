package io.ghap.provision.vpg.scheduler;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.*;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.*;
import io.ghap.provision.vpg.VirtualPrivateGridMeasurementEntry;
import io.ghap.provision.vpg.data.VPGMeasurementsFactory;

import org.apache.commons.lang.time.DateUtils;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;
import java.util.Calendar;

/**
 * The job polling Amazon CloudWatch for measurements and putting them in our DB.
 */
public class GetCloudWatchStackMeasurementsJob implements Job {

  private Logger logger = LoggerFactory.getLogger(GetCloudWatchStackMeasurementsJob.class);

  @Inject
  private VPGMeasurementsFactory vpgMeasurementsFactory;

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    JobKey jobKey = context.getJobDetail().getKey();
    if (null == context.getPreviousFireTime()) { //this is the first job run
      if (logger.isDebugEnabled()) {
        logger.debug("Next run for CloudWatch Poller Job <{} / {}> will be at {}", jobKey.getGroup(), jobKey.getName(), context.getNextFireTime());
      }
      return; // the only reason for this run is to have 'previousFireTime' stored for the "actual scheduled first run"
    }

    JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();

    logger.info("Starting CloudWatch Poller Job <{} / {}> at {}", jobKey.getGroup(), jobKey.getName(), new Date());
    String stackId = jobDataMap.getString("stackId");

    AWSCredentialsProvider credentials = new SystemPropertiesCredentialsProvider();
    AmazonCloudFormationClient cloudformation = new AmazonCloudFormationClient(credentials.getCredentials());
    DescribeStackResourcesRequest describeStackResourcesRequest = new DescribeStackResourcesRequest();
    describeStackResourcesRequest.setStackName(stackId);
    DescribeStackResourcesResult describeStackResourcesResult = cloudformation.describeStackResources(describeStackResourcesRequest);
    List<StackResource> resources = describeStackResourcesResult.getStackResources();
    for (StackResource resource : resources) {
      if (resource.getResourceType().equals("AWS::EC2::Instance")) {
        String instanceId = resource.getPhysicalResourceId();

        logger.info("Creating Get Metric Statistics Request.");
        VirtualPrivateGridMeasurementEntry measurement = vpgMeasurementsFactory.getLastMeasurementForInstance(instanceId);
        
        Date endDate = new Date();
        //start date set to "two weeks ago" when no measurements available yet
        Date startDate = measurement == null ? DateUtils.addDays(endDate, -14) : measurement.getMeasurementTime();
       
        logger.info("Start Date : {}", startDate);
        logger.info("End Date: {}", endDate);
        logger.info("Instance Id {}", instanceId);

        List<Datapoint> dataPoints = new ArrayList<>();

        //CloudWatch allows pulling data for single-day-range in one request, need multiple requests for larger ranges
        while(startDate.before(endDate)) { //increasing the start date in a loop
          Date tempEndDate = DateUtils.addDays(startDate, 1); //adding one day to start date
          if (tempEndDate.after(endDate)) { // final chunk, less than one day till the end
            tempEndDate = endDate;
          }
          // don't start the data pull from the exact moment of the last record otherwise we will
          // try and insert a duplicate record, instead increment it by a minute.  This should be
          // fine since we are pulling at 5 minute intervals.
          dataPoints.addAll(getDataPoints(instanceId, DateUtils.addMinutes(startDate, 1), tempEndDate));
          startDate = tempEndDate;
        }
        
        for (Datapoint dataPoint : dataPoints) { //copying data returned from CloudWatch stats to our db
          logger.info("Recording Datapoint for time : {}", dataPoint.getTimestamp());
          vpgMeasurementsFactory.create(stackId, instanceId, dataPoint.getTimestamp(), VirtualPrivateGridMeasurementEntry.Type.CPUUTILIZATION, dataPoint.getAverage());
        }
      }
    }
  }

  private List<Datapoint> getDataPoints(String instanceId, Date startDate, Date endDate) {
    AWSCredentialsProvider credentials = new SystemPropertiesCredentialsProvider();
    AmazonCloudWatchClient cloudWatch = new AmazonCloudWatchClient(credentials.getCredentials());

    GetMetricStatisticsRequest getMetricStatisticsRequest = new GetMetricStatisticsRequest();
    getMetricStatisticsRequest.setMetricName("CPUUtilization");
    getMetricStatisticsRequest.setStartTime(startDate);
    getMetricStatisticsRequest.setEndTime(endDate);
    getMetricStatisticsRequest.setPeriod(300);
    getMetricStatisticsRequest.setStatistics(Arrays.asList(new String[] {Statistic.Average.toString()}));
    getMetricStatisticsRequest.setNamespace("AWS/EC2");
    getMetricStatisticsRequest.setDimensions(Arrays.asList(new Dimension[] { new Dimension().withName("InstanceId").withValue(instanceId)}));


    logger.info("Invoking Get Metric Statistics Request.");
    GetMetricStatisticsResult getMetricStatisticsResult = cloudWatch.getMetricStatistics(getMetricStatisticsRequest);
    List<Datapoint> dataPoints = getMetricStatisticsResult.getDatapoints();
    logger.info("Found {} datapoints.", dataPoints.size());

    return dataPoints;
  }
}
