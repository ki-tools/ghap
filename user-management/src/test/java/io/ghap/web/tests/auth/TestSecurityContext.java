package io.ghap.web.tests.auth;

import io.ghap.auth.LdapPrincipal;

import javax.ws.rs.core.SecurityContext;
import java.security.Principal;

public class TestSecurityContext implements SecurityContext {
    @Override
    public Principal getUserPrincipal() {
        return new LdapPrincipal("test", "test");
    }

    @Override
    public boolean isUserInRole(String role) {
        return false;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public String getAuthenticationScheme() {
        return null;
    }
}
