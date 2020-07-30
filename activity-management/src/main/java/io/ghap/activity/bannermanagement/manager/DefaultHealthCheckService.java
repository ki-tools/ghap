package io.ghap.activity.bannermanagement.manager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.ghap.activity.bannermanagement.dao.HealthCheckDao;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Map;

/**
 */
@Path("health")
@Singleton
public class DefaultHealthCheckService implements HealthCheckService {

	@Inject
	private HealthCheckDao healthCheckDao;

	@Context
	private UriInfo uriInfo;

	@Override
	@GET
	@Produces("application/json")
	public Response check() {
		Map<String, String> data = healthCheckDao.checkHealth(uriInfo);
		Response.ResponseBuilder status;
		if (!healthCheckDao.isCheckSuccess(data)) {
			status = Response.status(Response.Status.SERVICE_UNAVAILABLE);
		} else {
			status = Response.status(Response.Status.OK);
		}
		return status.entity(data).build();
	}
}
