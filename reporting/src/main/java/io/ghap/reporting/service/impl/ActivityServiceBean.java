package io.ghap.reporting.service.impl;

import com.google.inject.Inject;
import com.netflix.governator.annotations.Configuration;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import io.ghap.reporting.data.Activity;
import io.ghap.reporting.service.ActivityService;
import io.ghap.reporting.service.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
public class ActivityServiceBean implements ActivityService {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Configuration("activity.service.url")
	private String url;

	@Inject
	private HttpService httpService;

	@Override
	public List<Activity> loadActivities(String accessToken) {
		Client client = httpService.create(accessToken);
		WebResource webResource = client.resource(url).path("/Activity/get");
		ClientResponse clientResponse = webResource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
				.get(ClientResponse.class);
		try {
			if (clientResponse.getStatus() != HttpServletResponse.SC_OK) {
				log.error("getting error response. status code = {} and accessToken {}",
						clientResponse.getStatus(), accessToken);

				Response response = Response.status(clientResponse.getStatus()).entity(webResource.getURI()).build();
				throw new WebApplicationException(response);
			}
			return clientResponse.getEntity(new GenericType<List<Activity>>() {});
		} finally {
			clientResponse.close();
		}
	}
}
