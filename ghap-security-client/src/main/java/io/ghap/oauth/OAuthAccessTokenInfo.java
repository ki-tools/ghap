package io.ghap.oauth;

import com.github.hburgmeier.jerseyoauth2.api.token.IAccessTokenInfo;
import com.github.hburgmeier.jerseyoauth2.api.user.IUser;

import java.util.Set;

/**
 */
public class OAuthAccessTokenInfo implements IAccessTokenInfo {

    private IUser user;

    private String clientId;

    private Set<String> authorizedScopes;

    public OAuthAccessTokenInfo(IUser user, String clientId, Set<String> authorizedScopes) {
        this.user = user;
        this.clientId = clientId;
        this.authorizedScopes = authorizedScopes;
    }

    @Override
    public IUser getUser() {
        return user;
    }

    @Override
    public String getClientId() {
        return clientId;
    }

    @Override
    public Set<String> getAuthorizedScopes() {
        return authorizedScopes;
    }
}
