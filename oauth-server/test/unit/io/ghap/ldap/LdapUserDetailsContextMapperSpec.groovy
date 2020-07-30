package io.ghap.ldap

import java.util.Collection;

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin

import org.springframework.ldap.core.ContextSource
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.userdetails.UserDetails;

import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
class LdapUserDetailsContextMapperSpec extends Specification {

    void "test isLoginFirstTime"() {
        setup:
            boolean result
            LdapUserDetailsContextMapper mapper = new LdapUserDetailsContextMapper({} as ContextSource)

        when:"login first time"
            result = mapper.isLoginFirstTime("0")

        then:"it should return true"
            result == true

        when:"login second time"
            result = mapper.isLoginFirstTime("1")

        then:"it should return false"
            result == false
    }

    void "test mapUserToContext"() {
        setup:
            Exception ex
            LdapUserDetailsContextMapper mapper = new LdapUserDetailsContextMapper({} as ContextSource)

        when:"call mapUserToContext(UserDetails user, DirContextAdapter ctx)"
            try {
                mapper.mapUserToContext({} as UserDetails, {} as DirContextAdapter)
            } catch (e) {
                ex = e
            }

        then:"it should throw exception"
            ex.getMessage() == "Only retrieving data from LDAP is currently supported"
    }

    void "test mapUserFromContext"() {
        setup:
            UserDetails ud
            def ctx
            String username
            Collection authorities
            LdapUserDetailsContextMapper mapper = new LdapUserDetailsContextMapper({} as ContextSource)

        when:"call mapUserToContext"
            ctx = [
                originalAttrs: [attrs: [
                    userprincipalname:  [values: ['']],
                    mail:               [values: ['']],
                    pwdlastset:         [values: ['']],
                    logoncount:         [values: ['']],
                    useraccountcontrol: [values: ['']],
                    pwd:                [values: ['']],
                ]],
                dn: 'dn'
            ] as DirContextOperations
            username = 'user'
            authorities = ['auth']
            ud = mapper.mapUserFromContext(ctx, username, authorities)

        then:"it should construct user details"
			ud != null
    }

}
