package io.ghap.user.manager.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.ghap.auth.AuthenticationException;
import io.ghap.auth.LdapPrincipal;
import io.ghap.ldap.LdapConnectionProvider;
import io.ghap.ldap.template.LdapConnectionTemplate;
import io.ghap.user.dao.mapper.UserEntryMapper;
import io.ghap.user.model.User;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.template.exception.LdapRuntimeException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.security.Principal;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;


@Path("auth")
@Singleton
public class AuthService {

    @Inject
    private LdapConnectionProvider<LdapConnection> ldapConnectionProvider;

    @Context
    SecurityContext securityContext;

    @Context
    HttpServletRequest request;

    @GET
    @Path("sign-out")
    public String signOut() {
        HttpSession session = request.getSession();

        if(session != null)
            session.invalidate();

        return null;
    }

    @GET
    @Path("sign-in")
    @Produces(APPLICATION_JSON)
    public User signInGet() {
        return bindConnection();
    }

    @POST
    @Path("sign-in")
    @Produces(APPLICATION_JSON)
    public User signInPost() {
        return bindConnection();
    }

    private User bindConnection(){
        Principal principal = securityContext.getUserPrincipal();
        if(principal instanceof LdapPrincipal){
            try(LdapConnection connection = ldapConnectionProvider.get()){
                LdapPrincipal ldapPrincipal = (LdapPrincipal) principal;
                HttpSession session = request.getSession(true);
                session.setAttribute("username", ldapPrincipal.getName());
                session.setAttribute("password", ldapPrincipal.getPassword());

                User user = new LdapConnectionTemplate(connection).lookup(((LdapPrincipal) principal).getDn(), UserEntryMapper.getInstance());
                return user;
            } catch (LdapException e) {
                throw new LdapRuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            throw new AuthenticationException("Invalid user name or password", "Authentication via form");
        }
    }

}
