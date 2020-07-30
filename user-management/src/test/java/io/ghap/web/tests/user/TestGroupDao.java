package io.ghap.web.tests.user;

import com.netflix.governator.annotations.AutoBindSingleton;
import io.ghap.user.dao.GroupDao;
import io.ghap.user.form.GroupFormData;
import io.ghap.user.model.AbstractModel;
import io.ghap.user.model.Group;
import org.apache.directory.api.ldap.model.exception.LdapException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@AutoBindSingleton(GroupDao.class)
public class TestGroupDao implements GroupDao {

    public static String NEW_GROUP_DN = "0";
    public static String NOT_EXISTED_GROUP_DN = "wrong-id";
    public static GroupFormData INVALID_GROUP_DATA = new GroupFormData();

    @Override
    public List<Group> findAll(String parentDn, boolean recursive) throws LdapException {
        return Arrays.asList(new Group("cn=Test Group," + parentDn));
    }

    @Override
    public List<AbstractModel> getMembers(String dn, boolean recursice) throws LdapException {
        return Collections.emptyList();
    }

    @Override
    public Group find(String dn) throws LdapException {
        return dn == NOT_EXISTED_GROUP_DN ? null : new Group(dn);
    }

    @Override
    public Group create(GroupFormData user) throws LdapException {
        return new Group(NEW_GROUP_DN);
    }

    @Override
    public boolean update(Group group, GroupFormData data) throws LdapException {
        return data != null && data != INVALID_GROUP_DATA;
    }

    @Override
    public boolean delete(Group group) throws LdapException {
        return group.getDn() == NOT_EXISTED_GROUP_DN ? false : true;
    }

    @Override
    public boolean addMember(Group group, String dn) throws LdapException {
        return group.getDn() == NOT_EXISTED_GROUP_DN ? false : true;
    }

    @Override
    public boolean deleteMember(Group group, String memberDn) throws LdapException {
        return group.getDn() == NOT_EXISTED_GROUP_DN ? false : true;
    }

	@Override
	public List<Group> getRoles(String dn) throws LdapException {
		return Collections.emptyList();
	}
}
