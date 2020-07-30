package io.ghap.user.dao.impl;


import io.ghap.ldap.LdapUtils;
import io.ghap.ldap.Permission;
import io.ghap.ldap.template.LdapConnectionTemplate;
import io.ghap.user.dao.PasswordGenerator;
import io.ghap.user.dao.UserDao;
import io.ghap.user.dao.ValidationError;
import io.ghap.user.dao.mapper.AbstractEntryMapper;
import io.ghap.user.dao.mapper.GroupEntryMapper;
import io.ghap.user.dao.mapper.UserEntryMapper;
import io.ghap.user.exceptions.EntryNotFound;
import io.ghap.user.form.UserFormData;
import io.ghap.user.model.AbstractModel;
import io.ghap.user.model.Group;
import io.ghap.user.model.User;
import io.ghap.user.model.validation.OnCreate;
import io.ghap.user.model.validation.OnResetPassword;
import io.ghap.user.model.validation.OnUpdate;
import org.apache.commons.lang.time.DateUtils;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.*;
import org.apache.directory.api.ldap.model.message.controls.SortKey;
import org.apache.directory.api.ldap.model.message.controls.SortRequestControlImpl;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.template.PasswordWarning;
import org.apache.directory.ldap.client.template.exception.PasswordException;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

import static io.ghap.ldap.Const.*;
import static io.ghap.ldap.LdapUtils.convertToByteString;

//import org.apache.bval.guice.Validate;

@Singleton
public class DefaultUserDao extends DefaultAbstractDao implements UserDao {

    private final Pattern guidPattern = Pattern.compile("\\w{8}-\\w{4}-\\w{4}-\\w{4}-\\w{12}");

    @Inject Permission permission;

    @Inject OnCreate onCreate;
    @Inject OnUpdate onUpdate;
    @Inject OnResetPassword onResetPassword;

    @Inject PasswordGenerator passwordGenerator;

  @Override
    public List<User> findAll(String parentDn, boolean recursive) throws LdapException {
        List<User> users;
        try(LdapConnection ldap = getConnection(false)) {

            LdapConnectionTemplate connectionTemplate = new LdapConnectionTemplate(ldap);

            Dn dn = (parentDn == null || parentDn.isEmpty()) ?
                    ldapConfiguration.getRootDn(ldap).add("cn=users") : connectionTemplate.newDn(parentDn);

            String filter = "(&(objectClass=user)(!(isCriticalSystemObject=*)))";

            SearchRequest searchRequest = connectionTemplate.newSearchRequest(dn, filter, recursive ? SearchScope.SUBTREE : SearchScope.ONELEVEL);

            // pagination related part
            SortRequestControlImpl sortRequestControl = new SortRequestControlImpl();
            sortRequestControl.addSortKey(new SortKey("sAMAccountName"));

            searchRequest.addControl(sortRequestControl);


            users = connectionTemplate.search(searchRequest, UserEntryMapper.getInstance());

        } catch (IOException e) {
            throw new LdapException(e);
        }
        return users;
    }

    @Override
    public List<User> findAll(String parentDn, boolean recursive, String filter) throws LdapException {
        List<User> users;
        try(LdapConnection ldap = getConnection(false)) {

            LdapConnectionTemplate connectionTemplate = new LdapConnectionTemplate(ldap);

            Dn dn = (parentDn == null || parentDn.isEmpty()) ?
                    ldapConfiguration.getRootDn(ldap).add("cn=users") : connectionTemplate.newDn(parentDn);

            if(filter == null)
                filter = "(objectClass=user)";

            SearchRequest searchRequest = connectionTemplate.newSearchRequest(dn, filter, recursive ? SearchScope.SUBTREE : SearchScope.ONELEVEL);

            // pagination related part
            SortRequestControlImpl sortRequestControl = new SortRequestControlImpl();
            sortRequestControl.addSortKey(new SortKey("sAMAccountName"));

            searchRequest.addControl(sortRequestControl);

            users = connectionTemplate.search(searchRequest, UserEntryMapper.getInstance());

        } catch (IOException e) {
            throw new LdapException(e);
        }
        return users;
    }

