package io.ghap.provision.vpg.scheduler;

import java.util.Arrays;
import java.util.Date;

import javax.inject.Inject;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;

import io.ghap.provision.vpg.VirtualPrivateGridMeasurementEntry;
import io.ghap.provision.vpg.data.VPGMeasurementsFactory;

  /**
   * The job polling Amazon CloudWatch for measurements and putting them in our DB.
   */
public class GetCloudWatchInstanceMeasurementsJob implements Job {

  private Logger logger = LoggerFactory.getLogger(GetCloudWatchInstanceMeasurementsJob.class);

  @Inject
  private VPGMeasurementsFactory vpgMeasurementsFactory;

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    JobKey jobKey = context.getJobDetail().getKey();
    if(null == context.getPreviousFireTime()) { //this is the first job run
      if (logger.isDebugEnabled()) {
        logger.debug("Next run for CloudWatch Poller Job <{} / {}> will be at {}", jobKey.getGroup(), jobKey.getName(), context.getNextFireTime());
      }
      return; // the only reason for this run is to have 'previousFireTime' stored for the "actual scheduled first run"
    }
  
    JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();

    //TODO: find out why logger is not used in other jobs          
    logger.info("Starting CloudWatch Poller Job <{} / {}> at {}", jobKey.getGroup(), jobKey.getName(), new Date());
    String instanceId = jobDataMap.getString("instanceId");
    String stackId = jobDataMap.getString("stackId");

    AWSCredentialsProvider credentials = new SystemPropertiesCredentialsProvider();
    AmazonCloudWatchClient cloudWatch = new AmazonCloudWatchClient(credentials.getCredentials());
    //cloudWatch.setEndpoint("monitoring.us-east-1.amazonaws.com");
    Dimension instanceDimension = new Dimension();
    instanceDimension.setName("instanceid");
    instanceDimension.setValue(instanceId); 
    
    //get measurements with 5-minute granularity (can't be less than 1 minute)
    GetMetricStatisticsRequest request = new GetMetricStatisticsRequest()
      .withStartTime(context.getPreviousFireTime()).withEndTime(new Date()).withPeriod(60 * 5)
      .withNamespace("AWS/EC2")      
      .withMetricName("CPUUtilization")
      .withStatistics("Average")
      .withDimensions(Arrays.asList(instanceDimension));
    GetMetricStatisticsResult result = cloudWatch.getMetricStatistics(request);
    
    for (Datapoint dp : result.getDatapoints()) { //copying data returned from CloudWatch stats to our db
      vpgMeasurementsFactory.create(stackId, instanceId, dp.getTimestamp(), VirtualPrivateGridMeasurementEntry.Type.CPUUTILIZATION, dp.getAverage());
    }
  }
}
