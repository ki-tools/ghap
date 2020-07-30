package io.ghap.ldap

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin

import org.springframework.ldap.core.support.BaseLdapPathContextSource
import org.springframework.security.authentication.encoding.PasswordEncoder

import spock.lang.Specification;

@TestMixin(GrailsUnitTestMixin)
class GhapPasswordComparisonAuthenticatorSpec extends Specification {

    void "test setPasswordAttributeName"() {
        setup:
            Throwable throwed
            GhapPasswordComparisonAuthenticator auth = new GhapPasswordComparisonAuthenticator({} as BaseLdapPathContextSource)
            String passAttrname = 'test'

        when:"password null"
            try {
                auth.setPasswordAttributeName(null)
            } catch (e) {
                throwed = e
            }

        then:"should throw 'passwordAttributeName must not be empty or null' ecxeption"
            throwed.getMessage() == 'passwordAttributeName must not be empty or null'

        when:"password empty"
            try {
                auth.setPasswordAttributeName('')
            } catch (e) {
                throwed = e
            }

        then:"should throw 'passwordAttributeName must not be empty or null' ecxeption"
            throwed.getMessage() == 'passwordAttributeName must not be empty or null'

        when:"password set"
            auth.setPasswordAttributeName(passAttrname)

        then:"should throw 'passwordAttributeName must not be empty or null' ecxeption"
            auth.passwordAttributeName == passAttrname
    }

    void "test setPasswordEncoder"() {
        setup:
            Throwable throwed
            GhapPasswordComparisonAuthenticator auth = new GhapPasswordComparisonAuthenticator({} as BaseLdapPathContextSource)
            PasswordEncoder encoder = {} as PasswordEncoder

        when:"password null"
            try {
                auth.setPasswordEncoder(null)
            } catch (e) {
                throwed = e
            }

        then:"should throw 'passwordEncoder must not be null.' ecxeption"
            throwed.getMessage() == 'passwordEncoder must not be null.'

        when:"password set"
            auth.setPasswordEncoder(encoder)

        then:"should throw 'passwordEncoder must not be null.' ecxeption"
            auth.passwordAttributeName == encoder
    }

}
