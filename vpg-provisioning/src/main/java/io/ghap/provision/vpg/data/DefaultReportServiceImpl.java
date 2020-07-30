package io.ghap.provision.vpg.data;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackStatus;
import io.ghap.aws.annotations.AWSApiCall;
import io.ghap.provision.vpg.VirtualPrivateGrid;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.github.hburgmeier.jerseyoauth2.rs.api.annotations.OAuth20;
import com.google.inject.Singleton;

import io.ghap.oauth.AllowedScopes;
import io.ghap.oauth.PredicateType;
import io.ghap.provision.vpg.VirtualPrivateGridMeasurementEntry;
import io.ghap.provision.vpg.VirtualPrivateGridStateEntry;

@Path("report")
@Singleton
@OAuth20
@AllowedScopes(scopes = { "GhapAdministrators", "Administrators", "GHAP Administrator" }, predicateType = PredicateType.OR)
public class DefaultReportServiceImpl {

  private Logger log = LoggerFactory.getLogger(this.getClass());

  @Inject
  private VPGMultiFactory vpgFactory;

  @Inject
  private VPGStateFactory vpgStateFactory;
  
  @Inject 
  private VPGMeasurementsFactory VPGMeasurementsFactory;


  @GET
  @Path("usagereport")
  @Produces(APPLICATION_JSON)
  /**
   * Produces a merge of stacks state durations and CPU median usage report for instances of the specified stack
   * each item copies the stack-related info, adding instanceID and cpu usage median for that instance.
   */
  public List<UsageReportItem> getUsageReport(@QueryParam("start") String start, @QueryParam("end") String end) {
    List<UsageReportItem> retVal = new ArrayList<>();
    
    log.info("getUsageReport, start = {}, end={}", start, end);

    List<DurationReportItem> durationReports = getStatesDuration(start, end);

    for(DurationReportItem durationItem : durationReports) {
      retVal.addAll(buildUsageReportRecords(durationItem, start, end));
    }
    return retVal;
  }

  private List<UsageReportItem> buildUsageReportRecords(DurationReportItem durationReportItem, String start, String end) {

    List<UsageReportItem> retVal = new ArrayList<>();

    List<CPUReportItem> cpuReports = getCpuUsageMedian(durationReportItem.getStackId(), start, end);
    if (null == cpuReports || cpuReports.isEmpty()) {

      UsageReportItem usageReportItem = new UsageReportItem(durationReportItem.getStackId(),
              durationReportItem.getUserId(), durationReportItem.getActivityId(),
              durationReportItem.getDurationExistence(), durationReportItem.getDurationRunning(), durationReportItem.getDurationStopped(),
              durationReportItem.getCreationDate(), durationReportItem.getTerminationDate(), null, null);


      retVal.add(usageReportItem); // no cpu data for this stack, so adding default report item
      log.debug("getUsageReport, adding running row on {}", durationReportItem.getStackId());

    } else { // have per-instance CPU info, so need to add multiple rows
      for(CPUReportItem cpuItem : cpuReports) { //add cpu usage info rows

        UsageReportItem usageReportItemForInstance =
                new UsageReportItem(durationReportItem.getStackId(),
                        durationReportItem.getUserId(), durationReportItem.getActivityId(),
                        durationReportItem.getDurationExistence(), durationReportItem.getDurationRunning(),
                        durationReportItem.getDurationStopped(),
                        durationReportItem.getCreationDate(), durationReportItem.getTerminationDate(),
                        cpuItem.getInstanceId(), cpuItem.getCpuload());

        retVal.add(usageReportItemForInstance);
        log.debug("getUsageReport, adding running row for instance {} with cpu info {} on {}",
                cpuItem.getInstanceId(), cpuItem.getCpuload(), durationReportItem.getStackId());
      }
    }

    return retVal;
  }

