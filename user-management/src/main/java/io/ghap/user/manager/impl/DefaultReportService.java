package io.ghap.user.manager.impl;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;
import com.netflix.governator.annotations.Configuration;
import com.sun.jersey.api.NotFoundException;
import io.ghap.auth.LdapConfiguration;
import io.ghap.auth.OauthCient;
import io.ghap.ldap.CollectionUtils;
import io.ghap.ldap.LdapUtils;
import io.ghap.mailer.NewUserMailer;
import io.ghap.mailer.PasswordResetMailer;
import io.ghap.mailer.PasswordUpdatedMailer;
import io.ghap.mailer.UpdateUserMailer;
import io.ghap.user.dao.DomainDao;
import io.ghap.user.dao.GroupDao;
import io.ghap.user.dao.UserDao;
import io.ghap.user.model.AbstractModel;
import io.ghap.user.model.Domain;
import io.ghap.user.model.Group;
import io.ghap.user.model.User;
import io.ghap.user.model.validation.OnResetPassword;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.nullToEmpty;
import static io.ghap.user.manager.impl.PaginateAnything.paginate;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.OK;

@Path("report")
@Singleton
public class DefaultReportService {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Inject
    LdapConfiguration ldapConfiguration;

    @Inject
    UserDao userDao;
    @Inject
    GroupDao groupDao;
    @Inject
    DomainDao domainDao;
    @Inject
    NewUserMailer newUserMailer;
    @Inject
    PasswordUpdatedMailer passwordUpdatedMailer;
    @Inject
    PasswordResetMailer passwordResetMailer;
    @Inject
    UpdateUserMailer updateUserMailer;
    @Inject
    OnResetPassword onResetPassword;

    @Inject
    OauthCient oauthCient;


    //@Inject
    //Provider<Dependancy> dependencyProvider;
    @Context
    UriInfo uriInfo;
    @Context
    HttpServletResponse servletResponse;
    private Dn roleDn;
    private Dn groupDn;

    @GET
    @Path("users")
    @Produces(APPLICATION_JSON)
    //http://stackoverflow.com/questions/13750010/jersey-client-how-to-add-a-list-as-query-parameter
    public Response users(@HeaderParam("Range") String range, @QueryParam("guid") final List<String> guids) throws LdapException {

        StringBuilder filter = new StringBuilder();
        if(guids.size() > 0){
            filter.append("(&(objectClass=user)(|");
            for(String guid:guids){
                filter.append("(objectGUID=").append(guid).append(")");
            }
            filter.append("))");
        } else {
            filter.append("(&(objectClass=user)(!(isCriticalSystemObject=*)))");
        }

        List<User> users = userDao.findAll(null, false, filter.toString());
        users = paginate(users, servletResponse, range);
        //http://stackoverflow.com/questions/507602/how-can-i-initialize-a-static-map
        return Response.status(OK).entity(users.stream()
                .map(user -> ImmutableMap.<String, String>builder()
                                .put("userid", user.getGuid())
                                .put("dn", user.getDn())
                                .put("name", user.getName())
                                .put("firstname", nullToEmpty(user.getFirstName()))
                                .put("lastname", nullToEmpty(user.getLastName()))
                                .put("email", nullToEmpty(user.getEmail()))
                                .put("account_status", user.isDisabled() ? "disabled" : "enabled")
                                .build()
                )
                .collect(Collectors.toList()))
                .build();
    }

    @GET
    @Path("groups")
    @Produces(APPLICATION_JSON)
    private Response groups() throws LdapException {
        return groups(GroupType.GROUP);
    }

    @GET
    @Path("roles")
    @Produces(APPLICATION_JSON)
    private Response roles() throws LdapException {
        return groups(GroupType.ROLE);
    }


    private Response groups(final GroupType groupType) throws LdapException {
        List<Map> report = new ArrayList<>();

        Dn groupsDn = getDefaultParent(groupType);
        List<Group> groups = groupDao.findAll(groupsDn.toString(), true);
        for(Group group:groups){
            List<AbstractModel> members = groupDao.getMembers(group.getDn(), false);

            if(members != null) for (Iterator<AbstractModel> iter = members.iterator(); iter.hasNext();){
                AbstractModel model = iter.next();
                if( model instanceof User ){
                    User user = (User)model;
                    report.add(
                            ImmutableMap.of(
                                    "userid", user.getGuid(),
                                    "dn", user.getDn(),
                                    "groupid", group.getGuid(),
                                    "group", nullToEmpty(group.getName())
                                    )
                    );
                }
            }
        }

        //http://stackoverflow.com/questions/507602/how-can-i-initialize-a-static-map
        return Response.status(OK).entity(report).build();
    }

    private Dn getDefaultParent(final GroupType groupType) throws LdapException {

        if(roleDn == null) {
            roleDn = LdapUtils.toDn(ldapConfiguration.getLdapRealm()).add("ou=roles");
            Group group = groupDao.find(roleDn.toString());
            if(group == null){
                roleDn = LdapUtils.toDn(ldapConfiguration.getLdapRealm()).add("cn=roles");
            }
        }
        if(groupDn == null) {
            groupDn = LdapUtils.toDn(ldapConfiguration.getLdapRealm()).add("cn=users");
        }

        return (groupType == GroupType.GROUP) ? groupDn:roleDn;
    }
}
