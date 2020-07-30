package io.ghap.web.tests.ldap;

import com.google.inject.Injector;
import io.ghap.auth.LdapConfiguration;
import io.ghap.ldap.LdapConnectionProvider;
import org.apache.directory.ldap.client.api.LdapConnection;

import javax.inject.Inject;

/**
 *
 */
public class TestLdapConnectionProvider implements LdapConnectionProvider {

    @Inject
    Injector injector;

    @Inject
    LdapConfiguration ldapConfiguration;

    @Override
    public LdapConnection get() {
        // TODO implement connection retrieve
        return null;
    }

}