  @GET
  @Path("duration")
  @Produces(APPLICATION_JSON)
  public List<DurationReportItem> getStatesDuration(@QueryParam("start") String start, @QueryParam("end") String end) {

    Date endDate = parseEndDate(end);
    Date startDate = parseStartDate(start, endDate);
    log.debug("getStatesDuration, start={}, end={}", start, end);
    log.debug("getStatesDuration parsed dates {} to {}", startDate, endDate);

    //get all state change events for all stacks and users, for the specified date range
    List<VirtualPrivateGridStateEntry> stateEntries = vpgStateFactory.get(startDate, endDate);

    Map<String, VpgStateReportInfo> stateInfoByStack = new HashMap<String, VpgStateReportInfo>();

    List<DurationReportItem> durationReportItems = new ArrayList<DurationReportItem>();

    log.debug("Begin retrieval of the statuses of the stacks");
    Map<String, String> stackStatuses = getStackStatuses();
    log.debug("Finished retrieval of the statuses of the stacks");


    //1st pass, setting initial states and filling structures.
    // at the end we will have activeStates map filled with initial stateEntries, 
    // and durationByStackUserState filled with DurationReport objects with zero duration
    for (VirtualPrivateGridStateEntry stateEntry : stateEntries) {

      VpgStateReportInfo vpgStateReportInfo = stateInfoByStack.get(stateEntry.getStackId());
      if (vpgStateReportInfo == null) {
        // this is the first time we encountered this stack/user/state combination
        vpgStateReportInfo = new VpgStateReportInfo(stateEntry.getStackId(), stateEntry.getUserId().toString(),
                stateEntry.getActivityId().toString());


        //getting active state for current stack/user key
        String key = stateEntry.getStackId() + stateEntry.getUserId().toString();


        VirtualPrivateGridStateEntry initialState = vpgStateFactory.getActiveState(stateEntry.getStackId(), stateEntry.getUserId(), startDate);
        if (null == initialState) {
          vpgStateReportInfo.setInitialStateEntry(stateEntry);

          log.debug("getStatesDuration, no initial state for state entry, putting stack {}, user {}, state {}, timestamp {}",
                  stateEntry.getStackId(), stateEntry.getUserId(), stateEntry.getState().name(), stateEntry.getTimestamp());
        } else {
          initialState.setTimestamp(startDate); // put range start date as 1st event timestamp

          vpgStateReportInfo.setInitialStateEntry(initialState);

          log.debug("getStatesDuration, putting initial state for state entry, {}, user {}, state {}, timestamp {}",
                  initialState.getStackId(), initialState.getUserId(), initialState.getState().name(), initialState.getTimestamp());
        }

        VirtualPrivateGrid vpg = vpgFactory.getByStackId(vpgStateReportInfo.getStackId());
        if (vpg != null) {
          vpgStateReportInfo.setEc2InstanceIds(vpg.getEc2InstanceIds());
        }

        if (stackStatuses.containsKey(vpgStateReportInfo.getStackId())) {
          vpgStateReportInfo.setStackStatus(stackStatuses.get(vpgStateReportInfo.getStackId()));
        }
        stateInfoByStack.put(stateEntry.getStackId(), vpgStateReportInfo);
      }

      vpgStateReportInfo.addStateEntry(stateEntry);

    } //now we have initial states set for all stack/user keys


    for (VpgStateReportInfo vpgStateReportInfo : stateInfoByStack.values()) {
      processStatesDurations(vpgStateReportInfo, startDate, endDate);

      durationReportItems.add(vpgStateReportInfo.toDurationReportItem());
    }


    if (log.isDebugEnabled()) { //dumping the stuff to return
      for (DurationReportItem item : durationReportItems) {
        log.debug("getStatesDuration, return dump item {}, user {}, duration of existence {}, duration in running state {}, duration in stopped state {}, creation date {}",
                item.getStackId(), item.getUserId(), item.getDurationExistence(), item.getDurationRunning(), item.getDurationStopped(), item.getCreationDate());
      }
    }

    return durationReportItems;
  }

