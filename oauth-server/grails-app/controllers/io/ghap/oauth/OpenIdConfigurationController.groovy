package io.ghap.oauth

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import grails.converters.JSON

class OpenIdConfigurationController {

    def grailsApplication
    def keyManagerService

    def index() {
        def map = [issuer: "${grailsApplication.config.grails.serverURL}/oauth",
                   authorization_endpoint: "${grailsApplication.config.grails.serverURL}/oauth/authorize",
                   token_endpoint: "${grailsApplication.config.grails.serverURL}/oauth/token",
                   token_endpoint_auth_methods_supported: ["client_secret_basic"],
                   userinfo_endpoint: "${grailsApplication.config.grails.serverURL}/oauth/tokeninfo",
                   jwks_uri : "${grailsApplication.config.grails.serverURL}/oauth/jwks_uri",
                   response_types_supported: [
                           "token id_token",
                           "token"
                   ],
                   subject_types_supported:
                           ["public"],
                   id_token_signing_alg_values_supported: ["RS256"],
        ]
        render map as JSON
    }

    def jwks() {
        def key = keyManagerService.keystore.getKey(grailsApplication.config.ghap.keystore.key, grailsApplication.config.ghap.keystore.password.toCharArray());
        def publicKey = keyManagerService.keystore.getCertificate(grailsApplication.config.ghap.keystore.key).publicKey
        def rsaKey = new RSAKey.Builder(publicKey).privateKey(key).keyUse(KeyUse.SIGNATURE).algorithm(JWSAlgorithm.parse("RSA")).keyID(grailsApplication.config.ghap.openid.client.id).build();
        response.contentType = "application/json"
        JWKSet jwkSet = new JWKSet(rsaKey);
        render jwkSet.toJSONObject().toString();
    }

    def webfinger() {
        def map = [subject: params.resource?:"maxim.tulupov@certara.com",
                   links: [[rel : params.rel?:"${grailsApplication.config.grails.serverURL}/oauth", href: "${grailsApplication.config.grails.serverURL}/oauth"]]]
        render map as JSON
    }
}
