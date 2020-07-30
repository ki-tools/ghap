package io.ghap.auth;

import java.security.Principal;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public class LdapPrincipal implements Principal, java.io.Serializable {

    public final String username;
    public final String password;

    private List<String> groups = Collections.emptyList();
    private List<String> roles = Collections.emptyList();
    private boolean adminPrincipal;
    private boolean firstTimeLogon;
    private String email;

    public String dn;
    private String accessToken;

    public LdapPrincipal(String username, String password){
        this.username = username;
        this.password = password;
    }

    @Override
    public String getName() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public List<String> getGroups() {
        return groups;
    }

    public void setGroups(List<String> groups) {
        this.groups = (groups == null) ? Collections.<String>emptyList() :groups;
    }

    public List<String> getRoles() {
        return roles;
    }

    public boolean isAdminPrincipal() {
        return adminPrincipal;
    }

    public void setAdminPrincipal(boolean adminPrincipal) {
        this.adminPrincipal = adminPrincipal;
    }

    public boolean isFirstTimeLogon() {
        return firstTimeLogon;
    }

    public void setFirstTimeLogon(boolean firstTimeLogon) {
        this.firstTimeLogon = firstTimeLogon;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setRoles(List<String> roles) {
        this.roles = (roles == null) ? Collections.<String>emptyList() :roles;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    @Override
    public String toString() {
        return "LdapPrincipal{" +
                "username='" + username + '\'' +
                ", dn=" + dn +
                '}';
    }
}