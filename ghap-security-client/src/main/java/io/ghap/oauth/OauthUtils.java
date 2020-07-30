package io.ghap.oauth;

import com.github.hburgmeier.jerseyoauth2.api.user.IUser;
import com.github.hburgmeier.jerseyoauth2.rs.impl.base.context.OAuthPrincipal;

import javax.ws.rs.core.SecurityContext;
import java.security.Principal;

/**
 */
public class OauthUtils {

    public static OAuthUser getOAuthUser(SecurityContext securityContext) {
        if (securityContext == null) {
            return null;
        }
        Principal principal = securityContext.getUserPrincipal();
        if (principal == null || !(principal instanceof OAuthPrincipal)) {
            return null;
        }
        IUser user = ((OAuthPrincipal) principal).getUser();
        if (user == null || !(user instanceof OAuthUser)) {
            return null;
        }
        return ((OAuthUser) user);
    }

    public static String getAccessToken(SecurityContext securityContext) {
        OAuthUser oAuthUser = getOAuthUser(securityContext);
        if (oAuthUser == null) {
            return null;
        }
        return oAuthUser.getAccessToken();
    }
}
