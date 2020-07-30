package io.ghap.provision.health;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.Map;

/**
 */
@Path("health")
@Singleton
public class HealthCheckResource {

    @Inject
    private HealthCheckService healthCheckService;

    @GET
    @Produces("application/json")
    public Response check() {
        Map<String, String> data = healthCheckService.checkHealth();
        Response.ResponseBuilder status;
        if (!healthCheckService.isCheckSuccess(data)) {
            status = Response.status(Response.Status.SERVICE_UNAVAILABLE);
        } else {
            status = Response.status(Response.Status.OK);
        }
        return status.entity(data).build();
    }
}
