package io.ghap.reporting.service;

import io.ghap.reporting.data.CPUReportItem;
import io.ghap.reporting.data.DurationReportItem;
import io.ghap.reporting.data.UsageReportItem;

import java.util.Date;
import java.util.List;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
public interface VPGService {
	List<DurationReportItem> loadDurationData(String accessToken, Date start, Date end);

	List<UsageReportItem> loadUsageData(String accessToken, Date start, Date end, String instanceType);

	List<CPUReportItem> loadCPUData(String accessToken, Date start, Date end);
}
