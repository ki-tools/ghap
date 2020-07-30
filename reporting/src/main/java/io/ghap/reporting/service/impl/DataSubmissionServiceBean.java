package io.ghap.reporting.service.impl;

import com.netflix.governator.annotations.Configuration;
import io.ghap.logevents.*;
import io.ghap.reporting.service.DataSubmissionService;
import io.searchbox.core.SearchResult;

import java.io.IOException;
import java.util.*;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
public class DataSubmissionServiceBean implements DataSubmissionService {

    private static final String SERVICE = "es";

    @Configuration("logevents.url")
    private String uri;
    private EventsClient client;

    @Override
    public List<EventItem> loadReportData(Types type, String accessToken, Date start, Date end) throws IOException {
        List<SearchResult.Hit<EventItem, Void>> read = getClient().read(type, start, end);
        List<EventItem> result = new ArrayList<>(read.size());
        for (SearchResult.Hit<EventItem, Void> element : read) {
            result.add(element.source);
        }
        return result;
    }

    private EventsClient getClient() {
        if (client != null) {
            return client;
        }
        client = createClient();
        return client;
    }

    private synchronized EventsClient createClient() {
        Map conf = new HashMap<>();
        conf.put("service", SERVICE);
        conf.put("uri", uri);
        return EventsClientFactory.build(DestinationType.ELASTICSEARCH, Indexes.GHAP, conf);
    }
}
