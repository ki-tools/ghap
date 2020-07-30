package io.ghap.user.dao.mapper;

import io.ghap.ldap.LdapUtils;
import io.ghap.user.model.User;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.template.EntryMapper;

import static io.ghap.ldap.LdapUtils.convertToDashedString;


public class UserEntryMapper implements EntryMapper<User> {
    private final static UserEntryMapper instance = new UserEntryMapper();

    private UserEntryMapper(){

    }

    public static UserEntryMapper getInstance(){
        return instance;
    }

    @Override
    public User map(Entry entry) throws LdapException {
        User user = new User(entry.getDn().getName());

        if( entry.containsAttribute("userPrincipalName") ) {

            String[] nameAndRealm = entry.get("userPrincipalName").getString().split("@");
            if (nameAndRealm.length == 2) {
                user.setName(nameAndRealm[0]);
            }

        }

        if( entry.containsAttribute("cn") )
            user.setFullName( entry.get("cn").getString() );

        if(user.getName() == null && entry.containsAttribute("sAMAccountName"))
            user.setName(entry.get("sAMAccountName").getString());

        if( entry.containsAttribute("objectGUID") )
            user.setGuid(convertToDashedString(entry.get("objectGUID").getBytes()));

        if( entry.containsAttribute("pwdLastSet") ) {
            String pwdLastSet = entry.get("pwdLastSet").getString();
            user.setResetPassword( "0".equals(pwdLastSet) );
            user.setPwdLastSet( LdapUtils.fromLdapDate(pwdLastSet) );
        }

        if( entry.containsAttribute("badPasswordTime") ) {
            user.setBadPasswordTime(LdapUtils.fromLdapDate(entry.get("badPasswordTime").getString()));
        }

        if( entry.containsAttribute("badPwdCount") ) {
            user.setBadPwdCount( Integer.parseInt( entry.get("badPwdCount").getString() ) );
        }

        if( entry.containsAttribute("userAccountControl") ) {
            user.setUserAccountControl(Integer.parseInt(entry.get("userAccountControl").getString()));
        }

        if( entry.containsAttribute("givenName") )
            user.setFirstName( entry.get("givenName").getString() );

        if( entry.containsAttribute("sn") )
            user.setLastName(entry.get("sn").getString());

        if( entry.containsAttribute("mail") )
            user.setEmail( entry.get("mail").getString());

        return user;
    }
}
