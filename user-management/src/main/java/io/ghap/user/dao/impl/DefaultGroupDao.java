package io.ghap.user.dao.impl;

import io.ghap.auth.LdapConfiguration;
import io.ghap.ldap.LdapConnectionProvider;
import io.ghap.ldap.template.LdapConnectionTemplate;
import io.ghap.user.dao.GroupDao;
import io.ghap.user.dao.ValidationError;
import io.ghap.user.dao.mapper.GroupEntryMapper;
import io.ghap.user.dao.mapper.ModelEntryMapper;
import io.ghap.user.form.GroupFormData;
import io.ghap.user.model.AbstractModel;
import io.ghap.user.model.Group;
import io.ghap.user.model.validation.OnCreate;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.*;
import org.apache.directory.api.ldap.model.message.controls.SortKey;
import org.apache.directory.api.ldap.model.message.controls.SortRequestControlImpl;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

import static io.ghap.ldap.Const.ROLES_CN_NAME;


public class DefaultGroupDao extends DefaultAbstractDao implements GroupDao {

    @Inject OnCreate onCreate;

    @Override
    public List<Group> findAll(String parentDn, boolean recursive) throws LdapException {
        List<Group> groups;
        try(LdapConnection ldap = ldapConnectionProvider.get()) {

            LdapConnectionTemplate connectionTemplate = new LdapConnectionTemplate(ldap);

            String filter = "(objectClass=group)";

            SearchRequest searchRequest = connectionTemplate.newSearchRequest(
                    connectionTemplate.newDn(parentDn),
                    filter,
                    recursive ? SearchScope.SUBTREE : SearchScope.ONELEVEL
            );

            // pagination related part
            SortRequestControlImpl sortRequestControl = new SortRequestControlImpl();
            sortRequestControl.addSortKey(new SortKey("sAMAccountName"));

            searchRequest.addControl(sortRequestControl);

            groups = connectionTemplate.search(searchRequest, GroupEntryMapper.getInstance());

        } catch (IOException e) {
            throw new LdapException(e);
        }
        return groups;
    }

    @Override
    public List<AbstractModel> getMembers(String dn, boolean recursive) throws LdapException {
        List<AbstractModel> members;
        try(LdapConnection ldap = ldapConnectionProvider.get()) {

            LdapConnectionTemplate connectionTemplate = new LdapConnectionTemplate(ldap);

            String filter = recursive ?
                    String.format("(&(objectCategory=*)(sAMAccountName=*)(memberOf:1.2.840.113556.1.4.1941:=%s))", dn) :
                    String.format("(&(objectCategory=*)(sAMAccountName=*)(memberOf=%s))", dn);

            members = connectionTemplate.search(
                    ldapConfiguration.getRootDn(ldap),
                    filter,
                    SearchScope.SUBTREE,
                    ModelEntryMapper.getInstance());

        } catch (IOException e) {
            throw new LdapException(e);
        }
        return members;
    }

    @Override
    public Group find(String dn) throws LdapException {
        Group group;

        try(LdapConnection ldap = ldapConnectionProvider.get()) {

            LdapConnectionTemplate connectionTemplate = new LdapConnectionTemplate(ldap);

            if (dn.contains(",") && dn.contains("=")) {
                // search by user DN
                group = connectionTemplate.lookup(connectionTemplate.newDn(dn), GroupEntryMapper.getInstance());
            }
            else {
                String filter = String.format("(&(objectClass=group)(sAMAccountName=%s))", dn);
                group = connectionTemplate.searchFirst(ldapConfiguration.getRootDn(ldap), filter, SearchScope.SUBTREE, GroupEntryMapper.getInstance());
            }

        }
        catch (IOException e){
            throw new LdapException(e);
        }

        return group;
    }

