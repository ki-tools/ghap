package io.ghap.user.dao.impl;

import io.ghap.auth.LdapConfiguration;
import io.ghap.auth.LdapPrincipal;
import io.ghap.ldap.LdapConnectionFactory;
import io.ghap.ldap.LdapConnectionProvider;
import io.ghap.ldap.LdapUtils;
import io.ghap.ldap.template.LdapConnectionTemplate;
import io.ghap.user.dao.DomainDao;
import io.ghap.user.dao.mapper.DomainEntryMapper;
import io.ghap.user.model.Domain;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;

public class DefaultDomainDao implements DomainDao {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final LdapConnectionProvider<LdapConnection> ldapConnectionProvider;

    @Inject private LdapConnectionFactory ldapConnectionFactory;

    @Inject
    LdapConfiguration ldapConfiguration;

    @Inject
    public DefaultDomainDao(LdapConnectionProvider<LdapConnection> ldapConnectionProvider){
        this.ldapConnectionProvider = ldapConnectionProvider;
        log.debug("New {} instance was created", this.getClass().getSimpleName());
    }

    @Override
    public Domain find(String dn) throws LdapException {
        Domain domain;

        final LdapPrincipal admin = ldapConfiguration.getAdmin();
        try(LdapConnection ldap = ldapConnectionFactory.get(admin)) {

            LdapConnectionTemplate connectionTemplate = new LdapConnectionTemplate(ldap);

            if (dn.contains(",") && dn.contains("=")) {
                // search by user DN
                domain = connectionTemplate.lookup(connectionTemplate.newDn(dn), DomainEntryMapper.getInstance());
            }
            else {
                String filter = String.format("(&(objectClass=domain)(name=%s))", dn);
                domain = connectionTemplate.searchFirst(ldapConfiguration.getRootDn(ldap), filter, SearchScope.SUBTREE, DomainEntryMapper.getInstance());
            }

        }
        catch (IOException e){
            throw new LdapException(e);
        }

        return domain;
    }

    @Override
    public Domain find() throws LdapException {
        String realm = ldapConfiguration.getLdapRealm();
        Dn domainDn = LdapUtils.toDn(realm);
        return find(domainDn.toString());
    }
}