    @Override
    public User find(String name) throws LdapException {
        return find(name, false);
    }

    @Override
    public AbstractModel findUserOrGroup(String name) throws LdapException {
        AbstractModel user;

        try(LdapConnection ldap = getConnection(false)) {

            LdapConnectionTemplate connectionTemplate = new LdapConnectionTemplate(ldap);

            if (name.contains(",") && name.contains("=")) {
                // search by user DN
                user = connectionTemplate.lookup(connectionTemplate.newDn(name), AbstractEntryMapper.getInstance());
            }
            else {
                String filter;
                if( name.contains("@") ){
                    filter = String.format("(&(|(objectClass=user)(objectClass=group))(|(userPrincipalName=%s)(mail=%s)))", name, name);
                }
                else if(guidPattern.matcher(name).matches()) {
                    filter = String.format("(&(|(objectClass=user)(objectClass=group))(objectGUID=%s))", convertToByteString(name.trim()));
                }
                else {
                    filter = String.format("(&(|(objectClass=user)(objectClass=group))(sAMAccountName=%s))", name);
                }
                user = connectionTemplate.searchFirst(ldapConfiguration.getRootDn(ldap), filter, SearchScope.SUBTREE, AbstractEntryMapper.getInstance());
            }

        }
        catch (IOException e){
            throw new LdapException(e);
        }

        return user;
    }

    @Override
    public User find(String name, boolean asAdmin) throws LdapException {
        User user;

        try(LdapConnection ldap = getConnection(asAdmin)) {

            LdapConnectionTemplate connectionTemplate = new LdapConnectionTemplate(ldap);

            if (name.contains(",") && name.contains("=")) {
                // search by user DN
                user = connectionTemplate.lookup(connectionTemplate.newDn(name), UserEntryMapper.getInstance());
            }
            else {
                String filter;
                if( name.contains("@") ){
                    filter = String.format("(&(objectClass=user)(|(userPrincipalName=%s)(mail=%s)))", name, name);
                }
                else if(guidPattern.matcher(name).matches()) {
                    filter = String.format("(&(objectClass=user)(objectGUID=%s))", convertToByteString(name.trim()));
                }
                else {
                    filter = String.format("(&(objectClass=user)(sAMAccountName=%s))", name);
                }
                user = connectionTemplate.searchFirst(ldapConfiguration.getRootDn(ldap), filter, SearchScope.SUBTREE, UserEntryMapper.getInstance());
            }

        }
        catch (IOException e){
            throw new LdapException(e);
        }

        return user;
    }

    @Override
    public User create(UserFormData data) throws LdapException {

        User user;

        if(data.getPassword() == null || data.getPassword().isEmpty()){
            // generate password if it wasn't provided
            data.setPassword(passwordGenerator.generate());
        }

        try(LdapConnection ldap = getConnection(false)) {

            LdapConnectionTemplate connectionTemplate = new LdapConnectionTemplate(ldap);

            String cn = getFullName(data.getFirstName(), data.getLastName());
            if(cn == null){
                cn = data.getName();
            }

            String parentDn = (data.getParentDn() == null || data.getParentDn().trim().isEmpty()) ?
                    ldapConfiguration.getRootDn(ldap).add("cn=users").toString() : data.getParentDn();

            Dn dn = connectionTemplate.newDn("cn=" + cn + "," + parentDn);

            final int newUAC = UF_NORMAL_ACCOUNT;

            Entry entry = connectionTemplate.newEntry(dn);
            entry.add( "objectClass", "top", "person", "user", "organizationalPerson", "inetOrgPerson", "posixAccount" )
                    .add("cn", cn)
                    .add("name", cn)
                    .add("displayName", cn)
                    .put("userAccountControl", "" + newUAC);

            if(data.getEmail() != null)
                entry.add("mail", data.getEmail());

            if(data.getName() != null) {
                entry.add("sAMAccountName", data.getName())
                     .put("userPrincipalName", data.getName() + "@" + ldapConfiguration.getLdapRealm());
                     //.add("uid", UUID.randomUUID().toString())// no need to use it since LDAP creates "objectGUID"
            }

            if(data.getFirstName() != null && !data.getFirstName().isEmpty())
                entry.add( "givenName", data.getFirstName() );

            if(data.getLastName() != null && !data.getLastName().isEmpty())
                entry.add( "sn", data.getLastName() );

            // for Windows 2008 AD compatibility
            entry.put("unicodePwd", ("\"" + data.getPassword() + "\"").getBytes(StandardCharsets.UTF_16LE));

            AddRequest addRequest = connectionTemplate.newAddRequest(entry);

            user = UserEntryMapper.getInstance().map(entry);
            user.setPassword( data.getPassword());

            onCreate.validate(user);

            AddResponse response = connectionTemplate.add(addRequest);


            ResultCodeEnum code = response.getLdapResult().getResultCode();

            if( code != ResultCodeEnum.SUCCESS ){
                String field = (code == ResultCodeEnum.ENTRY_ALREADY_EXISTS) ? "name":"";
                user.getErrors().add(new ValidationError(field, code.getMessage(), response.getLdapResult().getDiagnosticMessage()));
            } else {
                pwdLastSet(dn, 0);
                user = connectionTemplate.lookup(dn, UserEntryMapper.getInstance());
                user.setPassword( data.getPassword() );

                setUserUidAndGid(user);
            }

        }
        catch (IOException e){
            throw new LdapException(e);
        }

        return user;
    }

