package io.ghap.auth;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * An extension for the HTTPServletRequest that overrides the getUserPrincipal() and isUserInRole().
 *  We supply these implementations here, where they are not normally populated unless we are going through
 *  the facility provided by the container.
 * <p>If he user or roles are null on this wrapper, the parent request is consulted to try to fetch what ever the container has set for us.
 * This is intended to be created and used by the UserRoleFilter.
 * @author thein
 *
 */
public class UserRoleRequestWrapper extends HttpServletRequestWrapper {


    LdapPrincipal user;
    HttpServletRequest realRequest;

    public UserRoleRequestWrapper(LdapPrincipal user,HttpServletRequest request) {
        super(request);
        this.user = user;
        this.realRequest = request;
//    if (this.roles == null) {
//      this.roles = new ArrayList<String>();
//    }
    }

    @Override
    public boolean isUserInRole(String role) {
        if (this.user == null) {
            return this.realRequest.isUserInRole(role);
        }
        return this.user.isAdminPrincipal() || this.user.getRoles().contains(role) || this.user.getGroups().contains(role);
    }

    @Override
    public Principal getUserPrincipal() {
        if (this.user == null) {
            return realRequest.getUserPrincipal();
        }

        // make an anonymous implementation to just return our user
        return user;
    }
}
