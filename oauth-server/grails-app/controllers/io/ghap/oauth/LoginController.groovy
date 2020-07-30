package io.ghap.oauth

import grails.converters.JSON
import grails.plugin.springsecurity.SpringSecurityUtils
import io.ghap.exception.PrivacyPolicyNotAcceptedException
import org.springframework.security.authentication.AccountExpiredException
import org.springframework.security.authentication.CredentialsExpiredException
import org.springframework.security.authentication.DisabledException
import org.springframework.security.authentication.LockedException
import org.springframework.security.web.WebAttributes
import groovyx.net.http.RESTClient

class LoginController extends grails.plugin.springsecurity.LoginController {


	def auth() {

		def conf = SpringSecurityUtils.securityConfig

		if (springSecurityService.isLoggedIn()) {
			redirect uri: conf.successHandler.defaultTargetUrl
			return
		}

        def banners = []

		try {
			def client = new RESTClient(grailsApplication.config.ghap.banner.service.url)
            client.ignoreSSLIssues();

			def resp = client.get(path: 'current')

			if (response.status == 200)
                banners = resp.data
		} catch (Exception ex) {
		}

	
		String postUrl = request.contextPath + conf.apf.filterProcessesUrl
		render view: 'auth', model: [postUrl: postUrl,
		                             rememberMeParameter: conf.rememberMe.parameter,
		                             usernameParameter: conf.apf.usernameParameter,
		                             passwordParameter: conf.apf.passwordParameter,
		                             gspLayout: conf.gsp.layoutAuth,
                                     banners : banners]
	}
	
    def authfail() {
        String msg = ''
        def exception = session[WebAttributes.AUTHENTICATION_EXCEPTION]
        if (exception) {
            if (exception instanceof AccountExpiredException) {
                msg = 'Account expired'
            } else if (exception instanceof CredentialsExpiredException) {
                msg = 'Password expired'
            } else if (exception instanceof DisabledException) {
                msg = 'Account disabled'
            } else if (exception instanceof LockedException) {
                msg = 'Your account has been locked. Please contact GHAP support at 314-951-3090 to verify your identity and have your account re-enabled.'
            } else if (exception instanceof PrivacyPolicyNotAcceptedException) {
                msg = 'You must accept privacy policy prior login.'
            } else {
                msg = 'Log in failed. Please try again.'
            }
        }

        if (springSecurityService.isAjax(request)) {
            render([error: msg] as JSON)
        } else {
            flash.loginError = msg
            redirect action: 'auth', params: params
        }
    }
}
