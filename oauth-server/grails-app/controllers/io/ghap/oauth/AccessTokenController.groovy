package io.ghap.oauth

import grails.plugin.springsecurity.annotation.Secured

import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional

@Transactional(readOnly = true)
@Secured(['ROLE_Administrators','ROLE_GHAP_Administrator','ROLE_BMGF_Administrator'])
class AccessTokenController {

    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond AccessToken.list(params), model:[accessTokenInstanceCount: AccessToken.count()]
    }

    @Transactional
    def deleteById(long id) {
        delete(AccessToken.get(id));
        redirect action:"index", method:"GET"
    }

    @Transactional
    def delete(AccessToken accessTokenInstance) {

        if (accessTokenInstance == null) {
            notFound()
            return
        }

        accessTokenInstance.delete flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.deleted.message', args: [message(code: 'AccessToken.label', default: 'AccessToken'), accessTokenInstance.id])
                redirect action:"index", method:"GET"
            }
            '*'{ render status: NO_CONTENT }
        }
    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'accessToken.label', default: 'AccessToken'), params.id])
                redirect action: "index", method: "GET"
            }
            '*'{ render status: NOT_FOUND }
        }
    }
}
