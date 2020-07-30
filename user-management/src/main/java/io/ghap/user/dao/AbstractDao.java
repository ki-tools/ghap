package io.ghap.user.dao;

import io.ghap.user.model.AbstractModel;
import org.apache.directory.api.ldap.model.exception.LdapException;

public interface AbstractDao {
    AbstractModel findUserOrGroup(String dn) throws LdapException;
}
