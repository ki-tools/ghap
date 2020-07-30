package io.ghap.user.manager.impl;

import com.amazonaws.AmazonClientException;
import com.google.inject.Singleton;
import com.netflix.governator.annotations.Configuration;
import com.sun.jersey.api.NotFoundException;
import io.ghap.auth.LdapPrincipal;
import io.ghap.auth.OauthCient;
import io.ghap.ldap.CollectionUtils;
import io.ghap.mailer.NewUserMailer;
import io.ghap.mailer.PasswordResetMailer;
import io.ghap.mailer.PasswordUpdatedMailer;
import io.ghap.mailer.UpdateUserMailer;
import io.ghap.user.dao.DomainDao;
import io.ghap.user.dao.PasswordGenerator;
import io.ghap.user.dao.UserDao;
import io.ghap.user.dao.UserStorageDao;
import io.ghap.user.form.UserFormData;
import io.ghap.user.manager.UserService;
import io.ghap.user.manager.tracking.Change;
import io.ghap.user.manager.tracking.UserChanges;
import io.ghap.user.model.AbstractModel;
import io.ghap.user.model.Domain;
import io.ghap.user.model.Group;
import io.ghap.user.model.User;
import io.ghap.user.model.validation.OnResetPassword;
import io.ghap.util.SerialClone;
import org.apache.commons.mail.EmailException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.template.exception.PasswordException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static io.ghap.user.manager.impl.PaginateAnything.paginate;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;

@Path("user")
@Singleton
public class DefaultUserService implements UserService {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Inject private UserDao userDao;

    @Inject private UserStorageDao userStorageDao;

    @Inject private PasswordGenerator passwordGenerator;

    @Inject private DomainDao domainDao;
    @Inject private NewUserMailer newUserMailer;
    @Inject private PasswordUpdatedMailer passwordUpdatedMailer;
    @Inject private PasswordResetMailer passwordResetMailer;
    @Inject private UpdateUserMailer updateUserMailer;
    @Inject private OnResetPassword onResetPassword;

    @Inject private OauthCient oauthCient;


    //@Inject
    //Provider<Dependancy> dependencyProvider;
    @Context private UriInfo uriInfo;
    @Context private HttpServletResponse servletResponse;

    @Configuration("userservice.name")
    private String userServiceName;

    @Override
    @GET
    @Path("all/{dn}")
    @Produces(APPLICATION_JSON)
    public List<User> findAll(@PathParam("dn") String parentDn, @HeaderParam("Range") String range) throws LdapException {
        List<User> users = userDao.findAll("default".equalsIgnoreCase(parentDn) ? null : parentDn, false);
        users = paginate(users, servletResponse, range);
        Domain domain = domainDao.find();
        for(User user:users){
            user.setDomainRelatedProperties(domain);
        }
        return users;
    }

    @Override
    @GET
    @Path("{dn}")
    @Produces(APPLICATION_JSON)
    public User get(@PathParam("dn") String dn) throws LdapException {
        User user = userDao.find(dn);
        if(user == null) {
            throw new NotFoundException("Cannot find user \""+dn+"\"");
        }
        else {
            user.setDomainRelatedProperties(domainDao.find() );
        }

        return user;
    }

    @Override
    @GET
    @Path("or/group/{dn}")
    @Produces(APPLICATION_JSON)
    public AbstractModel getUserOrGroup(@PathParam("dn") String dn) throws LdapException {
        AbstractModel userOrGroup = userDao.findUserOrGroup(dn);
        if(userOrGroup == null) {
            throw new NotFoundException("Cannot find user \""+dn+"\"");
        }

        return userOrGroup;
    }

    @Override
    @GET
    @Produces(APPLICATION_JSON)
    public User getCurrentUser(@Context SecurityContext securityContext) throws LdapException {
        LdapPrincipal principal = (LdapPrincipal)securityContext.getUserPrincipal();
        User user = userDao.find(principal.getName(), true);
        if(user == null) {
            throw new NotFoundException("Cannot find user \""+principal.getName()+"\"");
        }
        else {
            user.setDomainRelatedProperties( domainDao.find() );
        }

        return user;
    }

    @Override
    @POST
    @Path("password/request/{dnOrEmail}")
    @Produces(APPLICATION_JSON)
    public User sendResetPasswordEmail(@PathParam("dnOrEmail") String userDnOrEmail, String urlPattern) throws LdapException {
        if(urlPattern == null || urlPattern.isEmpty()){
            throw new WebApplicationException(Response.status(BAD_REQUEST).entity("URL pattern should be specified in request body(for example \"https://www.ghap.io/#/reset-password-by-token?token=$token$\")").build());
        }
        User user = userDao.find(userDnOrEmail, true);
        if(user == null) {
            throw new NotFoundException("Cannot find user \""+userDnOrEmail+"\"");
        }
        else {
            String resetPasswordToken = userDao.newResetPasswordToken(user.getDn());
            try {
                passwordResetMailer.send(user, urlPattern, resetPasswordToken);
            } catch (EmailException e) {
                // write logs
            }
        }

        return user;
    }

