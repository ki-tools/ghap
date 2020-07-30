package io.ghap.reporting.service.impl;

import com.google.inject.Inject;
import io.ghap.logevents.EventItem;
import io.ghap.logevents.Types;
import io.ghap.oauth.OAuthUser;
import io.ghap.reporting.data.*;
import io.ghap.reporting.service.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
public class ReportGenerationServiceBean implements ReportGenerationService {

  private static final String DATA_CURATOR_ROLE = "Data Curator";
  private final static String EMPTY_STRING = "";
  private static final String NO_DATA = "No Data";
  private static final String NULL_DATA = "";

  @Inject
  private UserService userService;

  @Inject
  private CSVService csvService;

  @Inject
  private ProjectService projectService;

  @Inject
  private VPGService vpgService;

  @Inject
  private ActivityService activityService;

  @Inject
  private DataSubmissionService dataSubmissionService;

  @Override
  public String generateUsersReport(String accessToken) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
    List<User> users = userService.loadUsers(accessToken);
    StringBuilder sb = new StringBuilder();
    csvService.printRecords(sb, users,
            Arrays.asList("Username", "First Name", "Last Name", "Email", "Account Status"),
            "name", "firstName", "lastName", "email", "status");
    return sb.toString();
  }

  @Override
  public String generateGroupsReport(String accessToken) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
    List<UserServiceBean.GroupWithUsers> groupWithUsers = userService.loadGroupsWithUsers(accessToken);
    StringBuilder sb = new StringBuilder();
    List<FlatUserGroup> flatUserGroups = convertToFlatList(groupWithUsers);
    csvService.printRecords(sb, flatUserGroups,
            Arrays.asList("Group", "Username", "First Name", "Last Name", "Email", "Account Status"),
            "groupName", "name", "firstName", "lastName", "email", "status");
    return sb.toString();
  }

  @Override
  public String generateRolesReport(String accessToken) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
    List<UserServiceBean.GroupWithUsers> groupWithUsers = userService.loadRolesWithUsers(accessToken);
    StringBuilder sb = new StringBuilder();
    List<FlatUserGroup> flatUserGroups = convertToFlatList(groupWithUsers);
    csvService.printRecords(sb, flatUserGroups,
            Arrays.asList("Role", "Username", "First Name", "Last Name", "Email", "Account Status"),
            "groupName", "name", "firstName", "lastName", "email", "status");
    return sb.toString();
  }

  @Override
  public String generateProgramReport(String accessToken) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
    List<ProjectReportItem> items = projectService.loadProjects(accessToken);
    excludeGrantsWithNotExistingUsers(accessToken, items);
    StringBuilder sb = new StringBuilder();
    List<FlatProgramReport> flatList = convertToProjectFlatList(items);
    csvService.printRecords(sb, flatList,
            Arrays.asList("Program", "Username", "Email", "Permissions"),
            "programName", "username", "userEmail", "permissions");
    return sb.toString();
  }

  @Override
  public String generateGrantReport(String accessToken) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
    List<ProjectReportItem> items = projectService.loadGrants(accessToken);
    excludeGrantsWithNotExistingUsers(accessToken, items);
    StringBuilder sb = new StringBuilder();
    List<FlatProgramReport> flatList = convertToProjectFlatList(items);
    csvService.printRecords(sb, flatList,
            Arrays.asList("Grant", "Program", "Username", "Email", "Permissions"),
            "grantName", "programName", "username", "userEmail", "permissions");
    return sb.toString();
  }

  @Override
  public String generateSystemUsageReport(String accessToken, Date start, Date end, String instanceType, boolean excludeNotCommisioned) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
    List<UsageReportItem> usageReportItems = vpgService.loadUsageData(accessToken, start, end, instanceType);
