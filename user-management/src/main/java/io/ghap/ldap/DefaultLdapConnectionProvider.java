package io.ghap.ldap;

import com.google.inject.Injector;
import io.ghap.auth.LdapConfiguration;
import io.ghap.auth.LdapPrincipal;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.core.SecurityContext;

public class DefaultLdapConnectionProvider implements LdapConnectionProvider {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Inject Injector injector;
    @Inject LdapConnectionFactory ldapConnectionFactory;

    @Inject
    LdapConfiguration ldapConfiguration;

    @Override
    public LdapConnection get() throws LdapException {
        SecurityContext sc = injector.getInstance(SecurityContext.class);
        LdapPrincipal ldapPrincipal = (LdapPrincipal) sc.getUserPrincipal();

        return ldapConnectionFactory.get(ldapPrincipal);
    }


}
