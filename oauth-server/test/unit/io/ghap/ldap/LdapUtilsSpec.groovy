package io.ghap.ldap

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import spock.lang.Specification
import javax.naming.directory.Attribute

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
class LdapUtilsSpec extends Specification {

    void "test isPasswordNeverExpires"() {
        setup:
            boolean result

        when:"call isPasswordNeverExpires with null"
            result = LdapUtils.isPasswordNeverExpires(null)

        then:"it should return false"
            result == false

        when:"call isPasswordNeverExpires with UF_DONT_EXPIRE_PASSWD"
            result = LdapUtils.isPasswordNeverExpires(LdapUtils.UF_DONT_EXPIRE_PASSWD)

        then:"it should return true"
            result == true
    }

    void "test isUserMustResetPwd"() {
        setup:
            boolean result

        when:"call isUserMustResetPwd with null"
            result = LdapUtils.isUserMustResetPwd(null)

        then:"it should return false"
            result == false

		when:"call isUserMustResetPwd with 8388608"
            result = LdapUtils.isUserMustResetPwd(8388608)

        then:"it should return true"
            result == true
    }

    void "test getPasswordMaxAgeSeconds"() {
        setup:
			Attribute attr
            Integer result

        when:"call getPasswordMaxAgeSeconds with null"
            result = LdapUtils.getPasswordMaxAgeSeconds(null)

        then:"it should return null"
            result == null

        when:"call getPasswordMaxAgeSeconds with attr"
			attr = [
				get: {
					return 10000000
				}
			] as Attribute
            result = LdapUtils.getPasswordMaxAgeSeconds(attr)

        then:"it should return 1"
            result == 1

        when:"call getPasswordMaxAgeSeconds with null in attr"
			attr = [
				get: {
					return null
				}
			] as Attribute
            result = LdapUtils.getPasswordMaxAgeSeconds(attr)

        then:"it should return null"
            result == null
    }

	void "test fromLdapDate"() {
		when:"call fromLdapDate with null in attr"
			def d = (11644473600000L + 24 * 60 * 60 * 1000) * 10000
			Date result = LdapUtils.fromLdapDate(d.toString())

		then:"it should return null"
			result == new Date(0)
	}

}
