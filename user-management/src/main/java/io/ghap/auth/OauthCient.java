package io.ghap.auth;

import com.netflix.governator.annotations.Configuration;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.json.JSONConfiguration;
import io.ghap.util.StreamUtils;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.template.exception.LdapRuntimeException;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Map;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

@Singleton
public class OauthCient {
    private static final String BEARER_PREFIX = "Bearer ";

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Configuration("oauth.admin.url") String oauthPath;
    @Configuration("oauth.admin.username") String oauthKey;
    @Configuration("oauth.admin.password") String oauthSecret;

    private WebResource webResource;
    private WebResource healthCheck;
    private Client client;

    @PostConstruct
    void initClient(){
        if( oauthKey != null && oauthSecret != null && oauthPath != null) {
            ClientConfig clientConfig = new DefaultClientConfig();

            clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);

            try {
                clientConfig.getProperties().put(com.sun.jersey.client.urlconnection.HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new com.sun.jersey.client.urlconnection.HTTPSProperties(new HostnameVerifier() {

                    @Override
                    public boolean verify(String hostname, javax.net.ssl.SSLSession sslSession) {
                        return true;
                    }
                }, getSSLContext()));
            } catch (Exception e) {
                e.printStackTrace();
            }

            client = Client.create(clientConfig);
            client.addFilter(new HTTPBasicAuthFilter(oauthKey, oauthSecret));

            this.webResource = client.resource(oauthPath + "/tokeninfo");
            this.healthCheck = client.resource(oauthPath + "/health");
        }
    }

    /*
    public static void main(String[] args){
        OauthCient oauthCient = new OauthCient();
        oauthCient.oauthPath = "https://oauth.ghap.io/oauth2/tokeninfo";
        oauthCient.oauthKey = "9ef9451e-fb9e-4f72-96c0-139d1a68e5d3";
        oauthCient.oauthSecret = "93b7dd00-8c70-4c91-acc3-3054c3e6a1df";
        oauthCient.initClient();
        LdapPrincipal principal = oauthCient.request("c4b3f0f9-2a94-4aef-8fbc-e81f4574ba4c");
        System.out.printf(principal.toString());
    }
    */

    public LdapPrincipal request(String accessToken){
        if(webResource == null){
            return null;
        }

        if(accessToken.startsWith(BEARER_PREFIX))
            accessToken = accessToken.substring(BEARER_PREFIX.length());

        long start = System.currentTimeMillis();

        ClientResponse response = null;
        try {
            response = webResource
                    .queryParam("access_token", accessToken)
                    .accept("application/json")
                    .get(ClientResponse.class);

            LdapPrincipal principal = null;

            if(response.getStatus() == 410) {
                // token was expired
            }
            else if(response.getStatus() == 404) {
                // token are not exists
                log.warn("Wrong token was provided for OAuth ({}). Token: \"{}\". {}", webResource.getURI(), accessToken, StreamUtils.toUtfString(response.getEntityInputStream()));
            } else if (response.getStatus() == 401) {
                log.error("Invalid OAuth configuration. Uri: \"{}\", oauth.admin.username: \"{}\", token: \"{}\"", webResource.getURI(), oauthKey, accessToken);
                //500, "Invalid oauth configuration. See logs."
                throw new WebApplicationException(
                        Response.status(INTERNAL_SERVER_ERROR).entity("Invalid oauth configuration. See logs.").build()
                );
            } else if (response.getStatus() != 200) {
                log.error("Bad OAuth response {}. Uri: \"{}\", oauth.admin.username: \"{}\", token: \"{}\"", response.getStatus(), webResource.getURI(), oauthKey, accessToken);
                //500, "Invalid oauth configuration. See logs."
                throw new WebApplicationException(
                        Response.status(INTERNAL_SERVER_ERROR).entity("Oauth response: " + response.getStatus()).build()
                );
            }
            else {
                Map<String, ?> data = response.getEntity(OauthResponse.class).getPrincipal();
                principal = new LdapPrincipal( (String)data.get("dn"), (String)data.get("password") );
                principal.setDn(new Dn(principal.getName()));
                principal.setAccessToken(accessToken);
            }

            return principal;
        }
        catch (IOException e){
            throw new RuntimeException(e);
        } catch (LdapInvalidDnException e) {
            throw new LdapRuntimeException(e);
        } finally {
            log.info("Receiving access-token related data time: " + (System.currentTimeMillis()-start));
            try {
                response.close();
            } catch (Exception e){/* close quietly */}
        }
    }

    public void requestHealth(){
        if(healthCheck == null){
            throw new RuntimeException("Health check resource is undefined for \"" + oauthPath + "\"");
        }

        long start = System.currentTimeMillis();

        ClientResponse response = null;
        try {
            response = healthCheck
                    .accept("application/json")
                    .get(ClientResponse.class);

            if(response.getStatus() != 200) {
                throw new RuntimeException(response.toString());
            }
        } finally {
            log.info("Receiving health check related data time: " + (System.currentTimeMillis()-start));
            try {
                response.close();
            } catch (Exception e){/* close quietly */}
        }
    }

    public void update(String accessToken, String dn, String password) {
        if(client == null){
            return;
        }
        String path = oauthPath + "/tokeninfo/update";
        WebResource resource = client.resource(path);
        resource =  resource
                .queryParam("access_token", accessToken)
                .queryParam("dn", dn);

        if(password != null){
            resource = resource.queryParam("password", password);
        }

        ClientResponse response = resource.accept("application/json").get(ClientResponse.class);

        log.info("Oauth("+path+") was requested to update \"access_token="+ accessToken +"\" with \"dn="+dn+"\"" +
                (password == null ? "":" and \"password="+password+"\"") +
                ". Response: " + response.getStatus());

        if (response.getStatus() != 200) {
            throw new WebApplicationException(response.getStatus());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class OauthResponse {
        private Map<String, ?> principal;

        public Map<String, ?> getPrincipal() {
            return principal;
        }

        public void setPrincipal(Map<String, ?> principal) {
            this.principal = principal;
        }
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
