package io.ghap.oauth;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.Set;

/**
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TokenPrincipal {

    private String name;
    private String email;
    private String password;
    private Set<String> roles;
    private Set<String> groups;
    private boolean adminPrincipal;

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }

    public void setEmail(String email) { this.email = email; }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public Set<String> getGroups() {
        return groups;
    }

    public void setGroups(Set<String> groups) {
        this.groups = groups;
    }

    public boolean isAdminPrincipal() {
        return adminPrincipal;
    }

    public void setAdminPrincipal(boolean adminPrincipal) {
        this.adminPrincipal = adminPrincipal;
    }
}
