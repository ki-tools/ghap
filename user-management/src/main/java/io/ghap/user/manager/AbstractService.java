package io.ghap.user.manager;

import io.ghap.user.model.AbstractModel;
import org.apache.directory.api.ldap.model.exception.LdapException;

public interface AbstractService {
    AbstractModel getUserOrGroup(String dn) throws LdapException;
}