  private void processStatesDurations(VpgStateReportInfo vpgStateReportInfo, Date startDate, Date endDate) {

    VirtualPrivateGridStateEntry previousStateEntry = vpgStateReportInfo.getInitialStateEntry();

    // Process state entries for durations
    for ( VirtualPrivateGridStateEntry stateEntry : vpgStateReportInfo.getStateEntries()) {

      String key = stateEntry.getStackId() + stateEntry.getUserId().toString();
      log.debug("getStatesDuration, processing the key for, {}, user {}, state {}", stateEntry.getStackId(), stateEntry.getUserId(), stateEntry.getState().name());

      long durationBetweenStates = stateEntry.getTimestamp().getTime() - previousStateEntry.getTimestamp().getTime();

      if (stateEntry.getState() == VirtualPrivateGridStateEntry.State.CREATED) {
        //vpgStateReportInfo.addToDurationInRunningState(durationBetweenStates);
        vpgStateReportInfo.setStackCreationDate(stateEntry.getTimestamp().getTime());

      } else if (stateEntry.getState() == VirtualPrivateGridStateEntry.State.RUNNING) {
        if ((previousStateEntry.getState() == VirtualPrivateGridStateEntry.State.RUNNING) ||
                (previousStateEntry.getState() == VirtualPrivateGridStateEntry.State.CREATED)) {

          log.debug("Stack {}, user {} : Adding running duration duration {}",
                  vpgStateReportInfo.getStackId(), vpgStateReportInfo.getUserId(), durationBetweenStates);
          vpgStateReportInfo.addToDurationInRunningState(durationBetweenStates);

        } else {

          log.debug("Stack {}, user {} : Adding stopped duration duration {}",
                  vpgStateReportInfo.getStackId(), vpgStateReportInfo.getUserId(), durationBetweenStates);
          vpgStateReportInfo.addToDurationInStoppedState(durationBetweenStates);
        }

      } else if (stateEntry.getState() == VirtualPrivateGridStateEntry.State.STOPPED) {
        if (previousStateEntry.getState() == VirtualPrivateGridStateEntry.State.STOPPED) {
          log.debug("Stack {}, user {} : Adding stopped duration duration {}",
                  vpgStateReportInfo.getStackId(), vpgStateReportInfo.getUserId(), durationBetweenStates);
          vpgStateReportInfo.addToDurationInStoppedState(durationBetweenStates);

        } else {
          log.debug("Stack {}, user {} : Adding running duration duration {}",
                  vpgStateReportInfo.getStackId(), vpgStateReportInfo.getUserId(), durationBetweenStates);
          vpgStateReportInfo.addToDurationInRunningState(durationBetweenStates);
        }

      } else if (stateEntry.getState() == VirtualPrivateGridStateEntry.State.TERMINATED) {
        vpgStateReportInfo.setStackTerminationDate(stateEntry.getTimestamp().getTime());

        if (previousStateEntry.getState() == VirtualPrivateGridStateEntry.State.STOPPED) {
          log.debug("Stack {}, user {} : Adding stopped duration duration {}",
                  vpgStateReportInfo.getStackId(), vpgStateReportInfo.getUserId(), durationBetweenStates);

          vpgStateReportInfo.addToDurationInStoppedState(durationBetweenStates);

        } else {
          log.debug("Stack {}, user {} : Adding running duration duration {}",
                  vpgStateReportInfo.getStackId(), vpgStateReportInfo.getUserId(), durationBetweenStates);

          vpgStateReportInfo.addToDurationInRunningState(durationBetweenStates);
        }
      }

      if (VirtualPrivateGridStateEntry.State.CREATED.equals(previousStateEntry.getState())) {
        vpgStateReportInfo.setStackCreationDate(previousStateEntry.getTimestamp().getTime()); //save creation timestamp.
        log.debug("getStatesDuration, setting creation date for item, {}, user {}, state {}",
                vpgStateReportInfo.getStackId(), vpgStateReportInfo.getUserId(), previousStateEntry.getState());
      }


      previousStateEntry = stateEntry;
    }


    // now we processed all state change events, need to add the "till endDate" duration for all active states we have
    long durationBetweenLastStateAndEndDate = endDate.getTime() - previousStateEntry.getTimestamp().getTime();

    if (previousStateEntry.getState() == VirtualPrivateGridStateEntry.State.RUNNING ||
            previousStateEntry.getState() == VirtualPrivateGridStateEntry.State.CREATED) {

      log.debug("Stack {}, user {} : Adding running duration duration {}",
              vpgStateReportInfo.getStackId(), vpgStateReportInfo.getUserId(), durationBetweenLastStateAndEndDate);
      vpgStateReportInfo.addToDurationInRunningState(durationBetweenLastStateAndEndDate);

    } else if (previousStateEntry.getState() == VirtualPrivateGridStateEntry.State.STOPPED) {

      log.debug("Stack {}, user {} : Adding stopped duration duration {}",
              vpgStateReportInfo.getStackId(), vpgStateReportInfo.getUserId(), durationBetweenLastStateAndEndDate);
      vpgStateReportInfo.addToDurationInStoppedState(durationBetweenLastStateAndEndDate);

    }

    if (vpgStateReportInfo.getStackCreationDate() == null) {
      // there were no CREATE event in range, so we didn't store proper creation date

      Date creationDate = vpgStateFactory.getCreationDate(vpgStateReportInfo.getStackId(), UUID.fromString(vpgStateReportInfo.getUserId()));
      if (null != creationDate) {
        vpgStateReportInfo.setStackCreationDate(creationDate.getTime());
        log.debug("Setting out-of-range creation date for stack {}, user {}, creation date {}",
                vpgStateReportInfo.getStackId(), vpgStateReportInfo.getUserId(), creationDate);
      }

    }

    //If the stack creation silently failed behind the scenes, ensure that the termination date is adjusted.
    String stackStatus = vpgStateReportInfo.getStackStatus();
    if (StackStatus.CREATE_FAILED.name().equalsIgnoreCase(stackStatus)) {
      vpgStateReportInfo.setStackTerminationDate(vpgStateReportInfo.getStackCreationDate());
      vpgStateReportInfo.setDurationInRunningState(0L);
      vpgStateReportInfo.setDurationInStoppedState(0L);
    }

    //Now determine the duration for which the environment is in existence
    if (vpgStateReportInfo.getStackCreationDate() != null && vpgStateReportInfo.getStackCreationDate() > 0) {

      Long timeExistenceStart = vpgStateReportInfo.getStackCreationDate();
      if (timeExistenceStart < startDate.getTime()) {
        timeExistenceStart = startDate.getTime();
      }

      Long timeExistenceEnd;
      if (vpgStateReportInfo.getStackTerminationDate() != null && vpgStateReportInfo.getStackTerminationDate() > 0) {
        timeExistenceEnd = vpgStateReportInfo.getStackTerminationDate();
      } else {
        timeExistenceEnd = endDate.getTime();
      }

      vpgStateReportInfo.setDurationInExistence(timeExistenceEnd - timeExistenceStart);
    }

  }

