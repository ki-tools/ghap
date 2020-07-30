package io.ghap.user.manager;

import io.ghap.user.form.UserFormData;
import io.ghap.user.model.Group;
import io.ghap.user.model.User;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.template.exception.PasswordException;

import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.util.List;

public interface UserService extends AbstractService {

    List<User> findAll(String parentDn, String range) throws LdapException;;

    User getCurrentUser(SecurityContext securityContext) throws LdapException;

    User get(String dn) throws LdapException;

    User create(UserFormData user) throws LdapException;

    void resetPassword(String dn, UserFormData userFormData, SecurityContext securityContext) throws LdapException;

    User resetPasswordByToken(String resetPassworkToken, String password) throws LdapException, PasswordException;

    User sendResetPasswordEmail(String userDn, String urlPattern) throws LdapException;

    User checkPassword(String password) throws LdapException;

    User update(String dn, UserFormData user, SecurityContext securityContext) throws IOException, LdapException;

    User destroy(String dn) throws LdapException;

    String getServiceName();

    List<Group> getGroups(String dn) throws LdapException;

    List<Group> getRoles(String dn) throws LdapException;

    User enable(String dn) throws LdapException;

    User disable(String dn) throws LdapException;
}

