package io.ghap.reporting.service.impl;

import com.google.inject.Inject;
import com.netflix.governator.annotations.Configuration;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import io.ghap.reporting.data.CPUReportItem;
import io.ghap.reporting.data.DurationReportItem;
import io.ghap.reporting.data.UsageReportItem;
import io.ghap.reporting.service.HttpService;
import io.ghap.reporting.service.VPGService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
public class VPGServiceBean implements VPGService {

	private final Logger log = LoggerFactory.getLogger(this.getClass());
	private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

	@Configuration("vpg.provisioning.service.url")
	private String url;

	@Inject
	private HttpService httpService;

	@Override
	public List<DurationReportItem> loadDurationData(String accessToken, Date start, Date end) {
		Client client = httpService.create(accessToken);
		WebResource webResource = client.resource(url).path("/report/duration");
		webResource = appendQueryParams(webResource, start, end, null);
		ClientResponse clientResponse = webResource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
				.get(ClientResponse.class);
		try {
			if (clientResponse.getStatus() != HttpServletResponse.SC_OK) {
				log.error("getting error response. status code = {} and accessToken {}",
						clientResponse.getStatus(), accessToken);

				Response response = Response.status(clientResponse.getStatus()).entity(webResource.getURI()).build();
				throw new WebApplicationException(response);
			}
			return clientResponse.getEntity(new GenericType<List<DurationReportItem>>() {});
		} finally {
			clientResponse.close();
		}
	}

	@Override
	public List<UsageReportItem> loadUsageData(String accessToken, Date start, Date end, String instanceType) {
		Client client = httpService.create(accessToken);
		WebResource webResource = client.resource(url).path("/report/usagereport");
		webResource = appendQueryParams(webResource, start, end, instanceType);
		ClientResponse clientResponse = webResource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
				.get(ClientResponse.class);
		try {
			if (clientResponse.getStatus() != HttpServletResponse.SC_OK) {
				log.error("getting error response. status code = {} and accessToken {}",
						clientResponse.getStatus(), accessToken);

				Response response = Response.status(clientResponse.getStatus()).entity(webResource.getURI()).build();
				throw new WebApplicationException(response);
			}
			return clientResponse.getEntity(new GenericType<List<UsageReportItem>>() {});
		} finally {
			clientResponse.close();
		}
	}

	@Override
	public List<CPUReportItem> loadCPUData(String accessToken, Date start, Date end) {
		Client client = httpService.create(accessToken);
		WebResource webResource = client.resource(url).path("/report/cpuusage");
		webResource = appendQueryParams(webResource, start, end, null);
		ClientResponse clientResponse = webResource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
				.get(ClientResponse.class);
		try {
			if (clientResponse.getStatus() != HttpServletResponse.SC_OK) {
				log.error("getting error response. status code = {} and accessToken {}",
						clientResponse.getStatus(), accessToken);

				Response response = Response.status(clientResponse.getStatus()).entity(webResource.getURI()).build();
				throw new WebApplicationException(response);
			}
			return clientResponse.getEntity(new GenericType<List<CPUReportItem>>() {});
		} finally {
			clientResponse.close();
		}
	}

	private WebResource appendQueryParams(WebResource webResource, Date start, Date end, String instanceType) {
		if (start != null) {
			webResource = webResource.queryParam("start", new SimpleDateFormat(DATE_FORMAT).format(start));
		}
		if (end != null) {
			webResource = webResource.queryParam("end", new SimpleDateFormat(DATE_FORMAT).format(end));
		}
		return webResource;
	}
}
