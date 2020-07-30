package io.ghap.banner.dao.impl;

import com.google.inject.Inject;
import io.ghap.banner.dao.CommonPersistDao;
import io.ghap.banner.dao.HealthCheckDao;
import org.apache.commons.lang.time.StopWatch;

import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 */
public class DefaultHealthCheckDao implements HealthCheckDao {

    private static final String STATUS_FAILED = "Failed: ";



    @Inject
    private CommonPersistDao commonPersistDao;



    @Override
    public Map<String, String> checkHealth(UriInfo uriInfo) {
        Map<String, String> result = new HashMap<>();
        result.put("db", pingDb());
        return result;
    }

    @Override
    public boolean isCheckSuccess(Map<String, String> result) {
        Collection<String> values = new ArrayList<>(result.values());
        for (String val : values) {
            if (val.startsWith(STATUS_FAILED)) {
                return false;
            }
        }
        return true;
    }

    private String pingDb() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            commonPersistDao.executePingQuery();
        } catch (Throwable e) {
            return STATUS_FAILED + e.getMessage();
        } finally {
            stopWatch.stop();
        }
        return stopWatch.toString();
    }
}