  @AWSApiCall
  protected Map<String, String> getStackStatuses() {

    Map<String, String> stackStatuses = new HashMap<>();
    try {

      DescribeStacksRequest stackRequest = new DescribeStacksRequest();

      AWSCredentialsProvider credentials = new SystemPropertiesCredentialsProvider();
      AmazonCloudFormationClient cloudformationClient = new AmazonCloudFormationClient(credentials.getCredentials());

      DescribeStacksResult stackResults = cloudformationClient.describeStacks(stackRequest);
      List<Stack> stacks = stackResults.getStacks();

      for (Stack stack : stacks) {
        stackStatuses.put(stack.getStackId(), stack.getStackStatus());
      }


    } catch (Exception e) {
      log.error("Unable to retrieve status for stacks", e);
    }

    return stackStatuses;
  }

  @GET
  @Path("cpuusagemedian")
  @Produces(APPLICATION_JSON)
  /**
   * Produces CPU usage report for instances of the specified stack, for the specified date range.
   * the cpuload value returned for each instance is a median value of calculated 1-hour-averages of
   * cpu utilization data gathered from our local measurements history DB.
   */
  public List<CPUReportItem> getCpuUsageMedian(@QueryParam("stackid") String stackId, @QueryParam("start") String start, @QueryParam("end") String end) {
    
    Date endDate = parseEndDate(end);
    Date startDate = parseStartDate(start, endDate);
    log.debug("getCpuUsageMedian, start={}, end={}", start, end);
    log.debug("getCpuUsageMedian parsed dates {} to {}", startDate, endDate);
    
    ArrayList<CPUReportItem> retVal = new ArrayList<CPUReportItem>();

    List<VirtualPrivateGridMeasurementEntry> instances = VPGMeasurementsFactory.getByStack(stackId, startDate, endDate); 
    HashSet<String> uniqueInstances = new HashSet<String>();
    
    for (VirtualPrivateGridMeasurementEntry instance : instances) {
      if (uniqueInstances.contains(instance.getInstanceId())) {
        log.info("Already processed instanceId {}, skipping", instance.getInstanceId());
        continue;
      }
      uniqueInstances.add(instance.getInstanceId());
      Double medianCPU = VPGMeasurementsFactory.getInstanceMedianOfHourlyMeasurements(instance.getInstanceId(), startDate, endDate);
      retVal.add(new CPUReportItem(stackId, instance.getInstanceId(), medianCPU, startDate.getTime()));
    }
    
    return retVal;
  }
  
  
  @GET
  @Path("cpuusagehistory")
  @Produces(APPLICATION_JSON)
  /**
   * Get CPU utilization historical data stored in VPG DB, 
   * which is supposed to be updated from CloudWatch periodically, for all running stacks. 
   * 
   */
  public List<CPUReportItem> getCpuUsageHistory(@QueryParam("stackid") String stackId, @QueryParam("instanceid") String instanceId, @QueryParam("start") String start, @QueryParam("end") String end) {

    Date endDate = parseEndDate(end);
    Date startDate = parseStartDate(start, endDate);
    log.debug("getCpuUsageHistory, start={}, end={}", start, end);
    log.debug("getCpuUsageHistory parsed dates {} to {}", startDate, endDate);  

    //TODO: should it return the measurement object itself?
    List<VirtualPrivateGridMeasurementEntry> entries = VPGMeasurementsFactory.get(stackId, instanceId, startDate, endDate);
    
    //TODO: add stats aggregation/averaging here?
    ArrayList<CPUReportItem> retVal = new ArrayList<CPUReportItem>();
    for (VirtualPrivateGridMeasurementEntry entry : entries) {
      retVal.add(new CPUReportItem(stackId, instanceId, entry.getMeasurementValue(), entry.getMeasurementTime().getTime()));
    }
    
    return retVal;
  }
  
