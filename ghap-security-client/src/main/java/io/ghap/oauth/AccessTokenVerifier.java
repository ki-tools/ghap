package io.ghap.oauth;

import com.github.hburgmeier.jerseyoauth2.api.token.IAccessTokenInfo;
import com.github.hburgmeier.jerseyoauth2.api.token.InvalidTokenException;
import com.github.hburgmeier.jerseyoauth2.rs.api.token.IAccessTokenVerifier;
import com.google.inject.Singleton;
import com.netflix.governator.annotations.Configuration;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import org.apache.commons.lang.time.StopWatch;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;
import java.security.cert.X509Certificate;
import java.util.Set;

/**
 */
@Singleton
@Provider
public class AccessTokenVerifier implements IAccessTokenVerifier {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    private static Client client;

    @Configuration("oauth.admin.username")
    private String login;
    @Configuration("oauth.admin.password")
    private String password;
    @Configuration("oauth.admin.url")
    private String url;

    @Override
    public IAccessTokenInfo verifyAccessToken(String accessToken) throws InvalidTokenException {
        return convertResponse(loadTokenInfo(accessToken), accessToken);
    }

    private IAccessTokenInfo convertResponse(VerifyTokenResponse verifyTokenResponse, String accessToken) {
        TokenPrincipal principal = verifyTokenResponse.getPrincipal();
        Set<String> roles = verifyTokenResponse.getScopes();
        return new OAuthAccessTokenInfo(new OAuthUser(principal.getName(), principal.getEmail(), roles, accessToken),
                "project-provisioning", roles);
    }

    private VerifyTokenResponse loadTokenInfo(String accessToken) throws InvalidTokenException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            Client client = getClient();
            WebResource webResource = client.resource(url).path("tokeninfo").queryParam("access_token", accessToken);
            ClientResponse response = webResource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON).get(ClientResponse.class);
            if (response.getStatus() != HttpServletResponse.SC_OK) {
                try {
                    response.close();
                } catch (ClientHandlerException e) {
                    //do nothing
                }
                final String pwd = (password == null) ? null:password.split("-")[0];
                log.info("Cannot get token info \""+accessToken+"\". URL: \"" + url + "\". Password: \"" + pwd + "\\-...\". HTTP response: " + response.getStatus());
                throw new InvalidTokenException();
            }
            VerifyTokenResponse result = response.getEntity(VerifyTokenResponse.class);
            return result;
        } finally {
            stopWatch.stop();
            System.out.println("loadTokenInfo method execution time " + stopWatch.toString());
        }
    }

    private Client getClient() {
        if (client != null) {
            return client;
        }
        client = create();
        return client;
    }

    private synchronized Client create() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Client client = Client.create(getConfig());
        client.addFilter(new HTTPBasicAuthFilter(login, password));
        stopWatch.stop();
        System.out.println("create client method execution time " + stopWatch.toString());
        return client;
    }

    public static com.sun.jersey.api.client.config.ClientConfig getConfig() {
        com.sun.jersey.api.client.config.ClientConfig config = new com.sun.jersey.api.client.config.DefaultClientConfig(); // SSL configuration
        try {
            config.getProperties().put(com.sun.jersey.client.urlconnection.HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new com.sun.jersey.client.urlconnection.HTTPSProperties(new HostnameVerifier() {

                @Override
                public boolean verify(String hostname, javax.net.ssl.SSLSession sslSession) {
                    return true;
                }
            }, getSSLContext()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        config.getClasses().add(JacksonJsonProvider.class);
        return config;
    }

    // disable cert verification
    private static SSLContext getSSLContext() {

        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }
        };
        SSLContext ctx = null;
        try {
            ctx = SSLContext.getInstance("SSL");
            ctx.init(null, trustAllCerts, new java.security.SecureRandom());
        } catch (java.security.GeneralSecurityException ex) {
        }
        return ctx;
    }
}
