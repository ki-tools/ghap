package io.ghap.project.dao.impl;

import com.google.inject.Inject;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import io.ghap.project.dao.CommonPersistDao;
import io.ghap.project.dao.HealthCheckDao;
import io.ghap.project.dao.StashProjectDao;
import io.ghap.project.manager.DefaultProjectService;
import io.ghap.project.model.StashException;
import io.ghap.project.service.StashExceptionService;
import org.apache.commons.lang.time.StopWatch;

import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 */
public class DefaultHealthCheckDao implements HealthCheckDao {

    private static final String STATUS_FAILED = "Failed: ";

    @Inject
    private StashProjectDao stashProjectDao;

    @Inject
    private CommonPersistDao commonPersistDao;

    @Inject
    private StashExceptionService stashExceptionService;

    @Override
    public Map<String, String> checkHealth(UriInfo uriInfo) {
        Map<String, String> result = new HashMap<>();
        result.put("db", pingDb());
        result.put("ping stash", pingStash());
        result.put("check stash", checkStash());
        result.put("/project get", checkMethod("", "get", uriInfo));
        result.put("/project create", checkMethod("", "post", uriInfo));
        result.put("/grant update", checkMethod("/grant", "put", uriInfo));
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

    private String pingStash() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            stashProjectDao.ping();
        } catch (StashException e) {
            return stashExceptionService.toWebErrors(e).iterator().next().getMsg();
        } catch (Throwable e) {
            return STATUS_FAILED + e.getMessage();
        } finally {
            stopWatch.stop();
        }
        return stopWatch.toString();
    }

    private String checkStash() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            stashProjectDao.getProjects();
        } catch (StashException e) {
            return stashExceptionService.toWebErrors(e).iterator().next().getMsg();
        } catch (Throwable e) {
            return STATUS_FAILED + e.getMessage();
        } finally {
            stopWatch.stop();
        }
        return stopWatch.toString();
    }

    private String checkMethod(String name, String httpMethodName, UriInfo uriInfo) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        WebResource resource = create().resource(getUri(uriInfo).toASCIIString() + name);
        ClientResponse resp;
        try {
            if ("post".equalsIgnoreCase(httpMethodName)) {
                resp = resource.post(ClientResponse.class);
            } else if ("delete".equalsIgnoreCase(httpMethodName)) {
                resp = resource.delete(ClientResponse.class);
            } else if ("put".equalsIgnoreCase(httpMethodName)) {
                resp = resource.put(ClientResponse.class);
            } else {
                resp = resource.get(ClientResponse.class);
            }
            checkResponse(resp);
        } catch (Exception e) {
            return STATUS_FAILED + e.getMessage();
        } finally {
            stopWatch.stop();
        }
        return stopWatch.toString();
    }

    private void checkResponse(ClientResponse clientResponse) throws Exception {
        int status = clientResponse.getStatus();
        if (status >= 400 && status != 401 && status != 405) {
            throw new Exception("method returns " + status);
        }
    }

    private Client create() {
        Client client = Client.create(DefaultStashProjectDao.getConfig());
        return client;
    }

    public URI getUri(UriInfo uriInfo) {
        return uriInfo.getBaseUriBuilder().path(DefaultProjectService.class).build();
    }
}