  @GET
  @Path("cpuusage")
  @Produces(APPLICATION_JSON)
  /**
   * Get CPU utilization directly from CloudWatch. 
   * Up to one day interval. History goes back as deep as two weeks only.
   * 5-minute interval between datapoints.
   * 
   */
  public List<CPUReportItem> getCpuUsage(@QueryParam("stackid") String stackId, @QueryParam("instanceid") String instanceId, @QueryParam("start") String start, @QueryParam("end") String end) {

    Date endDate = parseEndDate(end);
    Date startDate = parseStartDate(start, endDate);
    log.debug("getCpuUsage, start={}, end={}", start, end);
    log.debug("getCpuUsage parsed dates {} to {}", startDate, endDate);  


    AWSCredentialsProvider credentials = new SystemPropertiesCredentialsProvider();
    AmazonCloudWatchClient cloudWatch = new AmazonCloudWatchClient(credentials.getCredentials());
    //cloudWatch.setEndpoint("monitoring.us-east-1.amazonaws.com");
    Dimension instanceDimension = new Dimension();
    instanceDimension.setName("instanceid");
    instanceDimension.setValue(instanceId);
    
    GetMetricStatisticsRequest request = new GetMetricStatisticsRequest()
      .withStartTime(startDate).withEndTime(endDate).withPeriod(60 * 60)  
      .withNamespace("AWS/EC2")      
      .withMetricName("CPUUtilization")
      .withStatistics("Average")
      .withDimensions(Arrays.asList(instanceDimension));
    GetMetricStatisticsResult result = cloudWatch.getMetricStatistics(request);
    
    ArrayList<CPUReportItem> retVal = new ArrayList<CPUReportItem>();
    for (Datapoint dp : result.getDatapoints()) {
      retVal.add(new CPUReportItem(stackId, instanceId, dp.getAverage(), dp.getTimestamp().getTime()));
    }
    
    return retVal;
  }
  