    @Override
    public boolean update(User user, UserFormData data, boolean asAdmin) throws LdapException {
        data.toUser(user);

        boolean generatePassword = (data.getPassword() != null && data.getPassword().isEmpty());
        if( generatePassword ){
            // generate password if empty password was provided
            data.setPassword(passwordGenerator.generate());
            user.setPassword(null);
        }

        try( LdapConnection ldap = getConnection(true) ) {

            LdapConnectionTemplate connectionTemplate = new LdapConnectionTemplate(ldap);

            Dn dn = connectionTemplate.newDn(user.getDn());

            String firstName = data.getFirstName() != null ? data.getFirstName():user.getFirstName();
            String lastName  = data.getLastName()  != null ? data.getLastName() :user.getLastName();

            String cn = getFullName(firstName, lastName);

            onUpdate.validate(user);

            if(data.getFirstName() != null || data.getLastName() != null){
                dn =  connectionTemplate.rename(dn, dn.getParent().add("cn=" + cn), "firstAndLastName", user.getErrors());
                if(dn == null || !user.getErrors().isEmpty()){
                    return false;
                }
                user.setDn(dn.toString());
                user.setFullName(cn);
            }

            ModifyRequest modifyRequest = connectionTemplate.newModifyRequest(dn);

            if(data.getFirstName() != null || data.getLastName() != null){

                if(cn != null)
                    modifyRequest.replace("displayName", cn);

                if( data.getFirstName() != null ) {
                    if( data.getFirstName().trim().isEmpty() ) {
                        modifyRequest.replace("givenName");
                    }
                    else modifyRequest.replace("givenName", data.getFirstName());
                }

                if( data.getLastName() != null ) {
                    if( data.getLastName().trim().isEmpty() ){
                        modifyRequest.replace("sn");
                    }
                    else modifyRequest.replace("sn", data.getLastName());
                }
            }

            if( data.getEmail() != null )
                modifyRequest.replace("mail", data.getEmail());

            if( data.getDisabled() != null ){
                int uac = user.getUserAccountControl();
                //see: http://ldapwiki.willeke.com/wiki/User-Account-Control%20Attribute%20Values
                final int userAccountControl = !data.getDisabled() ? (uac & 0xFFFFFFFD) : (uac | 0x00000002);
                modifyRequest.replace("userAccountControl", Integer.toString(userAccountControl));
                if( !data.getDisabled() ){
                    modifyRequest.replace("badPwdCount", Integer.toString(0));
                }
                user.setUserAccountControl(userAccountControl);
            }

            if( data.getName() != null ) {
                log.warn("User tried to rename " + dn);
                /*
                modifyRequest.replace("sAMAccountName", data.getName())
                        .replace("userPrincipalName", data.getName() + "@" + ldapConfiguration.getLdapRealm());
                if(cn == null)
                    modifyRequest.replace("displayName", data.getName());
                 */
            }


            ModifyResponse modifyResponse = null;
            ResultCodeEnum code = ResultCodeEnum.SUCCESS;

            if( !modifyRequest.getModifications().isEmpty() ) {
                modifyResponse = connectionTemplate.modify(modifyRequest);
                code = modifyResponse.getLdapResult().getResultCode();
            }

            if( code != ResultCodeEnum.SUCCESS ){
                String field = (code == ResultCodeEnum.ENTRY_ALREADY_EXISTS) ? "name":"";
                user.getErrors().add(new ValidationError(field, code.getMessage(), modifyResponse.getLdapResult().getDiagnosticMessage()));

            } else {

                if(data.getPassword() != null) try {

                    PasswordWarning result;
                    if(asAdmin){
                        result = connectionTemplate.modifyPassword( dn, null, data.getPassword());
                        //if(generatePassword) {
                            pwdLastSet(dn, 0);
                        //}
                    }
                    else {
                        permission.userCanChangePassword(user.getDn());
                        //set pwd last month earlier before changing password. Avoid password too young
                        setPwdLastSetMonthEarlier(dn);
                        try( LdapConnection ldapAsUser = getConnection(false) ) {
                            LdapConnectionTemplate t = new LdapConnectionTemplate(ldapAsUser);
                            result = t.modifyPassword(dn, data.getCurrentPassword(), data.getPassword(), false);
                        }
                        pwdLastSet(dn, -1);
                    }

                    // set new password to user
                    user.setPassword(data.getPassword());

                    //TODO process "result" somehow
                }
                catch (PasswordException e) {
                    user.getErrors().add(new ValidationError("password", e.getResultCode().getMessage(), e.getMessage()));
                }
            }

        }
        catch (IOException e){
            throw new LdapException(e);
        }

        return user.getErrors().isEmpty();
    }

