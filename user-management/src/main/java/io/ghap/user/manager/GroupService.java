package io.ghap.user.manager;

import io.ghap.user.form.GroupFormData;
import io.ghap.user.model.AbstractModel;
import io.ghap.user.model.Group;
import org.apache.directory.api.ldap.model.exception.LdapException;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.io.IOException;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;


public interface GroupService {

    /**
     * Return groups with specified parent Dn
     * @param parentDn - if parentDn equals "default" then it uses root Dn + "cn=users"
     * @param range
     * @return
     * @throws LdapException
     */
    List<Group> findAll(String parentDn, String range) throws LdapException;;

    /**
     * Return all roles and groups since it search object by class name from root DN
     * @param range
     * @return
     * @throws LdapException
     */
    List<Group> findAll(String range) throws LdapException;

    Group get(String dn) throws LdapException;

    List<AbstractModel> getMembers(String dn) throws LdapException;

    List<AbstractModel> getUsers(String dn) throws LdapException;

    List<Group> getRoles(String dn) throws LdapException;

    Group create(GroupFormData group) throws LdapException;

    Group update(String dn, GroupFormData group) throws IOException, LdapException;

    Group destroy(String dn) throws LdapException;

    Group addMember(String groupDn, String memberDn) throws LdapException;

    Group deleteMember(String groupDn, String memberDn) throws LdapException;
}
