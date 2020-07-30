package io.ghap.user.dao.mapper;

import io.ghap.user.model.Group;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.template.EntryMapper;

import static io.ghap.ldap.LdapUtils.convertToDashedString;

/**
 *
 */
public class GroupEntryMapper implements EntryMapper<Group> {

    private final static GroupEntryMapper instance = new GroupEntryMapper();

    private GroupEntryMapper(){

    }

    public static GroupEntryMapper getInstance(){
        return instance;
    }

    @Override
    public Group map(Entry entry) throws LdapException {
        Group group = new Group(entry.getDn().getName());

        if( entry.containsAttribute("objectGUID") )
            group.setGuid(convertToDashedString(entry.get("objectGUID").getBytes()));

        if( entry.containsAttribute("sAMAccountName") )
            group.setName(entry.get("sAMAccountName").getString());

        if( entry.containsAttribute("description") )
            group.setDescription(entry.get("description").getString());

        if( entry.containsAttribute("info") )
            group.setInfo(entry.get("info").getString());
        return group;
    }
}
