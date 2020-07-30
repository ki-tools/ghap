package io.ghap.reporting.data;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class MockReportFactoryImpl implements ReportFactory {

  private Map<UUID, Map<ReportType, Report>> map = new HashMap<>();

  @Override
  public String CreateReport(String accessToken, UUID uuid, ReportType reportType) {

    Calendar calendar = Calendar.getInstance();
    Date created = calendar.getTime();
    String token = getToken();

    if(!map.containsKey(uuid)) {
      Map<ReportType, Report> reports = new HashMap<>();
      map.put(uuid, reports);
    }
    Map<ReportType, Report> reports = map.get(uuid);
    Report report = new Report();
    report.setCreated(created.getTime());
    report.setName(String.format("%s-%d", reportType, created.getTime()));
    report.setOwner(uuid);
    report.setToken(token);
    report.setContentType("txt/csv");
    report.setFilename("dummy_report.csv");
    report.setReportType(reportType);
    reports.put(reportType, report);
    return token;
  }

  @Override
  public String CreateReport(String accessToken, UUID uuid, ReportType reportType, List<Constraint> constraints) {
    return CreateReport(accessToken, uuid, reportType);
  }

  @Override
  public Report GetReport(String token) {
    for(UUID uuid : map.keySet()) {
      Map<ReportType, Report> reports = map.get(uuid);
      for(ReportType reportType: reports.keySet()) {
        Report report = reports.get(reportType);
        String report_token = report.getToken();

        if(report_token.compareTo(token) == 0) {
          return report;
        }
      }
    }
    return null;
  }

  @Override
  public InputStream GetContent(Report report) {
    return new ByteArrayInputStream("A,B,C,D,E\n1,2,3,4,5\n".getBytes());
  }

  @Override
  public List<ReportStatus> Status(List<String> tokens) {
    List<ReportStatus> statuses = new ArrayList<>();
    for(String token : tokens) {
      statuses.add(Status(token));
    }
    return statuses;
  }

  @Override
  public ReportStatus Status(String token) {
    if(GetReport(token) != null) {
      return ReportStatus.COMPLETE;
    } else {
      return ReportStatus.NOT_FOUND;
    }
  }

  @Override
  public List<Report> ListReports() {
    List<Report> all = new ArrayList<Report>();
    for(UUID uuid : map.keySet()) {
      Map<ReportType, Report> reports = map.get(uuid);
      for(ReportType reportType: reports.keySet()) {
        Report report = reports.get(reportType);
        all.add(report);
      }
    }
    return all;
  }

  @Override
  public List<Report> ListReports(UUID userid) {
    List<Report> user = new ArrayList<Report>();
    for(UUID uuid : map.keySet()) {
      Map<ReportType, Report> reports = map.get(uuid);
      for(ReportType reportType: reports.keySet()) {
        Report report = reports.get(reportType);
        if(report.getOwner().equals(userid)) {
          user.add(report);
        }
      }
    }
    return user;
  }

  @Override
  public boolean RemoveReport(String token) {
    for(UUID uuid : map.keySet()) {
      Map<ReportType, Report> reports = map.get(uuid);
      for(ReportType reportType: reports.keySet()) {
        Report report = reports.get(reportType);
        if(report.getToken().equals(token)) {
          reports.remove(reportType);
        }
      }
    }
    return true;
  }

  public List<ReportType> GetAvailableReports() {
    return Arrays.asList(new ReportType[] {
      ReportType.USER_STATUS,
      ReportType.GROUP_STATUS,
      ReportType.GRANT_STATUS,
      ReportType.PROGRAM_STATUS,
      ReportType.COMPUTE,
      ReportType.WINDOWS_COMPUTE
    });
  }

  private String getToken() throws IllegalStateException {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      String random = getRandomAlphaNumericString(50);
      md.update(random.getBytes());
      byte[] digest = md.digest();
      StringBuffer digestString = new StringBuffer();
      for(int index = 0; index < digest.length; index++) {
        String hex = Integer.toHexString(0xff & digest[index]);
        digestString.append(hex);
      }
      String token = digestString.substring(0);
      return token;
    }
    catch(NoSuchAlgorithmException nse) {
      throw new IllegalStateException(nse);
    }
  }

  private String getRandomAlphaNumericString(int size) {
    String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    String ret = "";
    int length = chars.length();
    for (int i = 0; i < size; i++) {
      ret += chars.split("")[(int) (Math.random() * (length - 1))];
    }
    return ret;
  }
}

