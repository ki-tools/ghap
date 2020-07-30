package io.ghap.reporting.service.impl;

import com.google.inject.Inject;
import com.netflix.governator.annotations.Configuration;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import io.ghap.reporting.data.Group;
import io.ghap.reporting.data.User;
import io.ghap.reporting.service.HttpService;
import io.ghap.reporting.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
public class UserServiceBean implements UserService {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Configuration("user.management.url")
	private String url;

	@Inject
	private HttpService httpService;

	@Override
	public List<User> loadUsers(String accessToken) {
		Client client = httpService.create(accessToken);
		WebResource webResource = client.resource(url).path("/user/all/default");
		ClientResponse clientResponse = webResource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
				.get(ClientResponse.class);
		try {
			if (clientResponse.getStatus() != HttpServletResponse.SC_OK) {
				log.error("getting error response. status code = {} and accessToken {}",
						clientResponse.getStatus(), accessToken);

				Response response = Response.status(clientResponse.getStatus()).entity(webResource.getURI()).build();
				throw new WebApplicationException(response);
			}
			return clientResponse.getEntity(new GenericType<List<User>>() {});
		} finally {
			clientResponse.close();
		}
	}

	@Override
	public List<GroupWithUsers> loadGroupsWithUsers(String accessToken) {
		return __loadGroupsWithUsers(accessToken, "/group");
	}

	@Override
	public List<GroupWithUsers> loadRolesWithUsers(String accessToken) {
		return __loadGroupsWithUsers(accessToken, "/role");
	}

	private List<GroupWithUsers> __loadGroupsWithUsers(String accessToken, String prefix) {
		Client client = httpService.create(accessToken);
		WebResource webResource = client.resource(url).path(prefix + "/all/default");
		ClientResponse clientResponse = webResource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
				.get(ClientResponse.class);
		List<Group> groups;
		try {
			if (clientResponse.getStatus() != HttpServletResponse.SC_OK) {
				log.error("getting error response. status code = {} and accessToken {}",
						clientResponse.getStatus(), accessToken);

				Response response = Response.status(clientResponse.getStatus()).entity(webResource.getURI()).build();
				throw new WebApplicationException(response);
			}
			groups = clientResponse.getEntity(new GenericType<List<Group>>() {});
		} finally {
			clientResponse.close();
		}

		List<GroupWithUsers> result = new ArrayList<>(groups.size());
		for (Group group : groups) {
			webResource = client.resource(url).path(prefix + "/members/" + group.getDn());
			clientResponse = webResource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
					.get(ClientResponse.class);
			if (clientResponse.getStatus() != HttpServletResponse.SC_OK) {
				log.error("getting error response. status code = {} and accessToken {}",
						clientResponse.getStatus(), accessToken);

				Response response = Response.status(clientResponse.getStatus()).entity(webResource.getURI()).build();
				throw new WebApplicationException(response);
			}
			List<User> users = clientResponse.getEntity(new GenericType<List<User>>() {});
			result.add(new GroupWithUsers(group, users));
		}
		return result;
	}

	public static class GroupWithUsers {
		private Group group;
		private List<User> users;

		public GroupWithUsers(Group group, List<User> users) {
			this.group = group;
			this.users = users;
		}

		public Group getGroup() {
			return group;
		}

		public void setGroup(Group group) {
			this.group = group;
		}

		public List<User> getUsers() {
			return users;
		}

		public void setUsers(List<User> users) {
			this.users = users;
		}
	}
}
