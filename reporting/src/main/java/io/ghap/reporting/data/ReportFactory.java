package io.ghap.reporting.data;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.UUID;

public interface ReportFactory {
  enum ReportStatus {RUNNING, COMPLETE, ERROR, NOT_FOUND}
  List<ReportType> GetAvailableReports();
  String CreateReport(String accessToken, UUID uuid, ReportType reportType) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException;
  String CreateReport(String accessToken, UUID uuid, ReportType reportType, List<Constraint> constraints) throws NoSuchMethodException, IOException, IllegalAccessException, InvocationTargetException;
  Report GetReport(String token);
  InputStream GetContent(Report report);
  ReportStatus Status(String token);
  List<ReportStatus> Status(List<String> tokens);
  List<Report> ListReports();
  List<Report> ListReports(UUID userid);

  /**
   * Remove report and all children reports
   * @param token - report identifier
   * @return true if report was deleted(if provided token is correct) and false if not
   */
  boolean RemoveReport(String token);
}