//    if (excludeNotCommisioned && usageReportItems != null) {
//      for (Iterator<UsageReportItem> it = usageReportItems.iterator(); it.hasNext();) {
//        if (it.next().getDurationExistence() <= 0) {
//          it.remove();
//        }
//      }
//    }
    List<? extends FlatDurationReport> flatList = convertToDurationReportFlat(accessToken, usageReportItems, instanceType, excludeNotCommisioned);
    StringBuilder sb = new StringBuilder();
    csvService.printRecords(sb, flatList,
            Arrays.asList("Username", "User Email", "User Status",
                    "Activity Name", "InstanceId",

                    "Environment Commissioned - Duration (Hours)",
                    "Environment Running - Duration (Hours)",
                    "Environment Stopped - Duration (Hours)",

                    "Environment Creation Date", "Environment Termination Date",
                    "Average CPU Load (%)",
                     "StackId"),
            "username", "userEmail", "userStatus",
            "activityName", "instanceId",

            "durationExistence", "durationRunning", "durationStopped",
            
            "creationDate", "terminationDate",
            "cpuLoad", "stackId"

    );
    return sb.toString();
  }

  @Override
  public String generateCPUUsageReport(String accessToken, Date start, Date end) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
    List<CPUReportItem> items = vpgService.loadCPUData(accessToken, start, end);
    StringBuilder sb = new StringBuilder();
    csvService.printRecords(sb, items,
            Arrays.asList("StackId", "InstanceId", "CPU Load", "Timestamp"),
            "stackId", "instanceId", "cpuload", "timestamp");
    return sb.toString();
  }

  @Override
  public String generateDataSubmissionReport(String accessToken, Date start, Date end, boolean excludeDataCurators) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
    List<EventItem> datasetItems = dataSubmissionService.loadReportData(Types.DATASET_DOWNLOAD, accessToken, start, end);
    List<EventItem> userWorkspaceItems = dataSubmissionService.loadReportData(Types.USER_WORKSPACE_DOWNLOAD, accessToken, start, end);
    if (excludeDataCurators) {
      filterDatasubmissionItems(datasetItems);
      filterDatasubmissionItems(userWorkspaceItems);
    }

    List<DatasubmissionReportItem> result = new ArrayList<>(datasetItems.size() + userWorkspaceItems.size());
    for (EventItem item : datasetItems) {
      result.add(new DatasubmissionReportItem(item, "Dataset Download"));
    }
    for (EventItem item : userWorkspaceItems) {
      result.add(new DatasubmissionReportItem(item, "User Workspace Download"));
    }
    result.stream().sorted(new Comparator<DatasubmissionReportItem>(){
      @Override
      public int compare(DatasubmissionReportItem o1, DatasubmissionReportItem o2) {
        return o1.getTimestamp().compareTo(o2.getTimestamp());
      }
    });
    StringBuilder sb = new StringBuilder();
    csvService.printRecords(sb, result,
            Arrays.asList("User Name", "Email", "Roles", "Client Ip", "Remote Ip", "Date and Time of Download", "File Name", "File Type", "File Size", "Match Git Repo", "Dataset Source"),
            "userName", "email", "roles", "clientip", "remoteip", "timestamp", "fileName", "fileType", "fileSize", "matchWithGitMasterRepo", "type");
    return sb.toString();
  }

  /*
  public static void main(String[] args) throws Exception {
    List<DatasubmissionReportItem> result = new ArrayList<>(1);
    EventItem item = new EventItem();
    List<String> roles = new ArrayList<>();
    roles.add("mmm");
    roles.add("ooo");
    item.setRoles(roles);
    result.add(new DatasubmissionReportItem(item, "User Workspace Download"));
    CSVServiceBean csvService = new CSVServiceBean();
    StringBuilder sb = new StringBuilder();
    csvService.printRecords(sb, result,
            Arrays.asList("User Name", "Email", "Roles", "Client Ip", "Remote Ip", "Timestamp", "File Name", "File Type", "File Size", "Match Git Repo", "Report Type"),
            "userName", "email", "roles", "clientip", "remoteip", "timestamp", "fileName", "fileType", "fileSize", "matchWithGitMasterRepo", "type");
    System.out.println("===>" + sb);
  }
  */

  @Override
  public String generatePermissionMismatchReport(String accessToken) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
    List<PermissionMismatchItem> items = projectService.loadPermissions(accessToken);
    StringBuilder sb = new StringBuilder();
    csvService.printRecords(sb, items,
            Arrays.asList("Program", "Grant", "Group", "User", "Synchronized", "Exist in GIT", "Exist in Database", "Permission in GIT", "Permission in Database"),
            "program", "grant", "group", "user", "match", "existInStash", "existInDb", "stashPermissions", "dbPermissions");
    return sb.toString();
  }

  private List<FlatUserGroup> convertToFlatList(List<UserServiceBean.GroupWithUsers> groupWithUsers) {
    List<FlatUserGroup> result = new ArrayList<>(groupWithUsers.size());
    for (UserServiceBean.GroupWithUsers group : groupWithUsers) {
      for (User user : group.getUsers()) {
        result.add(new FlatUserGroup(group.getGroup(), user));
      }
    }
    return result;
  }

  private List<FlatProgramReport> convertToProjectFlatList(List<ProjectReportItem> items) {
    List<FlatProgramReport> result = new ArrayList<>(items.size());
    for (ProjectReportItem item : items) {
      result.add(new FlatProgramReport(item));
    }
    return result;
  }

  public static class FlatUserGroup {
    private Group group;
    private User user;

    public FlatUserGroup(Group group, User user) {
      this.group = group;
      this.user = user;
    }

    public String getGroupName() {
      return group.getName();
    }

    public String getLastName() {
      return user == null ? "" : user.getFirstName();
    }

    public String getFirstName() {
      return user == null ? "" : user.getLastName();
    }

    public String getEmail() {
      return user == null ? "" : user.getEmail();
    }

    public String getStatus() {
      return user == null ? "" : user.getStatus();
    }

    public String getName() {
      return user == null ? "" : user.getName();
    }
  }

  public static class FlatProgramReport {
    private ProjectReportItem reportItem;

    public FlatProgramReport(ProjectReportItem reportItem) {
      this.reportItem = reportItem;
    }

    public String getProgramName() {
      String result;
      if (reportItem.getProject() != null) {
        result = reportItem.getProject().getExternalId();
      } else if (reportItem.getGrant() != null && reportItem.getGrant().getProject() != null) {
        result = reportItem.getGrant().getProject().getExternalId();
      } else {
        result = "";
      }
      return result.endsWith("KEY") && result.length() > 3 ? result.substring(0, result.length() - 3) : result;
    }

    public String getGrantName() {
      return reportItem.getGrant() == null ? "" : reportItem.getGrant().getExternalId();
    }

    public String getPermissions() {
      return convertRulesToString(reportItem.getRules());
    }

    public String getUsername() {
      return reportItem.getUser() == null ? EMPTY_STRING : reportItem.getUser().getLogin();
    }

    public String getUserEmail() {
      return reportItem.getUser() == null ? EMPTY_STRING : reportItem.getUser().getEmail();
    }
  }

  private static String convertRulesToString(List<PermissionRule> rules) {
    if (CollectionUtils.isEmpty(rules)) {
      return EMPTY_STRING;
    }
    Iterator<PermissionRule> iterator = rules.iterator();
    StringBuilder sb = new StringBuilder(iterator.next().name());
    while (iterator.hasNext()) {
      sb.append(",").append(iterator.next().name());
    }
    return sb.toString();
  }

  private List<? extends FlatDurationReport> convertToDurationReportFlat(String accessToken, List<? extends DurationReportItem> items, String instanceType, boolean excludeNotCommisioned) {
    if (CollectionUtils.isEmpty(items)) {
      return Collections.emptyList();
    }
    List<User> users = userService.loadUsers(accessToken);
    List<Activity> activities = activityService.loadActivities(accessToken);
    Collections.sort(users);
    Collections.sort(activities);
    List<FlatDurationReport> result = new ArrayList<>(items.size());
    for (DurationReportItem item : items) {
      result.add(
              item instanceof UsageReportItem ?
                      new FlatUsageReport(users, activities, (UsageReportItem) item) :
                      new FlatDurationReport(users, activities, item)
      );
    }
    if (instanceType != null) {
      String trim = instanceType.trim();
      for (Iterator<FlatDurationReport> it = result.iterator(); it.hasNext(); ) {
        Activity activity = it.next().getActivity();
        if (activity == null) {
          it.remove();
          continue;
        }
        if (!trim.equalsIgnoreCase(activity.getOs())) {
          it.remove();
          continue;
        }
      }
    }

    // see BAP-2048
    Map<String, FlatDurationReport> userMap = new HashMap<>();
    for(FlatDurationReport r:result){
      userMap.put(r.getUserId(), r);
    }
    if (!excludeNotCommisioned) {
      for(User user:users){
        if( !userMap.containsKey(user.getGuid()) ){
          UsageReportItem fakeUsageReport = new UsageReportItem();
          fakeUsageReport.setUserId(user.getGuid());
          fakeUsageReport.setDurationStopped(0L);
          fakeUsageReport.setDurationRunning(0L);
          fakeUsageReport.setDurationExistence(0L);
          FlatUsageReport mockup = new FlatUsageReport(users, activities, fakeUsageReport);
          result.add(mockup);
        }
      }
    }
    return result;
  }

  private void excludeGrantsWithNotExistingUsers(String accessToken, List<ProjectReportItem> items) {
    List<User> users = userService.loadUsers(accessToken);
    Collections.sort(users);
    for (Iterator<ProjectReportItem> it = items.iterator(); it.hasNext(); ) {
      User u = new User();
      u.setGuid(it.next().getUser().getExternalId());
      int i = Collections.binarySearch(users, u);
      if (i < 0) {
        it.remove();
      }
    }
  }

  private static void filterDatasubmissionItems(List<EventItem> eventItems) {
    for (Iterator<EventItem> it = eventItems.iterator(); it.hasNext(); ) {
      EventItem next = it.next();
      //exclude data curators
      if (CollectionUtils.isNotEmpty(next.getRoles()) && next.getRoles().contains(DATA_CURATOR_ROLE)) {
        it.remove();
      }
    }
  }

  public static class FlatDurationReport {

    private List<User> users;
    private List<Activity> activities;
    protected DurationReportItem item;

    private User locatedUser;

    public FlatDurationReport(List<User> users, List<Activity> activities, DurationReportItem item) {
      this.users = users;
      this.activities = activities;
      this.item = item;
    }

    public String getStackId() {
      return item.getStackId();
    }

    public String getUserId() {
      return item.getUserId();
    }

    public String getDurationExistence() {

      return item.getDurationExistence() == null ? NULL_DATA : (item.getDurationExistence() < 0 ? NO_DATA :
              getDurationInHours(item.getDurationExistence()));
/*
//Excel messes up data in csv files that have HH:MM:SS formats
      return item.getDurationExistence() == null ? NULL_DATA : (item.getDurationExistence() < 0 ? NO_DATA :
              DurationFormatUtils.formatDurationHMS(item.getDurationExistence()));
*/
    }

    public String getDurationRunning() {

      return item.getDurationRunning() == null ? NULL_DATA : (item.getDurationRunning() < 0 ? NO_DATA :
              getDurationInHours(item.getDurationRunning()));
/*
//Excel messes up data in csv files that have HH:MM:SS formats
      return item.getDurationRunning() == null ? NULL_DATA : (item.getDurationRunning() < 0 ? NO_DATA :
              DurationFormatUtils.formatDurationHMS(item.getDurationRunning()));
*/
    }

    public String getDurationStopped() {

      return item.getDurationStopped() == null ? NULL_DATA : (item.getDurationStopped() < 0 ? NO_DATA :
              getDurationInHours(item.getDurationStopped()));
/*
//Excel messes up data in csv files that have HH:MM:SS formats
      return item.getDurationStopped() == null ? NULL_DATA : (item.getDurationStopped() < 0 ? NO_DATA :
              DurationFormatUtils.formatDurationHMS(item.getDurationStopped()));
*/
    }

    private String getDurationInHours(long durationMilliseconds) {
      double durationHours = ((double) durationMilliseconds) / DateUtils.MILLIS_PER_HOUR;
      NumberFormat formatter = DecimalFormat.getInstance();
      formatter.setMaximumFractionDigits(2);
      return formatter.format(durationHours);
    }

    public String getActivityName() {
      Activity activity = getActivity();
      return activity == null ? "NA" : activity.getActivityName();
    }

    public Activity getActivity() {
      if (item.getActivityId() != null) {
        Activity activity = new Activity();
        activity.setId(UUID.fromString(item.getActivityId()));
        int i = Collections.binarySearch(activities, activity);
        return i >= 0 ? activities.get(i) : null;
      } else {
        return null;
      }
    }

    public String getCreationDate() {
      return item.getCreationDate() == null ? NULL_DATA :
              (item.getCreationDate() == -1L ? NO_DATA :
                      DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG).format(new Date(item.getCreationDate())));
    }

    public String getTerminationDate() {
      return item.getTerminationDate() == null ? NULL_DATA :
              (item.getTerminationDate() == -1L ? NO_DATA :
                      DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG).format(new Date(item.getTerminationDate())));
    }

    public String getUsername() {
      User user = getUser();
      return user != null ? user.getName() : "";
    }

    public String getUserEmail() {
      User user = getUser();
      return user != null ? user.getEmail() : "";
    }

    public String getUserStatus() {
      User user = getUser();
      return user != null ? user.getStatus() : "";
    }

    private User getUser() {
      if (locatedUser == null) {
        User user = new User();
        user.setGuid(getUserId());
        int i = Collections.binarySearch(users, user);
        if (i >= 0) {
          locatedUser = users.get(i);
        }
      }
      return locatedUser;
    }
  }

  public static class FlatUsageReport extends FlatDurationReport {

    public FlatUsageReport(List<User> users, List<Activity> activities, DurationReportItem item) {
      super(users, activities, item);
    }

    public String getInstanceId() {
      String instanceId = ((UsageReportItem) item).getInstanceId();
      return instanceId == null ? "NA":instanceId;
    }

    public String getCpuLoad() {
      Double cpuLoad = ((UsageReportItem) item).getCpuLoad();

      if (cpuLoad == null) {
        return NULL_DATA;
      } else {
        return cpuLoad.intValue() == -1 ? NO_DATA : cpuLoad.toString();
      }
    }

  }

  public static class DatasubmissionReportItem {
    private EventItem eventItem;
    private String type;
    private String timestamp;

    public DatasubmissionReportItem(EventItem eventItem, String type) {
      this.eventItem = eventItem;
      this.type = type;
    }

    public String getClientip() {
      return eventItem.getClientip();
    }

    public String getTimestamp() {
      if (timestamp != null) {
        return timestamp;
      }
      timestamp = eventItem.getTimestamp();
      return timestamp;
    }

    public List<String> getRoles() {
      return eventItem.getRoles();
    }

    public String getEmail() {
      return eventItem.getEmail();
    }

    public String getRemoteip() {
      return eventItem.getRemoteip();
    }

    public String getFileName() {
      return eventItem.getFileName();
    }

    public String getFileType() {
      return eventItem.getFileType();
    }

    public Long getFileSize() {
      return eventItem.getFileSize();
    }

    public Boolean getMatchWithGitMasterRepo() {
      return eventItem.getMatchWithGitMasterRepo();
    }

    public String getType() {
      return type;
    }

    public String getUserName() {
      return eventItem.getUserName();
    }
  }
}
