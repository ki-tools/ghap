package io.ghap.project.model;

import io.ghap.project.domain.PermissionRule;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.Set;
import java.util.UUID;

/**
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LdapGroup {

    private UUID guid;
    private String objectClass;
    private String dn;
    private String name;

    public UUID getGuid() {
        return guid;
    }

    public void setGuid(UUID guid) {
        this.guid = guid;
    }

    public String getObjectClass() {
        return objectClass;
    }

    public void setObjectClass(String objectClass) {
        this.objectClass = objectClass;
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "LdapGroup{" +
                "guid=" + guid +
                ", objectClass='" + objectClass + '\'' +
                ", dn='" + dn + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
