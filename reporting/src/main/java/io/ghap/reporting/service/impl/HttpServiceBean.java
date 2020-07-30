package io.ghap.reporting.service.impl;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.filter.ClientFilter;
import io.ghap.reporting.service.HttpService;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.core.HttpHeaders;
import java.security.cert.X509Certificate;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
public class HttpServiceBean implements HttpService {

    @Override
    public Client create(String accessToken) {
        Client client = Client.create(getConfig());
        client.addFilter(new OAuthFilter(accessToken));
        return client;
    }

    public static com.sun.jersey.api.client.config.ClientConfig getConfig() {
        com.sun.jersey.api.client.config.ClientConfig config = new com.sun.jersey.api.client.config.DefaultClientConfig(); // SSL configuration
        try {
            config.getProperties().put(com.sun.jersey.client.urlconnection.HTTPSProperties.PROPERTY_HTTPS_PROPERTIES,
                    new com.sun.jersey.client.urlconnection.HTTPSProperties(new HostnameVerifier() {

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

    public class OAuthFilter extends ClientFilter {

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
