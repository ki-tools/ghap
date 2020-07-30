package io.ghap.service.bean;

import com.netflix.governator.annotations.Configuration;
import com.sun.jersey.api.client.*;
import com.sun.jersey.api.client.filter.ClientFilter;
import io.ghap.model.DefaultResponse;
import io.ghap.service.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
public class ProjectServiceBean implements ProjectService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Configuration("project.service.url")
    private String projectServiceUrl;

    @Override
    public boolean isUserHasGrantPermission(String accessToken, String project, String grant) {
        Client client = create(accessToken);
        WebResource webResource = client.resource(projectServiceUrl + "/directStash/userPermissions/" + urlEncode(project) + "/" + urlEncode(grant));
        ClientResponse clientResponse = webResource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
                .get(ClientResponse.class);
        if (clientResponse.getStatus() != HttpServletResponse.SC_OK) {
            log.error("getting error response. status code = {} for project {}, grant {} and accessToken {}",
                    clientResponse.getStatus(), project, grant, accessToken);
            if (clientResponse.getStatus() == HttpServletResponse.SC_NOT_FOUND) {
                //Grant is not exists.
                return true;
            }
            Response response = Response.status(clientResponse.getStatus()).entity(webResource.getURI()).build();
            throw new WebApplicationException(response);
        }
        DefaultResponse entity = clientResponse.getEntity(DefaultResponse.class);
        return entity.isSuccess();
    }

    @Override
    public boolean isUserHasProjectPermission(String accessToken, String project) {
        Client client = create(accessToken);
        WebResource webResource = client.resource(projectServiceUrl + "/directStash/userPermissions/" + urlEncode(project));
        ClientResponse clientResponse = webResource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
                .get(ClientResponse.class);
        if (clientResponse.getStatus() != HttpServletResponse.SC_OK) {
            log.error("getting error response. status code = {} for project {} and accessToken {}",
                    clientResponse.getStatus(), project, accessToken);

            if (clientResponse.getStatus() == HttpServletResponse.SC_NOT_FOUND) {
                //Project is not exists.
                return true;
            }
            Response response = Response.status(clientResponse.getStatus()).entity(webResource.getURI()).build();
            throw new WebApplicationException(response);
        }
        DefaultResponse entity = clientResponse.getEntity(DefaultResponse.class);
        return entity.isSuccess();

    }

    private Client create(String accessToken) {
        Client client = Client.create(PrepareRulesServiceBean.getConfig());
        client.addFilter(new OAuthFilter(accessToken));
        return client;
    }

    private String urlEncode(String source) {
        try {
            return URLEncoder.encode(source, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private class OAuthFilter extends ClientFilter {

        private String token;

        public OAuthFilter(String token) {
            this.token = token;
        }

        @Override
        public ClientResponse handle(ClientRequest cr) throws ClientHandlerException {
            if (!cr.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                cr.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            }
            return getNext().handle(cr);
        }
    }
}
