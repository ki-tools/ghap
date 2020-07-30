package io.ghap.reporting.data;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple factory for taking a {@link ResourceType} and creating a {@link ResourceDescriptor}
 */
public class ReportDescriptorFactory {
  private static ReportDescriptorFactory INSTANCE;

  private ReportDescriptorFactory() { super(); }

  public static ReportDescriptorFactory get() {
    if(INSTANCE==null) {
      INSTANCE=new ReportDescriptorFactory();
    }
    return INSTANCE;
  }

  public ReportDescriptor[] getDescriptors(ReportType... reportTypes) {
    List<ReportDescriptor> descriptors = new ArrayList<ReportDescriptor>();
    for(ReportType reportType: reportTypes) {
      descriptors.add(new ReportDescriptor(reportType));
    }
    return descriptors.toArray(new ReportDescriptor[0]);
  }

  public ReportDescriptor[] getDescriptors(List<ReportType> reportTypes) {
    List<ReportDescriptor> descriptors = new ArrayList<ReportDescriptor>();
    for(ReportType reportType: reportTypes) {
      descriptors.add(new ReportDescriptor(reportType));
    }
    return descriptors.toArray(new ReportDescriptor[0]);
  }
}
