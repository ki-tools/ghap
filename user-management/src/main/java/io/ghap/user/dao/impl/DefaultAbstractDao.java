package io.ghap.user.dao.impl;

import io.ghap.auth.LdapConfiguration;
import io.ghap.auth.LdapPrincipal;
import io.ghap.ldap.LdapConnectionFactory;
import io.ghap.ldap.LdapConnectionProvider;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.inject.Inject;

public abstract class DefaultAbstractDao {
    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    @Inject protected LdapConfiguration ldapConfiguration;
    @Inject protected LdapConnectionProvider<LdapConnection> ldapConnectionProvider;
    @Inject protected LdapConnectionFactory ldapConnectionFactory;

    protected LdapConnection getConnection(final boolean asAdmin) throws LdapException {
        final LdapPrincipal admin = ldapConfiguration.getAdmin();
        return asAdmin ? ldapConnectionFactory.get(admin):ldapConnectionProvider.get();
    }
}
