package io.ghap.oauth

import grails.util.Holders
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint

import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 */
class GhapAuthenticationEntryPoint extends LoginUrlAuthenticationEntryPoint {

    @Autowired
    GrailsApplication grailsApplication;

    GhapAuthenticationEntryPoint(String loginFormUrl) {
        super(loginFormUrl)
    }

    @Override
    protected String buildRedirectUrlToLoginPage(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) {
        if (request.getRequestURI().contains("oauth/authorize")) {
            return super.buildRedirectUrlToLoginPage(request, response, authException)
        }
        def loginPageUrl = super.buildRedirectUrlToLoginPage(request, response, authException)
        def session = request.getSession(false);
        if (session) {
            session.invalidate();
        }
        return buildLoginUrl(loginPageUrl)
    }

    @Override
    protected String buildHttpsRedirectUrlForRequest(HttpServletRequest request) throws IOException, ServletException {
        if (request.getRequestURI().contains("oauth/authorize")) {
            return super.buildHttpsRedirectUrlForRequest(request)
        }
        def loginPageUrl = super.buildHttpsRedirectUrlForRequest(request)
        def session = request.getSession(false);
        if (session) {
            session.invalidate();
        }
        return buildLoginUrl(loginPageUrl)
    }

    private String buildLoginUrl(String currentLoginUrl) {
        def loginContext = getLoginFormUrl();
        def i = currentLoginUrl.indexOf(loginContext);
        if (i <= 0) {
            return currentLoginUrl;
        }
        currentLoginUrl = currentLoginUrl.substring(0, i);
        if (!currentLoginUrl.endsWith("/")) {
            currentLoginUrl += "/"
        }
        return currentLoginUrl + "oauth/authorize?client_id=${grailsApplication.config.ghap.openid.client.id}&response_type=token&redirect_uri=${Holders.grailsApplication.config.grails.plugin.springsecurity.successHandler.defaultTargetUrl}"
    }
}
