package io.ghap.auth.authorize;

import io.ghap.auth.LdapPrincipal;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiSession;
import org.apache.wiki.auth.WikiSecurityException;
import org.apache.wiki.auth.authorize.Role;
import org.apache.wiki.auth.authorize.WebAuthorizer;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Collection;
import java.util.Properties;

public class GhapWebAuthorizer implements WebAuthorizer {
    private Role[] roles = new Role[0];
    @Override
    public boolean isUserInRole(HttpServletRequest request, Principal role) {
        return request.isUserInRole(role.getName());
    }

    @Override
    public Role[] getRoles() {
        Collection<GhapRole> ghapRoles =  UserManagementClient.getInstance().getRoles();
        final Role[] roles = new Role[ghapRoles.size()];
        int i=0;
        for(GhapRole role:ghapRoles){
            roles[i++] = new Role(role.getName());
        }
        this.roles = roles;
        return roles;
    }

    @Override
    public Role findRole(String roleName) {
        for(Role role:roles){
            if(role.getName().equalsIgnoreCase(roleName)){
                return role;
            }
        }
        return null;
    }

    @Override
    public void initialize(WikiEngine engine, Properties props) throws WikiSecurityException {
        // nothing to do
    }

    @Override
    public boolean isUserInRole(WikiSession session, Principal role) {

        if ( session == null || role == null )
        {
            return false;
        }
        Principal principal = session.getUserPrincipal();
        return session.hasPrincipal( role ) || (principal instanceof LdapPrincipal && ((LdapPrincipal)principal).isAdminPrincipal());

        /*
        Principal principal = session.getUserPrincipal();
        if(principal instanceof LdapPrincipal){
            LdapPrincipal user = (LdapPrincipal) principal;
            return user.isAdminPrincipal() || user.getRoles().contains(role) || user.getGroups().contains(role);
        }
        return false;
        */
    }
}