    @Override
    public boolean delete(User user) throws LdapException {
        try(LdapConnection ldap = getConnection(false)) {

            LdapConnectionTemplate connectionTemplate = new LdapConnectionTemplate(ldap);
            DeleteResponse response = connectionTemplate.delete(connectionTemplate.newDn(user.getDn()));

            ResultCodeEnum code = response.getLdapResult().getResultCode();
            if( code != ResultCodeEnum.SUCCESS ){
                user.getErrors().add(new ValidationError("", code.getMessage(), response.getLdapResult().getDiagnosticMessage()));
            }
        } catch (IOException e) {
            throw new LdapException(e);
        }
        return user.getErrors().isEmpty();
    }

    @Override
    public User resetPassword(User user, String oldPassword) throws LdapException {

        onResetPassword.validate(user);

        try( LdapConnection ldap = getConnection(true) ) {

            LdapConnectionTemplate connectionTemplate = new LdapConnectionTemplate(ldap);
            try {

                //set pwd last month earlier before changing password. Avoid password too young
                setPwdLastSetMonthEarlier(connectionTemplate.newDn(user.getDn()));
                PasswordWarning passwordWarning = connectionTemplate.modifyPassword(user.getDn(), oldPassword, user.getPassword());
                pwdLastSet(connectionTemplate.newDn(user.getDn()), -1);

            } catch (PasswordException e) {
                String resultCode = e.getResultCode() == null ? "passwordException":e.getResultCode().getMessage();
                String msg = e.getMessage() == null ? e.getLdapException().getMessage():e.getMessage();
                user.getErrors().add(new ValidationError("password", resultCode, msg));
            }
        } catch (IOException e) {
            throw new LdapException(e);
        }
        return user;
    }