    @Override
    @POST
    @Path("password/token/{resetPassworkToken}")
    @Produces(APPLICATION_JSON)
    public User resetPasswordByToken(@PathParam("resetPassworkToken") String resetPasswordToken, String newPassword) throws LdapException, PasswordException {
        if(newPassword == null || newPassword.isEmpty()){
            throw new WebApplicationException(
                    javax.ws.rs.core.Response.status(400).entity("New password should be provided").build()
            );
        }

        User user = userDao.resetPasswordByToken(resetPasswordToken, newPassword);
        if(user == null){
            throw new WebApplicationException(FORBIDDEN);
        }
        else if( !user.getErrors().isEmpty() ){
            throw new WebApplicationException( Response.status(400).entity(user).build() );
        }
        else {
            // notify user about changes by email
            try {
                passwordUpdatedMailer.send(user, false);
            } catch (EmailException e) {
                // exception was logged before
            }
        }
        return user;
    }


    @Override
    @DELETE
    @Path("{dn}")
    @Produces(APPLICATION_JSON)
    public User destroy(@PathParam("dn") String dn) throws LdapException {

        User user = dn.contains(",") && dn.contains("=") ? new User(dn) : userDao.find(dn);
        if(user == null) {
            throw new NotFoundException("Cannot find user \""+dn+"\"");
        }
        else if( !userDao.delete(user) ){
                throw new NotFoundException("Cannot find user \""+dn+"\"");
        }

        return user;
    }

    @Override
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public User create(UserFormData data) throws LdapException {
        User user = userDao.create(data);

        /*
        try {
            s3Client.makeFolder("", user.getGuid());
        } catch (IOException|AmazonClientException e){
            if( uriInfo == null || uriInfo.getRequestUri().getHost().equalsIgnoreCase("localhost") ) {
                log.error("Cannot create s3 folder for the user \"" + user.getName() + "\". " + e, e);
            } else {
                throw new AmazonClientException("Cannot create s3 folder for the user \"" + user.getName() + "\". " + e, e);
            }
        }
        */

        if( !user.getErrors().isEmpty() ){
            throw new WebApplicationException( Response.status(400).entity(user).build() );
        }

        userStorageDao.createUserWorkspaceStorage(user);
        userStorageDao.createUserLinuxHomeStorage(user);


        if( data.isNotifyByEmail() )try {
            newUserMailer.send(user);
        } catch (EmailException e) {
            // write logs
        }
        return user;
    }

    @Override
    @POST
    @Path("password/reset/{dn:.*}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public void resetPassword(@PathParam("dn") String dn, UserFormData userFormData, @Context SecurityContext securityContext) throws LdapException {
        if(dn == null || dn.isEmpty()){
            LdapPrincipal principal = (LdapPrincipal)securityContext.getUserPrincipal();
            if(principal != null) {
                dn = principal.getName();
            }
        }
        User user = (dn != null) ? userDao.find(dn, true):null;
        if(user == null){
            throw new NotFoundException("Cannot find user \"" + dn + "\"");
        }

        User currentUser = checkUpdatePermissions(securityContext, new Dn(user.getDn()));

        user.setPassword(userFormData.getPassword());
        if(currentUser.equals(user) && userFormData.getCurrentPassword() == null){
            LdapPrincipal principal = (LdapPrincipal)securityContext.getUserPrincipal();
            if( principal.getPassword() != null && !principal.getPassword().isEmpty()){
                userFormData.setCurrentPassword(principal.getPassword());
            } else {
                throw new WebApplicationException(Response.status(400).entity("Current password was not provided").build());
            }
        }
        userDao.resetPassword(user, currentUser.equals(user) ? userFormData.getCurrentPassword():null);// old password should be provided
        //userDao.resetPassword(user, true);
        if( user.getErrors().isEmpty() ) {
            try {
                passwordUpdatedMailer.send(user, false);
            } catch (EmailException e) {
                // write logs
            }
            if (user.equals(currentUser)) {
                LdapPrincipal principal = (LdapPrincipal) securityContext.getUserPrincipal();
                oauthCient.update(principal.getAccessToken(), user.getDn(), user.getPassword());
            } else {
                //TODO Oauth need support to update its DB for old user DN (from @PathParam("dn"))
                // it doesn't support it yet
            }
        }
        else {
            throw new WebApplicationException(Response.status(403).entity(user).build());
        }
    }