    @Override
    public Group create(GroupFormData data) throws LdapException {
    	if (isRole(data)) {
    		createAbsentRoleContainer();
    	}

        Group group;

        try(LdapConnection ldap = ldapConnectionProvider.get()) {

            LdapConnectionTemplate connectionTemplate = new LdapConnectionTemplate(ldap);

            String cn = data.getName();
            Dn dn = connectionTemplate.newDn("cn=" + cn + "," + data.getParentDn());

            Entry entry = connectionTemplate.newEntry(dn);
            entry.add("objectClass", "top", "group")
                    .add("cn", cn)
                    .add( "name", cn  )
                    .add( "description", data.getDescription() )
                    .add("groupType", String.valueOf( 0xFFFFFFFF00000000L | 0x80000000L | 0x00000002L) )//[ GlobalScope, Security ] see: https://msdn.microsoft.com/en-us/library/ms675935(v=vs.85).aspx
                    .add("sAMAccountName", cn);

            group = onCreate.validate( GroupEntryMapper.getInstance().map(entry) );

            AddRequest addRequest = connectionTemplate.newAddRequest(entry);
            AddResponse response = connectionTemplate.add(addRequest);

            ResultCodeEnum code = response.getLdapResult().getResultCode();

            if( code != ResultCodeEnum.SUCCESS ){
                String field = (code == ResultCodeEnum.ENTRY_ALREADY_EXISTS) ? "name":"";
                group.getErrors().add(new ValidationError(field, code.getMessage(), response.getLdapResult().getDiagnosticMessage()));
            }

        }
        catch (IOException e){
            throw new LdapException(e);
        }

        return group;
    }

	@Override
    public boolean update(Group group, GroupFormData data) throws LdapException {
        if(data.getDescription() == null && data.getName() == null)
            return true;

        try(LdapConnection ldap = ldapConnectionProvider.get()) {

            LdapConnectionTemplate connectionTemplate = new LdapConnectionTemplate(ldap);

            Dn dn = connectionTemplate.newDn(group.getDn());
            String newCn = data.getName();

            if( newCn != null ) {
                dn =  connectionTemplate.rename(dn, dn.getParent().add("cn=" + newCn), "name", group.getErrors());
                if(dn == null || !group.getErrors().isEmpty()){
                    return false;
                }
                group.setDn(dn.toString());
            }

            data.toGroup(group);

            ModifyRequest modifyRequest = connectionTemplate.newModifyRequest(dn);

            if(newCn != null){
                modifyRequest.replace("sAMAccountName", newCn);
            }

            if (data.getDescription() != null )
                modifyRequest.replace("description", data.getDescription());


            ModifyResponse modifyResponse = connectionTemplate.modify(modifyRequest);

            ResultCodeEnum code = modifyResponse.getLdapResult().getResultCode();

            if( code != ResultCodeEnum.SUCCESS ){
                String field = (code == ResultCodeEnum.ENTRY_ALREADY_EXISTS) ? "name":"";
                group.getErrors().add(new ValidationError(field, code.getMessage(), modifyResponse.getLdapResult().getDiagnosticMessage()));
            }

        }
        catch (IOException e){
            throw new LdapException(e);
        }

        return group.getErrors().isEmpty();
    }

    @Override
    public boolean delete(Group group) throws LdapException {
        try(LdapConnection ldap = ldapConnectionProvider.get()) {

            LdapConnectionTemplate connectionTemplate = new LdapConnectionTemplate(ldap);
            DeleteResponse response = connectionTemplate.delete(connectionTemplate.newDn(group.getDn()));

            ResultCodeEnum code = response.getLdapResult().getResultCode();
            if( code != ResultCodeEnum.SUCCESS ){
                group.getErrors().add(new ValidationError("", code.getMessage(), response.getLdapResult().getDiagnosticMessage()));
            }
        } catch (IOException e) {
            throw new LdapException(e);
        }
        return group.getErrors().isEmpty();
    }

