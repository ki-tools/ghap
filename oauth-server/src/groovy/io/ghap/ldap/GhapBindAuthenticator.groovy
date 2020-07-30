package io.ghap.ldap

import org.springframework.ldap.AuthenticationException
import org.springframework.ldap.core.support.BaseLdapPathContextSource
import org.springframework.security.authentication.CredentialsExpiredException
import org.springframework.security.authentication.LockedException
import org.springframework.security.ldap.authentication.BindAuthenticator

/**
 */
class GhapBindAuthenticator extends BindAuthenticator {

    private static final List<String> MESSAGES = Arrays.asList(
		"NT_STATUS_PASSWORD_MUST_CHANGE",
		"NT_STATUS_ACCOUNT_EXPIRED",
		"NT_STATUS_PASSWORD_EXPIRED",
		"data 773",
		"data 532",
		"data 701"
	);

    private static final List<String> ACCOUNT_LOCKED_MESSAGES = Arrays.asList(
		"NT_STATUS_ACCOUNT_LOCKED_OUT",
		"data 775"
	);

    GhapBindAuthenticator(BaseLdapPathContextSource contextSource) {
        super(contextSource)
    }

    @Override
    protected void handleBindException(String userDn, String username, Throwable cause) {
        super.handleBindException(userDn, username, cause)
        if (isUserMustResetPassword(cause)) {
            throw new CredentialsExpiredException("User must change password", cause);
        }
        if (isAccountLocked(cause)) {
            throw new LockedException("Account Locked");
        }
    }

    public static boolean isUserMustResetPassword(Throwable e) {
        return isMessageInList(MESSAGES, e);
    }

    public static isAccountLocked(Throwable e) {
        return isMessageInList(ACCOUNT_LOCKED_MESSAGES, e);
    }

    public static boolean isMessageInList(List<String> messages, Throwable e) {
        for (String message : messages) {
            if (e.getMessage().contains(message)) {
                return true;
            }
        }
        return false;
    }
}
