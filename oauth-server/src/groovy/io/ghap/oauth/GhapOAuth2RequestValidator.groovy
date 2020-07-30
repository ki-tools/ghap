package io.ghap.oauth

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.oauth2.common.exceptions.InvalidScopeException
import org.springframework.security.oauth2.provider.AuthorizationRequest
import org.springframework.security.oauth2.provider.ClientDetails
import org.springframework.security.oauth2.provider.OAuth2RequestValidator
import org.springframework.security.oauth2.provider.TokenRequest

/**
 */
class GhapOAuth2RequestValidator implements OAuth2RequestValidator{

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    void validateScope(AuthorizationRequest authorizationRequest, ClientDetails clientDetails) throws InvalidScopeException {
        //do nothing
    }

    @Override
    void validateScope(TokenRequest tokenRequest, ClientDetails clientDetails) throws InvalidScopeException {
        def scope = tokenRequest.getScope()
        log.info("scopes in request are ${scope}")
        //do nothing
    }
}
