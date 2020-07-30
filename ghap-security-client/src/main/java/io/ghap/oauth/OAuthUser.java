package io.ghap.oauth;

import com.github.hburgmeier.jerseyoauth2.api.user.IUser;

import java.util.Set;

/**
 */
public class OAuthUser implements IUser {

    private final String name;
    private final String email;
    private final String accessToken;
    private Set<String> roles;

    public OAuthUser(String name, String email, String accessToken) {
        this.name = name;
        this.email = email;
        this.accessToken = accessToken;
    }

    public OAuthUser(String name, String email, Set<String> roles, String accessToken) {
        this(name, email, accessToken);
        this.roles = roles;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getEmail() { return email; }

    public String getAccessToken() {
        return accessToken;
    }

    public Set<String> getRoles() {
        return roles;
    }
}
