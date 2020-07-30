package io.ghap.user.manager.impl;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.inject.Singleton;
import io.ghap.auth.OauthCient;
import io.ghap.user.dao.DomainDao;
import io.ghap.user.manager.ServiceStatus;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.ObjectMapper;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import java.io.IOException;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("health")
@Singleton
public class HealthCheckService {

    public static final String STATUS_FAILED = "Failed";

    @Inject DomainDao domainDao;
    @Inject OauthCient oauthCient;

    @GET
    @Produces(APPLICATION_JSON)
    public ServiceStatus get() throws IOException {
        ServiceStatus status = new ServiceStatus();

        setLdapStatus(status);
        setOauthStatus(status);

        /*
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(status);
        */

        if (isFailed(status)){
            throw new WebApplicationException(Response.status(503).entity(status).build());
        }

        return status;
    }

    private void setLdapStatus(ServiceStatus status){
        try {
            long start = System.currentTimeMillis();
            domainDao.find();
            status.setLdap(System.currentTimeMillis() - start);
        } catch (Exception e) {
            status.setLdap(STATUS_FAILED + ". " + e);
        }
    }

    private void setOauthStatus(ServiceStatus status){
        try {
            long start = System.currentTimeMillis();
            oauthCient.requestHealth();
            status.setOauth(System.currentTimeMillis() - start);
        } catch (Exception e) {
            status.setOauth(STATUS_FAILED + ". " + e);
        }
    }

    private boolean isFailed(ServiceStatus status){
        return (String.valueOf(status.getLdap()).startsWith(STATUS_FAILED) ||
                String.valueOf(status.getOauth()).startsWith(STATUS_FAILED));
    }
}
