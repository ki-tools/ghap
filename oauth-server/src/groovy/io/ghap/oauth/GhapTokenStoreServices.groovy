package io.ghap.oauth

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.common.OAuth2AccessToken
import org.springframework.security.oauth2.provider.OAuth2Authentication
import org.springframework.security.oauth2.provider.token.DefaultTokenServices
import org.springframework.transaction.annotation.Transactional

/**
 */
@Transactional
class GhapTokenStoreServices extends DefaultTokenServices {

    @Autowired
    GhapTokenStoreService ghapTokenStoreService

    @Override
    OAuth2AccessToken createAccessToken(OAuth2Authentication authentication) throws AuthenticationException {
        def token = ghapTokenStoreService.getTokenFromAuthentication(authentication);
        if (token) {
            def accessToken = AccessToken.findByAuthenticationKey(token);
            if (accessToken) {
                accessToken.delete(flush: true)
            }
        }
        return super.createAccessToken(authentication)
    }
}
