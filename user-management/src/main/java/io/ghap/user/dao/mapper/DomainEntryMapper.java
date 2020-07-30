package io.ghap.user.dao.mapper;

import io.ghap.user.model.Domain;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.ldap.client.template.EntryMapper;

import static io.ghap.ldap.LdapUtils.convertToDashedString;

public class DomainEntryMapper implements EntryMapper<Domain> {

    private final static int ONE_HUNDRED_NANOSECOND = 10000000;
    private final static long SECONDS_IN_DAY = 86400;

    private final static DomainEntryMapper instance = new DomainEntryMapper();

    private DomainEntryMapper(){

    }

    public static DomainEntryMapper getInstance(){
        return instance;
    }

    @Override
    public Domain map(Entry entry) throws LdapException {
        Domain domain = new Domain(entry.getDn().getName());

        if( entry.containsAttribute("objectGUID") )
            domain.setGuid(convertToDashedString(entry.get("objectGUID").getBytes()));

        if( entry.containsAttribute("name") )
            domain.setName(entry.get("name").getString());

        if( entry.containsAttribute("minPwdLength") )
            domain.setMinPwdLength(Integer.parseInt(entry.get("minPwdLength").getString()));

        if( entry.containsAttribute("lockoutThreshold") )
            domain.setLockoutThreshold(Integer.parseInt(entry.get("lockoutThreshold").getString()));

        if( entry.containsAttribute("maxPwdAge") ){
            domain.setMaxPwdAge( toDays(entry.get("maxPwdAge")));
        }

        if( entry.containsAttribute("minPwdAge") ){
            domain.setMinPwdAge( toDays(entry.get("minPwdAge")));
        }

        return domain;
    }

    private int toDays(Attribute attr) throws LdapInvalidAttributeValueException {
        String val = attr.getString();

        long valAsLong = Math.abs(Long.parseLong(val));
        long valAsSec = valAsLong / ONE_HUNDRED_NANOSECOND;
        int valAsDays = (int) (valAsSec / SECONDS_IN_DAY);
        return valAsDays;
    }
}
