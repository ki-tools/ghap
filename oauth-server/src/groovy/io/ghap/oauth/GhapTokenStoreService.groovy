package io.ghap.oauth

import grails.plugin.springsecurity.SpringSecurityUtils
import grails.plugin.springsecurity.oauthprovider.GormTokenStoreService
import grails.transaction.Transactional
import grails.util.Holders
import io.ghap.ldap.LdapUserDetails
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.impl.DefaultClaims
import org.apache.commons.lang.time.DateUtils
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken
import org.springframework.security.oauth2.common.OAuth2AccessToken
import org.springframework.security.oauth2.provider.OAuth2Authentication

import java.security.KeyStore

/**
 */
class GhapTokenStoreService extends GormTokenStoreService{

    private final Logger logger = LoggerFactory.getLogger(getClass())

    @Autowired
    private KeyManagerService keyManagerService;

    @Autowired
    private GrailsApplication grailsApplication;

    @Override
    OAuth2AccessToken readAccessToken(String tokenValue) {
        def (accessTokenLookup, GormAccessToken) = getAccessTokenLookupAndClass()

        def valuePropertyName = accessTokenLookup.valuePropertyName
        def gormAccessToken = GormAccessToken.findWhere((valuePropertyName): tokenValue)

        if (!gormAccessToken) {
            logger.debug("Failed to find access token")
            return null
        }
        createOAuth2AccessToken(gormAccessToken)
    }

    @Override
    @Transactional
    OAuth2Authentication readAuthentication(String token) {
        try {
            return super.readAuthentication(token)
        } catch (Throwable e) {
            logger.error("error deserialize class", e);
            try {
                removeAccessToken(token)
            } catch (Throwable ex) {
                logger.error("error delete access token", ex);
            }
        }
        return null;
    }

    @Override
    @Transactional
    void removeAccessToken(String tokenValue) {
        super.removeAccessToken(tokenValue)
    }

    @Transactional
    void updateAuthentication(String accessToken, String dn, String password) {
        def value = AccessToken.findByValue(accessToken);
        if (!value) {
            return
        }
        OAuth2Authentication authentication = getOauth2AuthenticationSerializer().deserialize(value?.authentication)
        LdapUserDetails ldapUserDetails = authentication.principal
        if (dn) {
            ldapUserDetails.dn = dn
        }
        if (password) {
            ldapUserDetails.password = password
        }
        value.authentication = getOauth2AuthenticationSerializer().serialize(authentication)
        value.save(flush: true, failOnError: true)
    }

    public String getTokenFromAuthentication(Authentication authentication) {
        return authenticationKeyGenerator.extractKey(authentication)
    }

    @Override
    @Transactional
    void storeAccessToken(OAuth2AccessToken token, OAuth2Authentication authentication) {
        def (accessTokenLookup, GormAccessToken) = getAccessTokenLookupAndClass()

        def authenticationKeyPropertyName = accessTokenLookup.authenticationKeyPropertyName
        def authenticationPropertyName = accessTokenLookup.authenticationPropertyName
        def usernamePropertyName = accessTokenLookup.usernamePropertyName
        def clientIdPropertyName = accessTokenLookup.clientIdPropertyName
        def valuePropertyName = accessTokenLookup.valuePropertyName
        def tokenTypePropertyName = accessTokenLookup.tokenTypePropertyName
        def expirationPropertyName = accessTokenLookup.expirationPropertyName
        def refreshTokenPropertyName = accessTokenLookup.refreshTokenPropertyName
        def scopePropertyName = accessTokenLookup.scopePropertyName

        logger.info("creating access token")
        AccessToken gormAccessToken = GormAccessToken.findWhere((valuePropertyName): token.value) ?: GormAccessToken.newInstance()

        gormAccessToken."$authenticationKeyPropertyName" = authenticationKeyGenerator.extractKey(authentication)
        gormAccessToken."$authenticationPropertyName" = oauth2AuthenticationSerializer.serialize(authentication)
        gormAccessToken."$usernamePropertyName" = authentication.isClientOnly() ? null : authentication.name
        gormAccessToken."$clientIdPropertyName" = authentication.getOAuth2Request().clientId
        gormAccessToken."$valuePropertyName" = token.value
        gormAccessToken."$tokenTypePropertyName" = token.tokenType
        gormAccessToken."$expirationPropertyName" = token.expiration
        gormAccessToken."$refreshTokenPropertyName" = token.refreshToken?.value
        gormAccessToken."$scopePropertyName" = token.scope
        gormAccessToken.nonce = UUID.randomUUID().toString();
        gormAccessToken.subject = authentication.name
        gormAccessToken.idToken = signToken(gormAccessToken.nonce, gormAccessToken.subject, getMinDate(gormAccessToken))

        HashMap<String, Object> additionalInfo = createAdditionalParameters(gormAccessToken)
        ((DefaultOAuth2AccessToken) token).setAdditionalInformation(additionalInfo);
        gormAccessToken.save()
        logger.info("created token = ${gormAccessToken}")
    }

