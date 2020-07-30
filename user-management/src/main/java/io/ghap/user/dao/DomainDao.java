package io.ghap.user.dao;

import io.ghap.user.model.Domain;
import org.apache.directory.api.ldap.model.exception.LdapException;

public interface DomainDao {

    Domain find(String dn) throws LdapException;

    Domain find() throws LdapException;
}
