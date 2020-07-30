package io.ghap.project.model;

import io.ghap.project.domain.PermissionRule;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.Set;
import java.util.UUID;

/**
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LdapUser {

    private UUID guid;
    private String email;
    private String objectClass;
    private String dn;
    private String name;
    private String firstName;
    private String lastName;
    private Set<PermissionRule> permissions;

    public UUID getGuid() {
        return guid;
    }

    public void setGuid(UUID guid) {
        this.guid = guid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Set<PermissionRule> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<PermissionRule> permissions) {
        this.permissions = permissions;
    }

    @Override
    public String toString() {
        return "LdapUser{" +
                "guid=" + guid +
                ", email='" + email + '\'' +
                ", objectClass='" + objectClass + '\'' +
                ", dn='" + dn + '\'' +
                ", name='" + name + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                '}';
    }
}
