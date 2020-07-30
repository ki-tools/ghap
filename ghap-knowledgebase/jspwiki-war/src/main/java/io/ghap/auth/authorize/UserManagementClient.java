package io.ghap.auth.authorize;

import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.json.JSONConfiguration;
import io.ghap.auth.LdapPrincipal;
import io.ghap.auth.StreamUtils;
import io.ghap.auth.Utils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;


import javax.net.ssl.HostnameVerifier;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static io.ghap.auth.StreamUtils.toUtfString;
import static io.ghap.auth.Utils.getSSLContext;
import static io.ghap.auth.Utils.slashAtTheEnd;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

public class UserManagementClient {

    private Logger log = Logger.getLogger(this.getClass());

    private static UserManagementClient instance;

    private final DynamicStringProperty userManagementUrl;
    private final DynamicStringProperty userManagementLogin;
    private final DynamicStringProperty userManagementPassword;

    private WebResource roleResource;
    private WebResource groupResource;
    private WebResource userResource;
    private Client client;


    private UserManagementClient(){
        userManagementUrl   = DynamicPropertyFactory.getInstance().getStringProperty("user.management.url", "http://userservice.samba.ghap.io/");
        userManagementLogin    = DynamicPropertyFactory.getInstance().getStringProperty("user.management.admin.adminUserName", "GHAPAdministrator");
        userManagementPassword = DynamicPropertyFactory.getInstance().getStringProperty("user.management.admin.adminUserPassword", null);
        initClient();
    }

    public static UserManagementClient getInstance(){
        if(instance == null){
            synchronized (UserManagementClient.class){
                if(instance == null){
                    instance = new UserManagementClient();
                }

            }
        } else {
            instance.initClient();
        }
        return instance;
    }

    private void initClient() {
        if( userManagementUrl.get() != null && userManagementLogin.get() != null && userManagementPassword.get() != null) {
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
            client.addFilter(new HTTPBasicAuthFilter(userManagementLogin.get(), userManagementPassword.get()));

            //this.roleResource = client.resource(getUserManagementUrl() + "role/all/default");
            this.roleResource = client.resource(getUserManagementUrl() + "group/and/role");
            this.groupResource = client.resource(getUserManagementUrl() + "group/all/default");
            this.userResource = client.resource(getUserManagementUrl() + "user/all/default");
        }
    }
    /**
     * Return path to oauth service with slash char at the end
     * @return
     */
    public String getUserManagementUrl() {
        return slashAtTheEnd(userManagementUrl.get());
    }

    public Collection<GhapRole> getRoles(){

        long start = System.currentTimeMillis();

        Collection<GhapRole> roles = Collections.EMPTY_LIST;
        ClientResponse response = null;
        try {
            response = roleResource
                    .accept("application/json")
                    .get(ClientResponse.class);

            if (response.getStatus() == 410) {
                // token was expired
            } else if (response.getStatus() == 401 || response.getStatus() == 403) {
                log.error(String.format("Invalid user-management auth configuration(response: %s). Uri: \"%s\", user.management.admin.adminUserName: \"%s\"", response.getStatus(), roleResource.getURI(), userManagementLogin.get()));
                //500, "Invalid oauth configuration. See logs."
                throw new WebApplicationException(
                        Response.status(INTERNAL_SERVER_ERROR).entity("Invalid user-management auth configuration(response: " + response.getStatus() + "). See logs.").build()
                );
            } else if (response.getStatus() != 200) {
                log.error(String.format("Bad user-management service response %s. Uri: \"%s\", user.management.admin.adminUserName: \"%s\", response: $s", response.getStatus(), roleResource.getURI(), userManagementLogin.get(), toUtfString(response.getEntityInputStream())));
                //500, "Invalid oauth configuration. See logs."
                throw new WebApplicationException(
                        Response.status(INTERNAL_SERVER_ERROR).entity("Oauth response: " + response.getStatus()).build()
                );
            } else if (!Utils.equals(MediaType.APPLICATION_JSON_TYPE, response.getType())) {
                String content = toUtfString(response.getEntityInputStream());
                throw new WebApplicationException(
                        Response.status(INTERNAL_SERVER_ERROR).entity("Invalid Oauth response:\n" + content).build()
                );
            } else {
                roles = response.getEntity(new GenericType<List<GhapRole>>() {
                });
                //System.out.println(roles);
            }

            return roles;
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

    public Collection<LdapPrincipal> getUsers(){
        Collection<LdapPrincipal> users = Collections.EMPTY_LIST;
        return users;
    }

    public Collection<GhapGroup> getGroups(){
        Collection<GhapGroup> groups = Collections.EMPTY_LIST;
        return groups;
    }
}
