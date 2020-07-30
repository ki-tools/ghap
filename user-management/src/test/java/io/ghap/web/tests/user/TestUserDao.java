package io.ghap.web.tests.user;

import com.netflix.governator.annotations.AutoBindSingleton;
import io.ghap.user.dao.PasswordGenerator;
import io.ghap.user.dao.UserDao;
import io.ghap.user.form.UserFormData;
import io.ghap.user.model.AbstractModel;
import io.ghap.user.model.Group;
import io.ghap.user.model.User;
import io.ghap.user.model.validation.OnCreate;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.template.exception.PasswordException;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@AutoBindSingleton(UserDao.class)
public class TestUserDao implements UserDao {

    public static String NEW_USER_DN = "0";
    public static String NOT_EXISTED_USER_DN = "wrong-id";
    public static UserFormData INVALID_USER_DATA = new UserFormData();

    @Inject OnCreate onCreate;
    @Inject PasswordGenerator passwordGenerator;

    @Override
    public List<User> findAll(String parentDn, boolean recursive) throws LdapException {
        return Arrays.asList(new User("cn=Test User," + parentDn));
    }

    @Override
    public List<User> findAll(String parentDn, boolean recursive, String filter) throws LdapException {
        return Arrays.asList(new User("cn=Test User," + parentDn));
    }

    @Override
    public User find(String dn) {
        return dn == NOT_EXISTED_USER_DN ? null : new User(dn);
    }

    @Override
    public User find(String name, boolean asAdmin) throws LdapException {
        return find(name);
    }

    @Override
    public User create(UserFormData data) {
        User user = new User(NEW_USER_DN);
        user.setName("TestName");
        user.setPassword( passwordGenerator.generate() );
        onCreate.validate(user);
        return user;
    }

    @Override
    public boolean update(User user, UserFormData data, boolean asAdmin) {
        return data != null && data != INVALID_USER_DATA;
    }

    @Override
    public boolean delete(User user) throws LdapException {
        return true;
    }

    @Override
    public User resetPassword(User user, String oldPassword) throws LdapException {
        return user;
    }

    @Override
    public User resetPasswordByToken(String resetPassworkToken, String newPassword) throws LdapException, PasswordException {
        return new User(NEW_USER_DN);
    }

    @Override
    public List<Group> memberOf(String dn, boolean recursive, boolean showGroups, boolean asAdmin) throws LdapException {
        return Collections.emptyList();
    }

    @Override
    public boolean enable(User user) throws LdapException {
        return user.getDn() != NOT_EXISTED_USER_DN;
    }

    @Override
    public boolean disable(User user) throws LdapException {
        return user.getDn() != NOT_EXISTED_USER_DN;
    }

    @Override
    public String newResetPasswordToken(String dn) throws LdapException {
        return "RESET-PASSWORD-TOKEN";
    }

    @Override
    public AbstractModel findUserOrGroup(String dn) throws LdapException {
        return find(dn);
    }
}
