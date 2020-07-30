package io.ghap.auth;

import org.apache.directory.api.ldap.model.name.Dn;

import java.security.Principal;

/**
 *
 */
public class LdapPrincipal implements Principal, java.io.Serializable {

    public final String username;
    public final String password;
    public Dn dn;
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

    public Dn getDn() {
        return dn;
    }

    public void setDn(Dn dn) {
        this.dn = dn;
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
