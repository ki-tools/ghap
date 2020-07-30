package io.ghap.project.dao.impl;

import io.ghap.project.dao.UserManagementDao;
import io.ghap.project.model.LdapGroup;
import io.ghap.project.model.LdapUser;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;


public class StubUserManagementDao implements UserManagementDao {

    private LdapUser ldapUser;

    @Override
    public Set<LdapUser> getUsersForGroup(String group, String accessToken) {
        return null;
    }

    @Override
    public Set<LdapUser> getUsersForGroup(String[] groups, String accessToken) {
        return null;
    }

    @Override
    public Set<LdapUser> getUsersForGroup(String accessToken) {
        return Collections.singleton(ldapUser);
    }

    @Override
    public Set<String> getEmails(Set<LdapUser> ldapUsers) {
        if (CollectionUtils.isEmpty(ldapUsers)) {
            return Collections.emptySet();
        }
        Set<String> result = new HashSet<>(ldapUsers.size());
        for (LdapUser user : ldapUsers) {
            if (StringUtils.isNotBlank(user.getEmail()) && user.getGuid() != null) {
                result.add(user.getEmail());
            }
        }
        return result;
    }

    @Override
    public Set<LdapUser> getAllUsers(String accessToken) {
        return Collections.singleton(ldapUser);
    }

    @Override
    public Set<LdapGroup> getAllGroups(String accessToken) {
        return null;
    }

    public void setUpUser(String email, UUID uuid) {
        ldapUser = new LdapUser();
        ldapUser.setEmail(email);
        ldapUser.setName(email);
        ldapUser.setDn(email);
        ldapUser.setObjectClass("user");
        ldapUser.setGuid(uuid);
    }

    @Override
    public LdapUser getUser(UUID uuid, String accessToken) {
        return null;
    }
}
