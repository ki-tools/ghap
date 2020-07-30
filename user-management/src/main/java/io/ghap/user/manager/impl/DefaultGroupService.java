package io.ghap.user.manager.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sun.jersey.api.NotFoundException;
import io.ghap.auth.LdapConfiguration;
import io.ghap.ldap.CollectionUtils;
import io.ghap.ldap.LdapUtils;
import io.ghap.user.dao.GroupDao;
import io.ghap.user.form.GroupFormData;
import io.ghap.user.manager.GroupService;
import io.ghap.user.model.AbstractModel;
import io.ghap.user.model.Group;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.name.Dn;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static io.ghap.user.manager.impl.PaginateAnything.paginate;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("group")
@Singleton
public class DefaultGroupService implements GroupService {

    @Inject GroupDao groupDao;

    @Inject LdapConfiguration ldapConfiguration;

    @Context HttpServletResponse servletResponse;

    @Override
    @GET
    @Path("all/{parentDn}")
    @Produces(APPLICATION_JSON)
    public List<Group> findAll(@PathParam("parentDn") String parentDn, @HeaderParam("Range") String range) throws LdapException {
        Dn dn = getDefaultParent(parentDn);
        List<Group> groups = groupDao.findAll(dn.toString(), true);

        if(groups != null){
            groups = groups.stream()
                    .filter(group -> !"ghap.io".equals(group.getInfo()))
                    .collect(Collectors.toList());
        }

        //CollectionUtils.sortGroups(groups);
        return paginate(groups, servletResponse, range);
    }

    @Override
    @GET
    @Path("and/role")
    @Produces(APPLICATION_JSON)
    public List<Group> findAll(@HeaderParam("Range") String range) throws LdapException {
        String root = LdapUtils.toDn(ldapConfiguration.getLdapRealm()).toString();
        List<Group> groups = groupDao.findAll(root, true);

        if(groups != null){
            groups = groups.stream()
                    .filter(group -> !"ghap.io".equals(group.getInfo()))
                    .collect(Collectors.toList());
        }

        //CollectionUtils.sortGroups(groups);
        return paginate(groups, servletResponse, range);
    }

    protected Dn getDefaultParent(String parentDn) throws LdapException {
        return ("default".equalsIgnoreCase(parentDn)) ?
                LdapUtils.toDn(ldapConfiguration.getLdapRealm()).add("cn=users") : new Dn(parentDn);
    }

    @Override
    @GET
    @Path("{dn}")
    @Produces(APPLICATION_JSON)
    public Group get(@PathParam("dn") String dn) throws LdapException {
        Group group = groupDao.find(dn);
        if(group == null) {
            throw new NotFoundException("Cannot find group \""+dn+"\"");
        }
        return group;
    }

    @Override
    @GET
    @Path("members/{dn}")
    @Produces(APPLICATION_JSON)
    public List<AbstractModel> getMembers(@PathParam("dn") String dn) throws LdapException {
        List<AbstractModel> members = groupDao.getMembers(dn, false);
        if(members == null) {
            throw new NotFoundException("Cannot find group \""+dn+"\"");
        }
        CollectionUtils.sort(members);
        return members;
    }

    @Override
    @GET
    @Path("users/{dn}")
    @Produces(APPLICATION_JSON)
    public List<AbstractModel> getUsers(@PathParam("dn") String dn) throws LdapException {
        List<AbstractModel> members = groupDao.getMembers(dn, false);
        if(members == null) {
            throw new NotFoundException("Cannot find group \""+dn+"\"");
        }
        for(Iterator<AbstractModel> iter = members.iterator(); iter.hasNext();){
            AbstractModel model = iter.next();
            if( !"user".equals(model.getObjectClass()) ){
                iter.remove();
            }
        }
        CollectionUtils.sort(members);
        return members;
    }

    @Override
    @GET
    @Path("roles/{dn}")
    @Produces(APPLICATION_JSON)
    public List<Group> getRoles(@PathParam("dn") String dn) throws LdapException {
        List<Group> roles = groupDao.getRoles(dn);
        if(roles != null){
            roles = roles.stream()
                    .filter(group -> !"ghap.io".equals(group.getInfo()))
                    .collect(Collectors.toList());
        }
        CollectionUtils.sortGroups(roles);
        return roles;
    }

    @Override
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Group create(GroupFormData data) throws LdapException {
        if(data.getParentDn() == null || data.getParentDn().isEmpty()){
            Dn parentDn = getDefaultParent("default");
            data.setParentDn(parentDn.toString());
        }

        Group group = groupDao.create(data);

        if( !group.getErrors().isEmpty() ){
            throw new WebApplicationException( Response.status(400).entity(group).build() );
        }
        return group;
    }

    @Override
    @POST
    @Path("{dn}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Group update(@PathParam("dn") String dn, GroupFormData data) throws IOException, LdapException {
        Group user = groupDao.find(dn);

        if (user == null)
            throw new NotFoundException("Cannot find user \""+dn+"\"");

        if (!groupDao.update(user, data)) {
            throw new WebApplicationException(Response.status(400).entity(user).build());
        }
        return user;
    }

    @Override
    @DELETE
    @Path("{dn}")
    @Produces(APPLICATION_JSON)
    public Group destroy(@PathParam("dn") String dn) throws LdapException {
        Group group = dn.contains(",") && dn.contains("=") ? new Group(dn) : groupDao.find(dn);
        if(group == null) {
            throw new NotFoundException("Cannot find group \""+dn+"\"");
        }
        else if( !groupDao.delete(group) ){
                throw new NotFoundException("Cannot find group \""+dn+"\"");
        }
        return group;
    }

    @Override
    @GET
    @Path("{groupDn}/add/{memberDn}")
    @Produces(APPLICATION_JSON)
    public Group addMember(@PathParam("groupDn") String groupDn, @PathParam("memberDn") String memberDn) throws LdapException {
        Group group = groupDn.contains(",") && groupDn.contains("=") ? new Group(groupDn) : groupDao.find(groupDn);
        if(group == null) {
            throw new NotFoundException("Cannot find group \""+groupDn+"\"");
        }
        else if( !groupDao.addMember(group, memberDn) ){
            Response response = Response.status(422).entity(group).build();
            throw new WebApplicationException(response);
        }
        return group;
    }

    @Override
    @GET
    @Path("{groupDn}/delete/{memberDn}")
    @Produces(APPLICATION_JSON)
    public Group deleteMember(@PathParam("groupDn") String groupDn, @PathParam("memberDn") String memberDn) throws LdapException {
        Group group = groupDn.contains(",") && groupDn.contains("=") ? new Group(groupDn) : groupDao.find(groupDn);
        if(group == null) {
            throw new NotFoundException("Cannot find group \""+groupDn+"\"");
        }
        else if( !groupDao.deleteMember(group, memberDn) ){
            throw new NotFoundException("Cannot find group \""+groupDn+"\"");
        }
        return group;
    }

}