    @Override
    public boolean addMember(Group group, String memberDn) throws LdapException {
        try(LdapConnection ldap = ldapConnectionProvider.get()) {

            LdapConnectionTemplate connectionTemplate = new LdapConnectionTemplate(ldap);

            Dn dn = connectionTemplate.newDn(group.getDn());
            ModifyResponse response = connectionTemplate.modify(dn, request -> request.add("member", memberDn));

            ResultCodeEnum code = response.getLdapResult().getResultCode();
            if( code != ResultCodeEnum.SUCCESS ){
                group.getErrors().add(new ValidationError("", code.getMessage(), response.getLdapResult().getDiagnosticMessage()));
            }

        } catch (IOException e) {
            throw new LdapException(e);
        }
        return group.getErrors().isEmpty();
    }

    @Override
    public boolean deleteMember(Group group, String memberDn) throws LdapException {
        try(LdapConnection ldap = ldapConnectionProvider.get()) {

            LdapConnectionTemplate connectionTemplate = new LdapConnectionTemplate(ldap);

            Dn dn = connectionTemplate.newDn(group.getDn());
            ModifyResponse response = connectionTemplate.modify(dn, request -> request.remove("member", memberDn));

            ResultCodeEnum code = response.getLdapResult().getResultCode();
            if( code != ResultCodeEnum.SUCCESS ){
                group.getErrors().add(new ValidationError("", code.getMessage(), response.getLdapResult().getDiagnosticMessage()));
            }

        } catch (IOException e) {
            throw new LdapException(e);
        }
        return group.getErrors().isEmpty();
    }

    @Override
    public List<Group> getRoles(String dn) throws LdapException {
        List<Group> groups;
        try(LdapConnection ldap = ldapConnectionProvider.get()) {

            LdapConnectionTemplate connectionTemplate = new LdapConnectionTemplate(ldap);

            String rootDn = "cn=" + ROLES_CN_NAME + "," + ldapConfiguration.getRootDn(ldap).toString();
            String filter = String.format("(&(objectClass=group)(member=%s))", dn);
            groups = connectionTemplate.search(
                    connectionTemplate.newDn(rootDn),
                    filter,
                    SearchScope.SUBTREE,
                    GroupEntryMapper.getInstance());

        } catch (IOException e) {
            throw new LdapException(e);
        }
        return groups;
    }

    private boolean isRole(GroupFormData data) {
		return data.getParentDn().toLowerCase().contains("cn=roles");
	}

    private void createAbsentRoleContainer() throws LdapException {
		String dn = "";
		Group group = null;
		try(LdapConnection ldap = ldapConnectionProvider.get()) {
			dn = "cn=" + ROLES_CN_NAME + "," + ldapConfiguration.getRootDn(ldap).toString();
            LdapConnectionTemplate connectionTemplate = new LdapConnectionTemplate(ldap);
            group = connectionTemplate.lookup(connectionTemplate.newDn(dn), GroupEntryMapper.getInstance());
        }
        catch (IOException e){}

		if (group != null)
			return;

		try(LdapConnection ldap = ldapConnectionProvider.get()) {
            LdapConnectionTemplate connectionTemplate = new LdapConnectionTemplate(ldap);
            Entry entry = connectionTemplate.newEntry(connectionTemplate.newDn(dn));
            entry.add("objectClass", "top", "container")
                    .add("cn", ROLES_CN_NAME)
                    .add("name", ROLES_CN_NAME);

            AddRequest addRequest = connectionTemplate.newAddRequest(entry);

            AddResponse response = connectionTemplate.add(addRequest);

            group = GroupEntryMapper.getInstance().map(entry);

            ResultCodeEnum code = response.getLdapResult().getResultCode();

            if( code != ResultCodeEnum.SUCCESS ){
                String field = (code == ResultCodeEnum.ENTRY_ALREADY_EXISTS) ? "name":"";
                group.getErrors().add(new ValidationError(field, code.getMessage(), response.getLdapResult().getDiagnosticMessage()));
            }

        }
        catch (IOException e){
            throw new LdapException(e);
        }
	}

}