    @Override
    OAuth2AccessToken getAccessToken(OAuth2Authentication authentication) {
        logger.info("start getting access token")
        def authenticationKey = authenticationKeyGenerator.extractKey(authentication)
        def (accessTokenLookup, GormAccessToken) = getAccessTokenLookupAndClass()

        def authenticationKeyPropertyName = accessTokenLookup.authenticationKeyPropertyName
        def gormAccessToken = GormAccessToken.findWhere((authenticationKeyPropertyName): authenticationKey)

        if (!gormAccessToken) {
            logger.debug("Failed to find access token for authentication [$authentication]")
            return null
        }

        def accessToken = createOAuth2AccessToken(gormAccessToken)
        def tokenValue = accessToken.value

        if(authenticationKey != getAuthenticationKeyFromAccessToken(tokenValue)) {
            logger.info("Authentication [$authentication] is not associated with retrieved access token")
            removeAccessToken(tokenValue)
            storeAccessToken(accessToken, authentication)
            gormAccessToken = GormAccessToken.findWhere(value: tokenValue)
            accessToken = createOAuth2AccessToken(gormAccessToken)
        }
        return accessToken
    }

    private OAuth2AccessToken createOAuth2AccessToken(gormAccessToken) {
        logger.info("start create OAuth2AccessToken for " + gormAccessToken.value)
        def accessTokenLookup = getAccessTokenLookupAndClass()[0] as Map

        def valuePropertyName = accessTokenLookup.valuePropertyName
        def tokenTypePropertyName = accessTokenLookup.tokenTypePropertyName
        def expirationPropertyName = accessTokenLookup.expirationPropertyName
        def refreshTokenPropertyName = accessTokenLookup.refreshTokenPropertyName
        def scopePropertyName = accessTokenLookup.scopePropertyName

        def token = new DefaultOAuth2AccessToken(gormAccessToken."$valuePropertyName")
        token.refreshToken = createRefreshTokenForAccessToken(gormAccessToken, refreshTokenPropertyName)
        token.tokenType = gormAccessToken."$tokenTypePropertyName"
        token.expiration = getMinDate(gormAccessToken);
        token.scope = gormAccessToken."$scopePropertyName"
        HashMap<String, Object> additionalInfo = createAdditionalParameters(gormAccessToken)
        token.setAdditionalInformation(additionalInfo);
        return token
    }

    public static Date getMinDate(AccessToken accessToken) {
        if (!accessToken?.lastUsed || !accessToken.expiration) {
            if (accessToken?.lastUsed) {
                return DateUtils.addMinutes(accessToken?.lastUsed, Holders.grailsApplication.config.ghap.token.expire.minutes)
            }
            return accessToken?.expiration
        }
        def date = DateUtils.addMinutes(accessToken?.lastUsed, Holders.grailsApplication.config.ghap.token.expire.minutes);
        if (date.before(accessToken?.expiration)) {
            return date
        } else {
            return accessToken?.expiration
        }
    }

    private def getAccessTokenLookupAndClass() {
        def accessTokenLookup = SpringSecurityUtils.securityConfig.oauthProvider.accessTokenLookup
        def GormAccessToken = getAccessTokenClass(accessTokenLookup.className)
        [accessTokenLookup, GormAccessToken]
    }

    private Class getAccessTokenClass(String accessTokenClassName) {
        getTokenClass('access', accessTokenClassName)
    }

    private Class getTokenClass(String tokenType, String className) {
        def tokenClass = className ? grailsApplication.getDomainClass(className) : null
        if (!tokenClass) {
            def message = String.format(INVALID_DOMAIN_CLASS_FORMAT, tokenType, className)
            throw new IllegalArgumentException(message)
        }
        return tokenClass.clazz
    }

    private def signToken(String nonce, String subject, Date expDate) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.setTimeInMillis(System.currentTimeMillis());
        Date issDate = calendar.getTime();
        def claims = new DefaultClaims([nonce: nonce]).setIssuer(grailsApplication.config.grails.serverURL + "/oauth")
                .setAudience(grailsApplication.config.ghap.openid.client.id).setExpiration(expDate).setIssuedAt(issDate).setSubject(subject)
        Jwts.builder()
                .setHeaderParams([typ: "JWT", alg: "RS256", kid: grailsApplication.config.ghap.openid.client.id])
                .setClaims(claims)
                .signWith(SignatureAlgorithm.RS256, keyManagerService.keystore.getKey(grailsApplication.config.ghap.keystore.key, grailsApplication.config.ghap.keystore.password.toCharArray())).compact();
    }

    private HashMap<String, Object> createAdditionalParameters(AccessToken gormAccessToken) {
        Map<String, Object> additionalInfo = new HashMap<>();
        logger.info("id_token = " + gormAccessToken.idToken)
        additionalInfo.put("id_token", gormAccessToken.idToken);
        additionalInfo.put("nonce", gormAccessToken.nonce);
        additionalInfo.put("subject", gormAccessToken.subject);
        additionalInfo
    }
}
