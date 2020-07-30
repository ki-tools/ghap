import grails.util.Holders

class UrlMappings {

	static mappings = {
        "/$controller/$action?/$id?(.$format)?"{
            constraints {
                // apply constraints here
            }
        }

        "/"(uri: "/oauth/authorize.dispatch?client_id=projectservice&response_type=token&redirect_uri=${Holders.grailsApplication.config.grails.plugin.springsecurity.successHandler.defaultTargetUrl}")
        "500"(view:'/error')
        "/oauth/tokeninfo"(controller: 'token', action: 'tokenInfo')
        "/oauth/tokeninfo/update"(controller: 'token', action: 'updateToken')
        "/oauth/revoke"(controller: 'token', action: 'revoke')
        "/oauth/.well-known/openid-configuration"(controller: 'openIdConfiguration', action: 'index')
        "/oauth/.well-known/webfinger"(controller: 'openIdConfiguration', action: 'webfinger')
        "/oauth/jwks_uri"(controller: 'openIdConfiguration', action: 'jwks')
	}
}
