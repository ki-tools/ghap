import grails.plugin.springsecurity.SpringSecurityUtils
import io.ghap.ldap.GhapAuthenticationProvider
import io.ghap.ldap.GhapBindAuthenticator
import io.ghap.ldap.GhapPasswordComparisonAuthenticator
import io.ghap.oauth.GhapAuthenticationEntryPoint
import io.ghap.oauth.GhapOAuth2RequestValidator
import io.ghap.oauth.GhapRedirectResolver
import io.ghap.oauth.GhapRequestHolderAuthenticationFilter
import io.ghap.oauth.GhapTokenStoreService
import io.ghap.oauth.GhapWrappedAuthorizationEndpoint

// Place your Spring DSL code here
beans = {

    def conf = SpringSecurityUtils.securityConfig
    def oauthConf = loadSecurityConfig()

    ldapUserDetailsMapper(io.ghap.ldap.LdapUserDetailsContextMapper, ref('contextSource')) {

    }

    authenticationProcessingFilter(GhapRequestHolderAuthenticationFilter) {
        authenticationManager = ref('authenticationManager')
        sessionAuthenticationStrategy = ref('sessionAuthenticationStrategy')
        authenticationSuccessHandler = ref('authenticationSuccessHandler')
        authenticationFailureHandler = ref('authenticationFailureHandler')
        rememberMeServices = ref('rememberMeServices')
        authenticationDetailsSource = ref('authenticationDetailsSource')
        requiresAuthenticationRequestMatcher = ref('filterProcessUrlRequestMatcher')
        usernameParameter = conf.apf.usernameParameter // j_username
        passwordParameter = conf.apf.passwordParameter // j_password
        continueChainBeforeSuccessfulAuthentication = conf.apf.continueChainBeforeSuccessfulAuthentication // false
        allowSessionCreation = conf.apf.allowSessionCreation // true
        postOnly = conf.apf.postOnly // true
        storeLastUsername = conf.apf.storeLastUsername // false
    }

    userApprovalHandler(io.ghap.oauth.GhapUserApprovalHandler) {
        approvalParameter = 'test22'
    }

    oauth2AuthorizationEndpoint(GhapWrappedAuthorizationEndpoint) {
        tokenGranter = ref('oauth2AuthorizationEndpointTokenGranter')
        authorizationCodeServices = ref('authorizationCodeServices')
        clientDetailsService = ref('clientDetailsService')
        redirectResolver = ref('redirectResolver')
        userApprovalHandler = ref('userApprovalHandler')
        OAuth2RequestFactory = ref('oauth2RequestFactory')
        OAuth2RequestValidator = ref('oauth2RequestValidator')

        // The URL where the user approves the grant
        userApprovalPage = oauthConf.oauthProvider.userApprovalEndpointUrl

        // The URL the user is directed to in case of an error
        errorPage = oauthConf.oauthProvider.errorEndpointUrl
    }

    ldapAuthProvider(GhapAuthenticationProvider, ref('ldapAuthenticator'), ref('ldapAuthoritiesPopulator')) {
        userDetailsContextMapper = ref('ldapUserDetailsMapper')
        hideUserNotFoundExceptions = conf.ldap.auth.hideUserNotFoundExceptions // true
        useAuthenticationRequestCredentials = conf.ldap.auth.useAuthPassword // true
        authoritiesMapper = ref('ldapAuthoritiesMapper')
    }

    redirectResolver(GhapRedirectResolver)

    oauth2RequestValidator(GhapOAuth2RequestValidator)

    gormTokenStoreService(GhapTokenStoreService) {
        oauth2AuthenticationSerializer = ref('oauth2AuthenticationSerializer')
        authenticationKeyGenerator = ref('authenticationKeyGenerator')
        grailsApplication = ref('grailsApplication')
    }

    ldapAuthenticator(GhapBindAuthenticator, ref('contextSource')) {
        userSearch = ref('ldapUserSearch')
    }

    pwdAuthenticator(GhapPasswordComparisonAuthenticator, ref('contextSource')) {
        userSearch = ref('ldapUserSearch')
    }

    authenticationEntryPoint(GhapAuthenticationEntryPoint, conf.auth.loginFormUrl) { // '/login/auth'
        forceHttps = conf.auth.forceHttps // false
        useForward = conf.auth.useForward // false
        portMapper = ref('portMapper')
        portResolver = ref('portResolver')
    }
}

private static ConfigObject loadSecurityConfig() {
    def conf = SpringSecurityUtils.securityConfig
    if (!conf || !conf.active) {
        return null
    }

    SpringSecurityUtils.loadSecondaryConfig 'DefaultOAuth2ProviderSecurityConfig'
    conf = SpringSecurityUtils.securityConfig

    if (!conf.oauthProvider.active) {
        return null
    }
    return conf
}