  private Date parseEndDate(String end) {
    Date endDate;
    try {
      SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); //TODO: no timezone in time format yet
      if (StringUtils.isNotEmpty(end)) {
        endDate = formatter.parse(end);
      } else { // no end param means "till now"
        log.debug("empty end date, setting current time");
        endDate = new Date();
      }
    } catch (ParseException e) {
      log.error("end date parser error, end={}, setting current time", end);
      endDate = new Date();
    }
    return endDate;
  }
  
  private Date parseStartDate(String start, Date endDate) {
    Date startDate;
    try {
      SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); //TODO: no timezone in time format yet
      if (StringUtils.isNotEmpty(start)) {
        startDate = formatter.parse(start);
        if (! startDate.before(endDate)) { //check whether start date is before end date, use default 1-month interval if it is not
          log.debug("start date is not before end date,  setting it to 1 month prior the end date");
          startDate = DateUtils.addMonths(endDate, -1);
        }
      } else { // no start param means 1-month before the end
        log.debug("empty start date, setting it to 1 month prior the end date");
        startDate = DateUtils.addMonths(endDate, -1);
      }
    } catch (ParseException e) {
      log.error("start date parser error, start={}, setting it to 1 month prior the end date", start);
      startDate = DateUtils.addMonths(endDate, -1);
    }    
      
    return startDate;
  }
  
  public static class UsageReportItem {

    private String stackId;
    private String userId;
    private String activityId;

    private Long durationExistence;
    private Long durationRunning;
    private Long durationStopped;

    private Long creationDate;
    private Long terminationDate;
    private String instanceId;
    private Double cpuLoad;

  //no-arg constructor is needed for serialization-deserialization to work properly in Jersey
    public UsageReportItem(){
    }
    public UsageReportItem(String stackId, String userId, String activityId,
                           Long durationExistence, Long durationRunning, Long durationStopped,
                           Long creationDate, Long terminationDate, String instanceId, Double cpuLoad) {
      super();
      this.stackId = stackId;
      this.userId = userId;
      this.activityId = activityId;

      this.durationExistence = durationExistence;
      this.durationRunning = durationRunning;
      this.durationStopped = durationStopped;

      this.creationDate = creationDate;
      this.terminationDate = terminationDate;
      this.instanceId = instanceId;
      this.cpuLoad = cpuLoad;
    }
    
    public String getStackId() {
      return stackId;
    }
    public void setStackId(String stackId) {
      this.stackId = stackId;
    }
    public String getUserId() {
      return userId;
    }
    public void setUserId(String userId) {
      this.userId = userId;
    }
    public String getActivityId() {
      return activityId;
    }
    public void setActivityId(String activityId) {
      this.activityId = activityId;
    }

    public Long getDurationExistence() {
      return durationExistence;
    }

    public void setDurationExistence(Long durationExistence) {
      this.durationExistence = durationExistence;
    }

    public Long getDurationRunning() {
      return durationRunning;
    }

    public void setDurationRunning(Long durationRunning) {
      this.durationRunning = durationRunning;
    }

    public Long getDurationStopped() {
      return durationStopped;
    }

    public void setDurationStopped(Long durationStopped) {
      this.durationStopped = durationStopped;
    }

    public Long getCreationDate() {
      return creationDate;
    }
    public void setCreationDate(Long creationDate) {
      this.creationDate = creationDate;
    }

    public Long getTerminationDate() {
      return terminationDate;
    }

    public void setTerminationDate(Long terminationDate) {
      this.terminationDate = terminationDate;
    }

    public String getInstanceId() {
      return instanceId;
    }
    public void setInstanceId(String instanceId) {
      this.instanceId = instanceId;
    }
    public Double getCpuLoad() {
      return cpuLoad;
    }
    public void setCpuLoad(Double cpuLoad) {
      this.cpuLoad = cpuLoad;
    }
    
    

  }

  private static class VpgStateReportInfo {

    private String stackId;
    private String userId;
    private String activityId;

    private VirtualPrivateGridStateEntry initialStateEntry;
    private List<VirtualPrivateGridStateEntry> stateEntries = new LinkedList<VirtualPrivateGridStateEntry>();

    private Long durationInExistence;
    private Long durationInRunningState;
    private Long durationInStoppedState;

    private Long stackCreationDate;
    private Long stackTerminationDate;

    private String ec2InstanceIds;
    private String stackStatus;


    public VpgStateReportInfo(String stackId, String userId, String activityId) {
      this.stackId = stackId;
      this.userId = userId;
      this.activityId = activityId;

      this.durationInRunningState = 0L;
      this.durationInStoppedState = 0L;

      this.stackCreationDate = null; //this means unknown creation date
      this.stackTerminationDate = null; //this means unknown termination date
    }

    public void addStateEntry(VirtualPrivateGridStateEntry stateEntry) {
      stateEntries.add(stateEntry);
    }

    public List<VirtualPrivateGridStateEntry> getStateEntries() {
      return stateEntries;
    }

    public VirtualPrivateGridStateEntry getInitialStateEntry() {
      return initialStateEntry;
    }

    public void setInitialStateEntry(VirtualPrivateGridStateEntry initialStateEntry) {
      this.initialStateEntry = initialStateEntry;
    }

    public String getStackId() {
      return stackId;
    }

    public String getUserId() {
      return userId;
    }

    public String getActivityId() {
      return activityId;
    }


    public String getEc2InstanceIds() {
      return ec2InstanceIds;
    }

    public void setEc2InstanceIds(String ec2InstanceIds) {
      this.ec2InstanceIds = ec2InstanceIds;
    }

    public Long getDurationInRunningState() {
      return durationInRunningState;
    }

    public void setDurationInRunningState(Long durationInRunningState) {
      this.durationInRunningState = durationInRunningState;
    }

    public Long getDurationInStoppedState() {
      return durationInStoppedState;
    }

    public void setDurationInStoppedState(Long durationInStoppedState) {
      this.durationInStoppedState = durationInStoppedState;
    }


    public Long getStackCreationDate() {
      return stackCreationDate;
    }

    public Long getStackTerminationDate() {
      return stackTerminationDate;
    }

    public void setStackCreationDate(Long stackCreationDate) {
      this.stackCreationDate = stackCreationDate;
    }

    public void setStackTerminationDate(Long stackTerminationDate) {
      this.stackTerminationDate = stackTerminationDate;
    }

    public String getStackStatus() {
      return stackStatus;
    }

    public void setStackStatus(String status) {
      this.stackStatus = status;
    }

    public void addToDurationInRunningState(long durationToAdd) {
      if (durationToAdd > 0) {
        this.durationInRunningState += durationToAdd;
      }
    }

    public void addToDurationInStoppedState(long durationToAdd) {
      if (durationToAdd > 0) {
        this.durationInStoppedState += durationToAdd;
      }
    }

    public Long getDurationInExistence() {
      return durationInExistence;
    }

    public void setDurationInExistence(Long durationInExistence) {
      this.durationInExistence = durationInExistence;
    }

    public DurationReportItem toDurationReportItem() {


      DurationReportItem durationReportItem =
              new DurationReportItem(stackId, userId, activityId);

      durationReportItem.setDurationRunning(durationInRunningState);
      durationReportItem.setDurationStopped(durationInStoppedState);
      durationReportItem.setDurationExistence(durationInExistence);


      if (stackCreationDate != null && stackCreationDate > 0) {
        durationReportItem.setCreationDate(stackCreationDate);
      }
      if (stackTerminationDate != null && stackTerminationDate > 0) {
        durationReportItem.setTerminationDate(stackTerminationDate);
      }

      return durationReportItem;
    }
  }

  
  public static class DurationReportItem {

    private String stackId;
    private String userId;
    private String activityId;

    private Long creationDate;
    private Long terminationDate;

    private Long durationExistence;
    private Long durationRunning;
    private Long durationStopped;



    //no-arg constructor is needed for serialization-deserialization to work properly in Jersey
    public DurationReportItem(){
    }

    public DurationReportItem(String stackId, String userId, String activityId) {
      this.stackId = stackId;
      this.userId = userId;
      this.activityId = activityId;

      this.creationDate = null; //this means unknown creation date
      this.terminationDate = null;
      this.durationExistence = null;
      this.durationRunning = null;
      this.durationStopped = null;
    }
    
    public String getStackId() {
      return stackId;
    }
    public void setStackId(String stackId) {
      this.stackId = stackId;
    }
    
    public String getActivityId() {
      return activityId;
    }
    public void setActivityId(String activityId) {
      this.activityId = activityId;
    }

    public String getUserId() {
      return userId;
    }
    public void setUserId(String userId) {
      this.userId = userId;
    }

    public Long getCreationDate() {
      return creationDate;
    }
    public void setCreationDate(Long creationDate) {
      this.creationDate = creationDate;
    }

    public Long getTerminationDate() {
      return terminationDate;
    }

    public void setTerminationDate(Long terminationDate) {
      this.terminationDate = terminationDate;
    }

    public Long getDurationExistence() {
      return durationExistence;
    }

    public void setDurationExistence(Long durationExistence) {
      this.durationExistence = durationExistence;
    }

    public Long getDurationRunning() {
      return durationRunning;
    }

    public void setDurationRunning(Long durationRunning) {
      this.durationRunning = durationRunning;
    }

    public Long getDurationStopped() {
      return durationStopped;
    }

    public void setDurationStopped(Long durationStopped) {
      this.durationStopped = durationStopped;
    }

    @Override
    public String toString() {
      return String.format("StackId : %s, UserId : %s, DurationExistence : %d, DurationRunning : %d, DurationStopped : %d, CreationDate : %d",
              getStackId(), getUserId(), getDurationExistence(), getDurationRunning(), getDurationStopped(), getCreationDate());
    }
  }

  public static class CPUReportItem {
    private String stackId;
    private String instanceId;
    private Double cpuload;
    private Long timestamp;

    //no-arg constructor is needed for serialization-deserialization to work properly in Jersey
    public CPUReportItem(){
    }

    public CPUReportItem(String stackId, String instanceId, Double cpuload, Long timestamp) {
      super();
      this.stackId = stackId;
      this.instanceId = instanceId;
      this.cpuload = cpuload;
      this.timestamp = timestamp;
    }

    
    public String getStackId() {
      return stackId;
    }
    public void setStackId(String stackId) {
      this.stackId = stackId;
    }
    public String getInstanceId() {
      return instanceId;
    }
    public void setInstanceId(String instanceId) {
      this.instanceId = instanceId;
    }
    public Double getCpuload() {
      return cpuload;
    }
    public void setCpuload(Double cpuload) {
      this.cpuload = cpuload;
    }
    public Long getTimestamp() {
      return timestamp;
    }
    public void setTimestamp(Long timestamp) {
      this.timestamp = timestamp;
    }
  }


  
}
