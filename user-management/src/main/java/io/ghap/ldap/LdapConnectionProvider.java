package io.ghap.ldap;

import com.google.inject.throwingproviders.CheckedProvider;
import org.apache.directory.api.ldap.model.exception.LdapException;

import javax.ws.rs.WebApplicationException;


public interface LdapConnectionProvider<T> extends CheckedProvider<T> {
    public T get() throws LdapException, WebApplicationException;
}
