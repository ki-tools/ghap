package io.ghap.oauth

import grails.plugin.springsecurity.oauthprovider.exceptions.OAuth2AuthorizationEndpointException
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception
import org.springframework.security.oauth2.provider.ClientRegistrationException
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.support.SessionStatus
import org.springframework.web.servlet.ModelAndView

import java.security.Principal

/**
 */
class GhapWrappedAuthorizationEndpoint extends GhapAuthorizationEndpoint {

    @Override
    public ModelAndView authorize(Map<String, Object> model,
                                  @RequestParam Map<String, String> requestParameters, SessionStatus sessionStatus, Principal principal) {
        try {
            return super.authorize(model, requestParameters, sessionStatus, principal);
        }
        catch (ClientRegistrationException e) {
            throw new OAuth2AuthorizationEndpointException(e);
        }
        catch(OAuth2Exception e) {
            throw new OAuth2AuthorizationEndpointException(e);
        }
    }
}
