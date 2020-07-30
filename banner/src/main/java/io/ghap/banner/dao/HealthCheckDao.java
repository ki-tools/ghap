package io.ghap.banner.dao;

import javax.ws.rs.core.UriInfo;
import java.util.Map;

/**
 * Created by maksim on 18.10.15.
 */
public interface HealthCheckDao {
	Map<String, String> checkHealth(UriInfo uriInfo);

	boolean isCheckSuccess(Map<String, String> result);
}