    @Override
    public User resetPasswordByToken(String resetPassworkToken, String newPassword) throws LdapException, PasswordException {
        User user;

        try(LdapConnection ldap = getConnection(true)) {

            LdapConnectionTemplate connectionTemplate = new LdapConnectionTemplate(ldap);

            String filter =  String.format("(&(objectClass=user)(url=%s))", resetPassworkToken);
            user = connectionTemplate.searchFirst(ldapConfiguration.getRootDn(ldap), filter, SearchScope.SUBTREE, UserEntryMapper.getInstance());
            if(user != null){
                user.setPassword(newPassword);
                onResetPassword.validate(user);

                Dn dn = connectionTemplate.newDn(user.getDn());

                String currentPassword = passwordGenerator.generate();

                setPwdLastSetMonthEarlier(dn);//set pwd last month earlier before changing password. Avoid password too young
                PasswordWarning passwordWarning = connectionTemplate.modifyPassword(dn, null, currentPassword);

                //set pwd last month earlier before changing password. Avoid password too young
                setPwdLastSetMonthEarlier(dn);
                try {
                    connectionTemplate.modifyPassword(user.getDn(), currentPassword, user.getPassword());
                    connectionTemplate.modify(dn, request -> request.remove("url", resetPassworkToken));

                    pwdLastSet(dn, -1);
                }
                catch (PasswordException e) {
                    user.getErrors().add(new ValidationError("password", e.getResultCode().getMessage(), e.getMessage()));
                }
            }
        }
        catch (IOException e){
            throw new LdapException(e);
        }
        return user;
    }

    @Override
    public List<Group> memberOf(String dn, boolean recursive, boolean showGroups, boolean asAdmin) throws LdapException {
        List<Group> groups;
        try(LdapConnection ldap = getConnection(asAdmin)) {

            LdapConnectionTemplate connectionTemplate = new LdapConnectionTemplate(ldap);

            Dn rootDn = ldapConfiguration.getRootDn(ldap);

            if (recursive) {
                Entry entry = ldap.lookup( dn, new String[]{"tokenGroups"} );

                if(  Objects.isNull(entry) ){
                    throw new EntryNotFound("Cannot find entry with DN=\""+dn+"\"");
                }

                //paceholder for an LDAP filter that will store SIDs of the groups the user belongs to
                StringBuffer groupsSearchFilter = new StringBuffer();
                groupsSearchFilter.append("(|");

                Attribute tokenGroups = entry.get("tokenGroups");

                for(Iterator<Value<?>> it = tokenGroups.iterator();it.hasNext();) {
                    byte[] sid = it.next().getBytes();
                    groupsSearchFilter.append("(objectSid=" + LdapUtils.binarySidToStringSid(sid) + ")");
                }

                groupsSearchFilter.append(")");

                // Search for groups the user belongs to in order to get their names
                groups = connectionTemplate.search(
                        rootDn,
                        groupsSearchFilter.toString(),
                        SearchScope.SUBTREE,
                        GroupEntryMapper.getInstance());
            }
            else {
                String filter = String.format("(&(objectClass=group)(member=%s))", dn);
                groups = connectionTemplate.search(
                        rootDn,
                        filter,
                        SearchScope.SUBTREE,
                        GroupEntryMapper.getInstance());
            }

            List<Group> filteredGroups = new ArrayList<Group>();
            for (Iterator<Group> iterator = groups.iterator(); iterator.hasNext();) {
                Group group = iterator.next();
                if ( (group.getDn().contains(ROLES_CN_NAME) && !showGroups) ||
                        (!group.getDn().contains(ROLES_CN_NAME) && showGroups) ) {
                    filteredGroups.add(group);
                }
            }
            groups = filteredGroups;

        } catch (IOException e) {
            throw new LdapException(e);
        }
        return groups;
    }

    @Override
    public boolean enable(User user) throws LdapException {
        return changeStatus(user, true);
    }

    @Override
    public boolean disable(User user) throws LdapException {
        return changeStatus(user, false);
    }

    @Override
    public String newResetPasswordToken(String userDn) throws LdapException {
        final String token = UUID.randomUUID().toString();
        try(LdapConnection ldap = getConnection(true)) {
            LdapConnectionTemplate connectionTemplate = new LdapConnectionTemplate(ldap);
            Dn dn = connectionTemplate.newDn(userDn);
            connectionTemplate.modify(dn, request -> request.replace("url", token));
        } catch (IOException e) {
            throw new LdapException(e);
        }
        return token;
    }

