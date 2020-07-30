package io.ghap.user.manager;

import org.apache.directory.api.ldap.model.exception.LdapException;

import javax.ws.rs.PathParam;


public interface PreviewEmailService {
    public String newUserEmailPreview(String dn) throws LdapException;
    public String passwordExpiraionEmailPreview(String dn) throws LdapException;
    public String updateUserEmailPreview(@PathParam("dn") String dn) throws LdapException;
}
