package io.ghap.reporting.service;

import io.ghap.logevents.EventItem;
import io.ghap.logevents.Types;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
public interface DataSubmissionService {
    List<EventItem> loadReportData(Types type, String accessToken, Date start, Date end) throws IOException;
}
