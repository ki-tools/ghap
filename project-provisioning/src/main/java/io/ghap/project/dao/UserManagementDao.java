package io.ghap.project.dao;

import io.ghap.project.model.LdapGroup;
import io.ghap.project.model.LdapUser;

import java.util.Set;
import java.util.UUID;

/**
 */
public interface UserManagementDao {
    Set<LdapUser> getUsersForGroup(String group, String accessToken);

    Set<LdapUser> getUsersForGroup(String[] groups, String accessToken);

    Set<LdapUser> getUsersForGroup(String accessToken);

    Set<String> getEmails(Set<LdapUser> ldapUsers);

    LdapUser getUser(UUID uuid, String accessToken);

    Set<LdapUser> getAllUsers(String accessToken);

    Set<LdapGroup> getAllGroups(String accessToken);
}
