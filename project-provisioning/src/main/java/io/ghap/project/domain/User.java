package io.ghap.project.domain;

import org.codehaus.jackson.annotate.JsonIgnore;

import javax.persistence.*;
import java.util.Objects;
import java.util.Set;

/**
 */
@Entity
@Table(name = "USERS")
public class User extends BaseDBEntityImpl {

    public User(){

    }

    public User(final ObjectClass objectClass){
        this.objectClass = Objects.requireNonNull(objectClass, "\"objectClass\" attribute is not defined. It can be \"user\" or \"group\"");
    }

    @Column(columnDefinition="varchar(16) default 'user'")
    @Enumerated(EnumType.STRING)
    private ObjectClass objectClass = ObjectClass.user;

    @Column
    private String login;

    @Column
    private String email;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user", cascade = {CascadeType.REMOVE})
    @JsonIgnore
    private Set<Permission> permissions;

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public ObjectClass getObjectClass() {
        return objectClass;
    }

    public void setObjectClass(ObjectClass objectClass) {
        this.objectClass = objectClass;
    }

    public String getStashTarget() {
        return objectClass == null ? ObjectClass.user.getStashTarget() : objectClass.getStashTarget();
    }

    public boolean isGroup() {
        return ObjectClass.group == objectClass;
    }
}