    private ModifyResponse updateUserAccountControl(final Dn dn, final int newUAC) throws LdapException, IOException {
        try(LdapConnection ldap = getConnection(true)){
            final LdapConnectionTemplate connectionTemplate = new LdapConnectionTemplate(ldap);
            return connectionTemplate.modify(dn, request -> request.replace("userAccountControl", Integer.toString(newUAC)));
        }
    }

    private ModifyResponse pwdLastSet(final Dn dn, final long val) throws LdapException, IOException {
        // This method should be disabled for AD
        try(LdapConnection ldap = getConnection(true)){
            final LdapConnectionTemplate connectionTemplate = new LdapConnectionTemplate(ldap);
            return connectionTemplate.modify(dn, request -> request.replace("pwdLastSet", Long.toString(val)));
        }
    }

    private ModifyResponse setPwdLastSetMonthEarlier(final Dn dn) throws IOException, LdapException {
        return pwdLastSet(dn, LdapUtils.toLdapDate(DateUtils.addMonths(new Date(), -1)));
    }

    private ModifyResponse setAccountExpires(final Dn dn, final int val) throws LdapException, IOException {
        try(LdapConnection ldap = getConnection(true)){
            final LdapConnectionTemplate connectionTemplate = new LdapConnectionTemplate(ldap);
            return connectionTemplate.modify(dn, request -> request.replace("accountExpires", Integer.toString(val)));
        }
    }

    private ModifyResponse setUserUidAndGid(final User user) throws LdapException, IOException {

      final String userUid = OsCommandExecutionUtils.getUserUid(user.getName());
      final String userGid = OsCommandExecutionUtils.getUserGid(user.getName());

      if (userUid != null && userGid != null) {
        try(LdapConnection ldap = getConnection(true)){
          final LdapConnectionTemplate connectionTemplate = new LdapConnectionTemplate(ldap);

          return connectionTemplate.modify(connectionTemplate.newDn(user.getDn()),
                  request -> request.replace("uidNumber", userUid).replace("gidNumber", userGid));
        }
      } else {
        log.error(String.format("The uidNumber and gidNumber could not be set for user <%s>", user.getName()));
        return null;
      }

    }

    private boolean changeStatus(User user, boolean enable) throws LdapException {
        try (LdapConnection ldap = getConnection(false)) {

            LdapConnectionTemplate connectionTemplate = new LdapConnectionTemplate(ldap);

            int uac = user.getUserAccountControl();

            //see: http://ldapwiki.willeke.com/wiki/User-Account-Control%20Attribute%20Values
            final int userAccountControl = enable ? (uac & 0xFFFFFFFD) : (uac | 0x00000002);

            ModifyResponse response = connectionTemplate.modify(
                    connectionTemplate.newDn(user.getDn()),
                    request -> request
                            .replace("userAccountControl", Integer.toString(userAccountControl))
                            .replace("lockoutTime", "0") // for AD instead of below
                            // below code cannot be used with AD since "badPwdCount" is SAM owned attribute and we can't touch them
                            //.replace("badPwdCount", Integer.toString(0))
            );

            ResultCodeEnum code = response.getLdapResult().getResultCode();

            if(code != ResultCodeEnum.SUCCESS){
                user.getErrors().add(new ValidationError("", code.getMessage(), response.getLdapResult().getDiagnosticMessage()));
            }
            else {
                user.setUserAccountControl(userAccountControl);
            }

            return user.getErrors().isEmpty();

        } catch (IOException e) {
            throw new LdapException(e);
        }
    }


    private String getFullName(String firstName, String lastName){
        String cn = null;
        if(firstName != null && !firstName.isEmpty()){
            cn = firstName;
        }
        if(lastName != null && !lastName.isEmpty()){
            cn = (cn == null) ? lastName : String.join(" ", cn, lastName);
        }
        return cn;
    }
}
