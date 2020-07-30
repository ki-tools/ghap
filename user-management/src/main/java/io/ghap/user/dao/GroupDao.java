package io.ghap.user.dao;

import io.ghap.user.form.GroupFormData;
import io.ghap.user.model.AbstractModel;
import io.ghap.user.model.Group;
import org.apache.directory.api.ldap.model.exception.LdapException;

import java.util.List;

/**
 *
 */
public interface GroupDao {
    List<Group> findAll(String parentDn, boolean recursive) throws LdapException;

    List<AbstractModel> getMembers(String dn, boolean recursice) throws LdapException;

    Group find(String dn) throws LdapException;

    Group create(GroupFormData user) throws LdapException;

    boolean update(Group group, GroupFormData data) throws LdapException;

    boolean delete(Group group) throws LdapException;

    boolean addMember(Group group, String memberDn) throws LdapException;

    boolean deleteMember(Group group, String memberDn) throws LdapException;

    List<Group> getRoles(String dn) throws LdapException;

}
