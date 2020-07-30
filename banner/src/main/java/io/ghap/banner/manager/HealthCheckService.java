package io.ghap.banner.manager;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 */
public interface HealthCheckService {
	@GET
	@Produces("application/json")
	Response check();
}
