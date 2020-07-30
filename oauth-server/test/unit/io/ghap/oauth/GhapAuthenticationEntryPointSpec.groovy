package io.ghap.oauth

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin

import spock.lang.Specification

@TestMixin(GrailsUnitTestMixin)
class GhapAuthenticationEntryPointSpec extends Specification {

    void "test buildLoginUrl"() {
        setup:
            GhapAuthenticationEntryPoint point = new GhapAuthenticationEntryPoint('login')
            String result
            String url

        when:"call buildLoginUrl with 'qwe'"
            url = 'qwe'
            result = point.buildLoginUrl(url)

        then:"it should return qwe"
            result == url


        when:"call buildLoginUrl with 'notlogin'"
            url = 'notlogin'
            result = point.buildLoginUrl(url)

        then:"it should return qwe"
            result == url


        when:"call buildLoginUrl with 'qwe/login'"
            url = 'qwe'
            result = point.buildLoginUrl(url + '/login')

        then:"it should add / and params string"
            result.startsWith(url + '/oauth')
    }

}
