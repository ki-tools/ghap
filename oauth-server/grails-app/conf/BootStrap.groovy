import io.ghap.oauth.Client

class BootStrap {

    def ldapAuthoritiesPopulator
    def grailsApplication

    def init = { servletContext ->

        ldapAuthoritiesPopulator.setConvertToUpperCase(false)

        if (Client.count() <= 0) {
            new Client(
                    clientId: grailsApplication.config.ghap.openid.client.id,
                    clientSecret: grailsApplication.config.ghap.client.secret,
                    authorizedGrantTypes: ['authorization_code', 'refresh_token', 'implicit', 'password', 'client_credentials'],
                    authorities: ['GHAP_Administrator'],
                    scopes: ['read', 'write'],
                    skipConsent: true
            ).save(flush: true)
        }
    }
    def destroy = {
    }
}
