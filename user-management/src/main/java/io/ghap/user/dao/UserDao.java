package io.ghap.user.dao;

import io.ghap.user.form.UserFormData;
import io.ghap.user.model.Group;
import io.ghap.user.model.User;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.template.exception.PasswordException;

import java.util.List;

/**
 *
 */
public interface UserDao extends AbstractDao {


    List<User> findAll(String parentDn, boolean recursive) throws LdapException;

    List<User> findAll(String parentDn, boolean recursive, String filter) throws LdapException;

    User find(String dn) throws LdapException;

    User find(String name, boolean asAdmin) throws LdapException;

    /**
     * Create new user
     * @param user
     * @return new user if create operation was successful and null if not
     */
    User create(UserFormData user) throws LdapException;

    /**
     * Update user data
     * @param user
     * @param asAdmin
     * @return true if update was success, false if not
     */
    boolean update(User user, UserFormData data, boolean asAdmin) throws LdapException;

    boolean delete(User user) throws LdapException;

    User resetPassword(User user, String oldPassword) throws LdapException;

    User resetPasswordByToken(String resetPassworkToken, String newPassword) throws LdapException, PasswordException;

    List<Group> memberOf(String dn, boolean recursive, boolean showGroups, boolean asAdmin) throws LdapException;

    boolean enable(User user) throws LdapException;

    boolean disable(User user) throws LdapException;

    String newResetPasswordToken(String dn) throws LdapException;

}
