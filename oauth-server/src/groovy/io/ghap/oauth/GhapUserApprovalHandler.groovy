package io.ghap.oauth

import grails.transaction.Transactional
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.provider.AuthorizationRequest
import org.springframework.security.oauth2.provider.approval.DefaultUserApprovalHandler

/**
 */
class GhapUserApprovalHandler extends DefaultUserApprovalHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    GrailsApplication grailsApplication;

    @Override
    @Transactional
    boolean isApproved(AuthorizationRequest authorizationRequest, Authentication userAuthentication) {
        log.info("start check is approved authorities = '${userAuthentication.authorities}'")
        def clientId = authorizationRequest.clientId
        if (!clientId) {
            return super.isApproved(authorizationRequest, userAuthentication)
        }
        def client = Client.findByClientId(clientId);
        if (!client) {
            return super.isApproved(authorizationRequest, userAuthentication)
        }
        if (client.skipConsent) {
            authorizationRequest.authorities = userAuthentication.authorities
            authorizationRequest.scope = new HashSet<String>(userAuthentication?.authorities?.collect{it?.authority.replace("ROLE_", "").replace("_", " ")})
            return true;
        }

        def approved = super.isApproved(authorizationRequest, userAuthentication)
        log.info("authrequest authorities = '${authorizationRequest.authorities}'")
        return approved
    }

    @Override
    AuthorizationRequest checkForPreApproval(AuthorizationRequest authorizationRequest, Authentication userAuthentication) {
        def principal = userAuthentication.getPrincipal();
        log.error("principal = ${principal}, redirect url = ${authorizationRequest.getRedirectUri()}")

        if (principal?.firstTimeLogon) {
            log.error("first time logon redirect to ${grailsApplication.config.ghap.tos.page}")
            authorizationRequest.setRedirectUri("${grailsApplication.config.ghap.tos.page}?firstTime=true");
        }else if (principal?.passwordExpiredFlag) {
            log.error("password expired redirect to ${grailsApplication.config.ghap.password.expired.redirect.page}")
            authorizationRequest.setRedirectUri(grailsApplication.config.ghap.password.expired.redirect.page);
        } else {
            def uri = authorizationRequest.getRedirectUri();
            if (!uri || uri.contains("oauth.")) {
                log.error("disable internal redirect. redirect to ${grailsApplication.config.ghap.tos.page}")
                authorizationRequest.setRedirectUri(grailsApplication.config.ghap.tos.page);
            }
            log.error("password not expired redirect to ${authorizationRequest.getRedirectUri()}")
        }
        return super.checkForPreApproval(authorizationRequest, userAuthentication)
    }
}
