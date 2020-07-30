package io.ghap.oauth

import grails.converters.JSON

import javax.servlet.http.HttpServletResponse

class HealthController {

	def checkHealthService

	def index() {
		def map = checkHealthService.checkHealth();
		if (!checkHealthService.isCheckSuccess(map)) {
			response.status = HttpServletResponse.SC_SERVICE_UNAVAILABLE
		}
		render map as JSON
	}
}
