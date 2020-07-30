package io.ghap.data.logevents;

import com.netflix.governator.annotations.Configuration;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;

import static io.ghap.data.logevents.Utils.slashAtTheEnd;
import static io.ghap.data.logevents.Utils.toUtfString;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

@Singleton
public class ProjectProvisioningClient {
    private Logger log = LoggerFactory.getLogger(this.getClass());

    //'http://projectservice.samba.ghap.io/rest/v1'
    @Configuration("project.service.url") String projectServiceUrl;

    private Client client;

    @PostConstruct
    void initClient(){
        if( projectServiceUrl != null) {
            ClientConfig clientConfig = new DefaultClientConfig();

            clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
            clientConfig.getProperties().put(com.sun.jersey.client.urlconnection.HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new com.sun.jersey.client.urlconnection.HTTPSProperties(new HostnameVerifier() {

                @Override
                public boolean verify(String hostname, javax.net.ssl.SSLSession sslSession) {
                    return true;
                }
            }, getSSLContext()));


            client = Client.create(clientConfig);
            client.setConnectTimeout(3*60*1000);
            client.setReadTimeout(3*60*1000);
        }
    }

    public Boolean exists(String fileName, String authorization) throws IOException {
        WebResource webResource = getWebResource(fileName);

        ClientResponse response = webResource
                    .accept("application/json")
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .get(ClientResponse.class);

        Boolean result = null;
        switch( response.getStatus() ){
            case 410:
                // token was expired
                break;
            case 401:
            case 403:
                log.error(String.format("Invalid project-provisioning auth configuration(response: %s). Authorization: %s. Uri: \"%s\"", response.getStatus(), authorization, projectServiceUrl));
                throw new WebApplicationException(
                        Response.status(INTERNAL_SERVER_ERROR).entity("Invalid  project-provisioning auth configuration(response: " + response.getStatus() + "). See logs.").build()
                );
            case 404:
                result = false;
                break;
            case 200:
                if (!Utils.equals(MediaType.APPLICATION_JSON_TYPE, response.getType())) {
                    String content = toUtfString(response.getEntityInputStream());
                    throw new WebApplicationException(
                            Response.status(INTERNAL_SERVER_ERROR).entity("Invalid Oauth response:\n" + content).build()
                    );
                }
                result = true;
                break;
            default:
                log.error(String.format("Bad  project-provisioning service response %s. Uri: \"%s\", response: $s", response.getStatus(), projectServiceUrl, toUtfString(response.getEntityInputStream())));
                //500, "Invalid oauth configuration. See logs."
                throw new WebApplicationException(
                        Response.status(INTERNAL_SERVER_ERROR).entity("Oauth response: " + response.getStatus()).build()
                );
        }

        return result;

    }

    private WebResource getWebResource(String fileName) {
        String encodedFileName = null;
        try {
            encodedFileName = URLEncoder.encode(fileName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            encodedFileName = Utils.encodeURIComponent(fileName);
        }
        return client.resource(slashAtTheEnd(projectServiceUrl) + "directStash/fileExists/" + encodedFileName);
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
