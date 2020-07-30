package io.ghap.user.dao.mapper;

import io.ghap.user.model.AbstractModel;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.template.EntryMapper;

public class AbstractEntryMapper implements EntryMapper<AbstractModel> {
    private final static AbstractEntryMapper instance = new AbstractEntryMapper();

    private AbstractEntryMapper(){

    }

    public static AbstractEntryMapper getInstance(){
        return instance;
    }

    @Override
    public AbstractModel map(Entry entry) throws LdapException {
        if( entry.containsAttribute("objectClass") ){
            Attribute classes = entry.get("objectClass");
            if(classes.contains("user")){
                return UserEntryMapper.getInstance().map(entry);
            }
            else if(classes.contains("group")) {
                return GroupEntryMapper.getInstance().map(entry);
            }
        }
        return null;
    }
}