    @Override
    @POST
    @Path("password/check")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public User checkPassword(String password) throws LdapException {
        User user = new User();
        user.setPassword(password);
        user = onResetPassword.validate(user);
        return user;
    }

    @Override
    @POST
    @Path("{dn}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public User update(@PathParam("dn") String dn, UserFormData data, @Context SecurityContext securityContext) throws IOException, LdapException {
        User user = userDao.find(dn, true);

        if (user == null)
            throw new NotFoundException("Cannot find user \""+dn+"\"");

        User currentUser = checkUpdatePermissions(securityContext, new Dn(user.getDn()));
        User prevUser = SerialClone.clone(user);
        boolean selfUpdate = currentUser.equals(user);

        //List<Change> changes = UserChanges.check(user, data);
        List<Change> changes = null;
        ConstraintViolationException constraintViolationException = null;

        try {
            if (!userDao.update(user, data, !selfUpdate)) {
                throw new WebApplicationException(Response.status(400).entity(user).build());
            } else {
                changes = UserChanges.check(prevUser, user);
            }
        } catch (ConstraintViolationException e){
            constraintViolationException = e;
        }
        if( changes != null){
            // notify user about changes

            // notify Oauth service
            LdapPrincipal principal = (LdapPrincipal)securityContext.getUserPrincipal();
            String password = (constraintViolationException == null) ? data.getPassword() : null;
            if(selfUpdate){
                oauthCient.update(principal.getAccessToken(), user.getDn(), password);
            }
            else {
                //TODO Oauth need support to update its DB for old user DN (from @PathParam("dn"))
                // it doesn't support it yet
            }
            // notify user about changes by email
            if( data.isNotifyByEmail() ) try {
                boolean hidePassword = data.getPassword() == null || (!data.getPassword().isEmpty() && currentUser.equals(user));
                updateUserMailer.send(user, changes, hidePassword);
            } catch (EmailException e) {
                // exception was logged before
            }
        }
        if(constraintViolationException != null){
            throw constraintViolationException;
        }
        return user;
    }

    @Override
    public String getServiceName() {
        return userServiceName;
    }

    @Override
    @GET
    @Path("groups/{dn}")
    @Produces(APPLICATION_JSON)
    public List<Group> getGroups(@PathParam("dn") String dn) throws LdapException {
        List<Group> groups = userDao.memberOf(dn, false, true, false);
        CollectionUtils.sortGroups(groups);
        return groups;
    }

    @Override
    @GET
    @Path("roles/{dn}")
    @Produces(APPLICATION_JSON)
    public List<Group> getRoles(@PathParam("dn") String dn) throws LdapException {
        List<Group> groups = userDao.memberOf(dn, false, false, true);
        CollectionUtils.sortGroups(groups);
        return groups;
    }

    @Override
    @GET
    @Path("enable/{dn}")
    @Produces(APPLICATION_JSON)
    public User enable(@PathParam("dn") String dn) throws LdapException {
        User user = userDao.find(dn);
        if(user == null){
            throw new NotFoundException("Cannot find user \""+dn+"\"");
        }
        if( !userDao.enable(user) ){
            throw new WebApplicationException(Response.status(400).entity(user).build());
        }
        return user;
    }

    @Override
    @GET
    @Path("disable/{dn}")
    @Produces(APPLICATION_JSON)
    public User disable(@PathParam("dn") String dn) throws LdapException {
        User user = userDao.find(dn);
        if(user == null){
            throw new NotFoundException("Cannot find user \""+dn+"\"");
        }
        if( !userDao.disable(user) ){
            throw new WebApplicationException(Response.status(400).entity(user).build());
        }
        return user;
    }

    private User checkUpdatePermissions(SecurityContext securityContext, Dn userDn) throws LdapException {
        LdapPrincipal principal = (LdapPrincipal)securityContext.getUserPrincipal();
        User currentUser = userDao.find(principal.getName(), true);
        if(currentUser == null){
            throw new NotFoundException("Cannot find user \""+principal.getName()+"\"");
        }
        Dn currentUserDn = new Dn(currentUser.getDn());
        if( !currentUserDn.equals(userDn) ){
            // check that user has admin rights
            List<Group> groups = userDao.memberOf(currentUser.getDn(), true, true, true);
            List<String> groupNames = groups.stream().map(it -> it.getName()).collect(Collectors.toList());
            if( !groupNames.contains("Administrators") ){
                String msg = "Current user \"" + currentUserDn + "\" tried to reset password for \"" + userDn + "\"";
                throw new WebApplicationException(Response.status(FORBIDDEN).entity(msg).build());
            }
        }
        return currentUser;
    }

}
