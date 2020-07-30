package io.ghap.service.bean;

import com.netflix.governator.annotations.Configuration;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.client.filter.LoggingFilter;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import io.ghap.model.TokenResponse;
import io.ghap.service.OauthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.Collections;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
public class OauthServiceBean implements OauthService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Configuration("oauth.admin.username")
    private String login;
    @Configuration("oauth.admin.password")
    private String password;
    @Configuration("oauth.admin.url")
    private String url;

    @Override
    public String buildLoginUrl(String currentUrl) {
        StringBuilder sb = new StringBuilder(url);
        if (!url.endsWith("/")) {
            sb.append("/");
        }
        sb.append("authorize?client_id=").append(login).append("&response_type=code&scope=read,write&redirect_uri=").append(currentUrl);
        return sb.toString();
    }

    @Override
    public TokenResponse exchangeToken(String code, String redirectUri) {
        WebResource resource = create().resource(url + "/token");
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        params.put("grant_type", Collections.singletonList("authorization_code"));
        params.put("code", Collections.singletonList(code));
        params.put("redirect_uri", Collections.singletonList(redirectUri));
        ClientResponse clientResponse = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, params);
        if (clientResponse.getStatus() != HttpServletResponse.SC_OK) {
            log.error("getting error response. status code = {} for code {}",
                    clientResponse.getStatus(), code);

            Response response = Response.status(clientResponse.getStatus()).entity(resource.getURI()).build();
            throw new WebApplicationException(response);
        }
        return clientResponse.getEntity(TokenResponse.class);
    }

    private Client create() {
        Client client = Client.create(PrepareRulesServiceBean.getConfig());
        client.addFilter(new HTTPBasicAuthFilter(login, password));
        client.addFilter(new LoggingFilter());
        return client;
    }
}
