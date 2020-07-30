package io.ghap.auth;

import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.json.JSONConfiguration;
import org.apache.log4j.Logger;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import javax.annotation.PostConstruct;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

import static io.ghap.auth.Utils.getSSLContext;
import static io.ghap.auth.Utils.slashAtTheEnd;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

public class OauthClient {
    private static final String BEARER_PREFIX = "Bearer ";
    private static OauthClient instance;

    private Logger log = Logger.getLogger(this.getClass());

    private final DynamicStringProperty oauthPath;
    private final DynamicStringProperty oauthKey;
    private final DynamicStringProperty oauthSecret;

    private WebResource webResource;
    private WebResource healthCheck;
    private Client client;

    //Singleton
    private OauthClient(){
        oauthPath   = DynamicPropertyFactory.getInstance().getStringProperty("oauth.admin.url", "http://oauth.samba.ghap.io/");
        oauthKey    = DynamicPropertyFactory.getInstance().getStringProperty("oauth.admin.username", "projectservice");
        oauthSecret = DynamicPropertyFactory.getInstance().getStringProperty("oauth.admin.password", "");
        initClient();
    }

    public static OauthClient getInstance(){
        if(instance == null){
            synchronized (OauthClient.class){
                if(instance == null){
                    instance = new OauthClient();
                }

            }
        } else {
            instance.initClient();
        }
        return instance;
    }

    /**
     * Return path to oauth service with slash char at the end
     * @return
     */
    public String getOauthPath() {
        return slashAtTheEnd(oauthPath.get());
    }

    @PostConstruct
    private void initClient(){
        if( oauthKey.get() != null && oauthSecret.get() != null && oauthPath.get() != null) {
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
            client.addFilter(new HTTPBasicAuthFilter(oauthKey.get(), oauthSecret.get()));

            this.webResource = client.resource(getOauthPath() + "tokeninfo");
            this.healthCheck = client.resource(getOauthPath() + "health");
        }
    }

    /*
    public static void main(String[] args){
        OauthCient oauthCient = new OauthCient();
        oauthCient.oauthPath = "https://oauth.ghap.io/oauth2/tokeninfo";
        oauthCient.oauthKey = "";
        oauthCient.oauthSecret = "";
        oauthCient.initClient();
        LdapPrincipal principal = oauthCient.request("");
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
                log.warn(String.format("Wrong token was provided for OAuth (%s). Token: \"%s\". %s", webResource.getURI(), accessToken, StreamUtils.toUtfString(response.getEntityInputStream())));
            } else if (response.getStatus() == 401) {
                log.error(String.format("Invalid OAuth configuration. Uri: \"%s\", oauth.admin.username: \"%s\", token: \"%s\"", webResource.getURI(), oauthKey, accessToken));
                //500, "Invalid oauth configuration. See logs."
                throw new WebApplicationException(
                        Response.status(INTERNAL_SERVER_ERROR).entity("Invalid oauth configuration. See logs.").build()
                );
            } else if (response.getStatus() != 200) {
                log.error(String.format("Bad OAuth response %s. Uri: \"%s\", oauth.admin.username: \"%s\", token: \"%s\"", response.getStatus(), webResource.getURI(), oauthKey, accessToken));
                //500, "Invalid oauth configuration. See logs."
                throw new WebApplicationException(
                        Response.status(INTERNAL_SERVER_ERROR).entity("Oauth response: " + response.getStatus()).build()
                );
            }
            else if( !Utils.equals(MediaType.APPLICATION_JSON_TYPE, response.getType())) {
                String content = StreamUtils.toUtfString(response.getEntityInputStream());
                throw new WebApplicationException(
                        Response.status(INTERNAL_SERVER_ERROR).entity("Invalid Oauth response:\n" + content).build()
                );
            }
            else {
                Map<String, ?> data = response.getEntity(OauthResponse.class).getPrincipal();
                principal = new LdapPrincipal( (String)data.get("name"), (String)data.get("password") );
                principal.setDn( (String)data.get("dn") );
                principal.setGroups((List<String>) data.get("groups"));
                principal.setRoles((List<String>) data.get("roles"));
                principal.setAdminPrincipal((Boolean) data.get("adminPrincipal"));
                principal.setFirstTimeLogon((Boolean) data.get("firstTimeLogon"));
                principal.setEmail((String) data.get("email"));
                principal.setAccessToken(accessToken);
            }

            return principal;
        }
        catch (IOException e){
            throw new RuntimeException(e);
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
                (password == null ? "":" and \"password=""\"") +
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

}
