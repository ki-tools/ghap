package io.ghap.oauth

import grails.artefact.Artefact
import io.ghap.entity.Principal
import io.ghap.entity.VerifyTokenResponse
import io.ghap.ldap.LdapUserDetails
import org.springframework.security.oauth2.common.OAuth2AccessToken
import org.springframework.security.oauth2.provider.OAuth2Authentication

import javax.servlet.http.HttpServletResponse

@Artefact('Controller')
class TokenController {

    static responseFormats = ['json', 'xml']

    static allowedMethods = [tokenInfo: 'GET', updateToken: 'GET']

    def gormTokenStoreService
    def grailsApplication

    def tokenInfo() {
        def token = params.access_token;
        if (!token) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND)
            return
        }
        OAuth2AccessToken token1 = gormTokenStoreService.readAccessToken(token);
        if (!token1) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND)
            return
        }
        if (token1.expired) {
            try {
                gormTokenStoreService.removeAccessToken(token);
            } catch (Throwable e) {
                //do nothing
            }
            response.sendError(HttpServletResponse.SC_GONE)
            return
        }
        OAuth2Authentication authentication = gormTokenStoreService.readAuthentication(token);
        if (!authentication) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND)
            return
        }

        LdapUserDetails pr = authentication.principal

        def roles = new HashSet<String>(authentication?.authorities?.collect{it?.authority.replace("ROLE_", "").replace("_", " ")})
        Principal principal = new Principal(name: pr.fullname, email: pr.email, password: pr.password, dn: pr.dn, passwordExpired: pr?.passwordExpiredFlag, firstTimeLogon: pr?.firstTimeLogon,
                groups: roles, adminPrincipal: (roles.contains('Administrators') || roles.contains('GHAP Administrator') || roles.contains('BMGF Administrator')))
        VerifyTokenResponse resp = new VerifyTokenResponse(audience: grailsApplication.config.ghap.openid.client.id, scopes: roles, principal: principal, expires_in: token1.expiresIn)
        AccessToken.executeUpdate("update AccessToken e set e.lastUsed = :lastUsed where e.value = :token", [token: token, lastUsed: new Date()]);
        respond resp
    }

    def revoke() {
        session.invalidate();
        def header = request.getHeader("Authorization");
        if (!header) {
            render status: HttpServletResponse.SC_OK
            return
        }
        def token = header - 'Bearer '
        gormTokenStoreService.removeAccessToken(token)
        render status: HttpServletResponse.SC_OK
    }

    def updateToken() {
        if (!params.dn && !params.password) {
            render status: HttpServletResponse.SC_OK
            return
        }

        def token = params.access_token;
        if (!token) {
            render status: HttpServletResponse.SC_OK
            return
        }
        gormTokenStoreService.updateAuthentication(token, params.dn, params.password)
        render status: HttpServletResponse.SC_OK
    }
}
