package io.ghap.oauth

import java.net.URL
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception
import org.springframework.security.oauth2.provider.ClientDetails
import org.springframework.security.oauth2.provider.endpoint.DefaultRedirectResolver

/**
 */
class GhapRedirectResolver extends DefaultRedirectResolver {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    String resolveRedirect(String requestedRedirect, ClientDetails client) throws OAuth2Exception {
        log.error("start resolve redirect. uri = ${requestedRedirect}")
        if (!client.registeredRedirectUri) {
            log.debug("Use redirect URI without validation: " + requestedRedirect)
            return requestedRedirect
        }
        else if(requestedRedirect){
            String reqHost = new URL(requestedRedirect).getHost()
            for(String uriStr:client.registeredRedirectUri){
                String regHost = new URL(uriStr).getHost()
                if( allowRedirect(reqHost, regHost) ){
                    log.debug("Use redirect URI with host name validation: " + requestedRedirect)
                    return requestedRedirect
                }
            }
            log.info("Check redirect URL: " + requestedRedirect);
            for(String uriStr:client.registeredRedirectUri){
                String regHost = new URL(uriStr).getHost()
                log.info(allowRedirect(reqHost, regHost) + " ---> " + uriStr)
            }
        }
        String result = super.resolveRedirect(requestedRedirect, client)
        log.debug("Use redirect URI with full validation: " + result)
        return result
    }

    private static boolean allowRedirect(String reqHost, String regHost){
        reqHost = reqHost.toLowerCase()
        regHost = regHost.toLowerCase()

        boolean allow = reqHost.equals(regHost) || reqHost.endsWith("." + regHost)

        if( !allow ){
            String[] reqArr = reqHost.split("\\.")
            String[] regArr = regHost.split("\\.")

            allow = reqArr.length >= 2 && regArr.length >=2 &&
                    reqArr[reqArr.length-1].equals(regArr.length-1) &&
                    reqArr[reqArr.length-2].equals(regArr.length-2)
        }
        return allow
    }
}
