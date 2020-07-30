package io.ghap.project.dao.impl;

import com.netflix.governator.annotations.Configuration;
import com.sun.jersey.api.client.*;
import com.sun.jersey.api.client.filter.ClientFilter;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import io.ghap.project.dao.UserManagementDao;
import io.ghap.project.model.LdapGroup;
import io.ghap.project.model.LdapUser;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 */
public class DefaultUserManagementDao implements UserManagementDao {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Configuration("user.management.url")
    private String url;

    @Configuration("user.management.admin.groups")
    private String[] adminGroups;

    @Configuration("user.management.consumer.key")
    private String consumerKey;

    @Configuration("user.management.consumer.secret")
    private String consumerSecret;

    @Override
    public Set<LdapUser> getUsersForGroup(String group, String accessToken) {
        if (StringUtils.isBlank(group)) {
            return Collections.emptySet();
        }
        Client client = create(accessToken);
        WebResource webResource = client.resource(url + "/group/members/").path(group);
        Set<LdapUser> users = webResource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
                .get(new GenericType<Set<LdapUser>>() {});
        return users;
    }

    @Override
    public Set<LdapUser> getUsersForGroup(String[] groups, String accessToken) {
        if (groups == null || groups.length == 0) {
            return Collections.emptySet();
        }
        Set<LdapUser> result = new HashSet<>();
        for (String group : groups) {
            result.addAll(getUsersForGroup(group, accessToken));
        }
        return result;
    }

    @Override
    public Set<LdapUser> getUsersForGroup(String accessToken) {
        return getUsersForGroup(adminGroups, accessToken);
    }

    @Override
    public Set<String> getEmails(Set<LdapUser> ldapUsers) {
        if (CollectionUtils.isEmpty(ldapUsers)) {
            return Collections.emptySet();
        }
        Set<String> result = new HashSet<>(ldapUsers.size());
        for (LdapUser user : ldapUsers) {
            if (StringUtils.isNotBlank(user.getEmail()) && user.getGuid() != null) {
                result.add(user.getEmail());
            }
        }
        return result;
    }

    @Override
    public LdapUser getUser(UUID uuid, String accessToken) {
        if (uuid == null) {
            return null;
        }
        Client client = create(accessToken);
        WebResource webResource = client.resource(url + "/user/or/group/").path(uuid.toString());
        ClientResponse clientResponse = webResource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
                .get(ClientResponse.class);
        if (clientResponse.getStatus() != HttpServletResponse.SC_OK) {
            log.error("getting error response. status code = {} for user guid {} and accessToken {}",
                    clientResponse.getStatus(), uuid, accessToken);

            Response response = Response.status(clientResponse.getStatus()).entity(webResource.getURI()).build();
            throw new WebApplicationException(response);
        }
        return clientResponse.getEntity(LdapUser.class);
    }

    @Override
    public Set<LdapUser> getAllUsers(String accessToken) {
        Client client = create(accessToken);
        WebResource webResource = client.resource(url + "/user/all/default");
        Set<LdapUser> users = webResource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
                .get(new GenericType<Set<LdapUser>>() {});
        return users;
    }

    @Override
    public Set<LdapGroup> getAllGroups(String accessToken) {
        Client client = create(accessToken);
        WebResource webResource = client.resource(url + "/group/all/default");
        Set<LdapGroup> groups = webResource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
                .get(new GenericType<Set<LdapGroup>>() {});
        return groups;
    }

    private Client create(String accessToken) {
        Client client = Client.create(DefaultStashProjectDao.getConfig());
        client.addFilter(new OAuthFilter(accessToken));
        return client;
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
