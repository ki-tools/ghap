package io.ghap.exception

import org.springframework.security.core.AuthenticationException

/**
 */
class PrivacyPolicyNotAcceptedException extends AuthenticationException {

    PrivacyPolicyNotAcceptedException(String msg, Throwable t) {
        super(msg, t)
    }

    PrivacyPolicyNotAcceptedException(String msg) {
        super(msg)
    }
}
