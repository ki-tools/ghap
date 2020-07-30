package io.ghap.ldap

import org.springframework.ldap.core.support.BaseLdapPathContextSource;

import java.util.List;

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
class GhapBindAuthenticatorSpec extends Specification {

    void "test isMessageInList"() {
        setup:
            List<String> messages = ['a', 'b']
            Throwable e
            boolean result

        when:"Messages array contains string with exception"
            e = new Exception('a')
            result = GhapBindAuthenticator.isMessageInList(messages, e)

        then:"The result is true"
            result == true

        when:"Messages array doesn't contain string with exception"
            e = new Exception('c')
            result = GhapBindAuthenticator.isMessageInList(messages, e)

        then:"The result is false"
            result == false
    }

//	void "test isUserMustResetPassword"() {
//		setup:
//			int calls = 0
//			List<String> passedMessages
//			Throwable passedE
//			Throwable exception
//			GhapBindAuthenticator.metaClass.'static'.isMessageInList = { List<String> messages, Throwable e ->
//				passedMessages = messages
//				passedE = e
//				calls++
//			}
//
//		when:
//			exception = new Exception('a')
//			GhapBindAuthenticator.isUserMustResetPassword(exception)
//
//		then:"isMessageInList should be called with right params"
//			calls == 1
//			passedMessages == GhapBindAuthenticator.MESSAGES
//			passedE == exception
//	}
//
//	void "test isAccountLocked"() {
//		setup:
//			int calls = 0
//			List<String> passedMessages
//			Throwable passedE
//			Throwable exception
//			GhapBindAuthenticator.metaClass.'static'.isMessageInList = { List<String> messages, Throwable e ->
//				passedMessages = messages
//				passedE = e
//				calls++
//			}
//
//		when:
//			exception = new Exception('a')
//			GhapBindAuthenticator.isAccountLocked(exception)
//
//		then:"isMessageInList should be called with right params"
//			calls == 1
//			passedMessages == GhapBindAuthenticator.ACCOUNT_LOCKED_MESSAGES
//			passedE == exception
//	}

    void "test handleBindException"() {
        setup:
            Throwable throwed
            GhapBindAuthenticator gba = new GhapBindAuthenticator({} as BaseLdapPathContextSource)

        when:"'NT_STATUS_PASSWORD_MUST_CHANGE' exception appears"
            try {
                gba.handleBindException('a', 'a', new Exception('NT_STATUS_PASSWORD_MUST_CHANGE'))
            } catch (e) {
                throwed = e
            }

        then:"should throw 'User must change password' ecxeption"
            throwed.getMessage() == 'User must change password'

        when:"'NT_STATUS_ACCOUNT_LOCKED_OUT' exception appears"
            try {
                gba.handleBindException('b', 'b', new Exception('NT_STATUS_ACCOUNT_LOCKED_OUT'))
            } catch (e) {
                throwed = e
            }

        then:"should throw 'Account Locked' ecxeption"
            throwed.getMessage() == 'Account Locked'

        when:
            gba.handleBindException('c', 'c', new Exception('a'))
            throwed = null

        then:"should throw nothing"
            throwed == null
    }

}
