package io.ghap.auth;

import com.sun.jersey.spi.container.ContainerRequest;
import io.ghap.ldap.LdapConnectionProvider;
import org.apache.directory.ldap.client.api.LdapConnection;

import javax.ws.rs.core.SecurityContext;

import static java.util.Objects.requireNonNull;


public class LdapSecurityContext implements SecurityContext {

    private final LdapPrincipal principal;
    private final boolean secure;
    private final LdapConnectionProvider<LdapConnection> ldapConnectionProvider;

    public LdapSecurityContext(ContainerRequest request, final LdapPrincipal principal, final LdapConnectionProvider<LdapConnection> ldapConnectionProvider) {
        requireNonNull(request, "Request should be defined");

        this.secure = "https".equalsIgnoreCase( request.getRequestUri().getScheme() );
        this.principal = requireNonNull(principal, "LdapPrincipal should be defined");
        this.ldapConnectionProvider = requireNonNull(ldapConnectionProvider, "Injector should be defined");;
    }

    @Override
    public LdapPrincipal getUserPrincipal() {
        return this.principal;
    }

    @Override
    public boolean isUserInRole(String role) {
        //TODO use ldap connection provider to check
        return true;
    }

    @Override
    public boolean isSecure() {
        return secure;
    }


    @Override
    public String getAuthenticationScheme() {
        return SecurityContext.BASIC_AUTH;
    }
}
