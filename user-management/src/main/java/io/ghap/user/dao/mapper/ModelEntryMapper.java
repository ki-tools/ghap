package io.ghap.user.dao.mapper;

import io.ghap.user.model.AbstractModel;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.template.EntryMapper;

import java.util.Iterator;

/**
 *
 */
public class ModelEntryMapper implements EntryMapper<AbstractModel> {
    private final static ModelEntryMapper instance = new ModelEntryMapper();

    private ModelEntryMapper(){

    }

    public static ModelEntryMapper getInstance(){
        return instance;
    }

    @Override
    public AbstractModel map(Entry entry) throws LdapException {
        Attribute objectClasses = entry.get("objectClass");

        AbstractModel domain = null;

        for(Iterator<Value<?>> it = objectClasses.iterator();it.hasNext();){
            String clazz = it.next().getString();
            switch(clazz){
                case "user":
                    domain = UserEntryMapper.getInstance().map(entry);
                    break;
                case "group":
                    domain = GroupEntryMapper.getInstance().map(entry);
                    break;
                default:
            }
        }

        return domain;
    }
}
