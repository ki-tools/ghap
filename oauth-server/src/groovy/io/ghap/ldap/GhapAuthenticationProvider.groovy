package io.ghap.ldap

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.ldap.core.DirContextOperations
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.CredentialsExpiredException
import org.springframework.security.authentication.LockedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider
import org.springframework.security.ldap.authentication.LdapAuthenticator
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator
import org.springframework.util.Assert
import org.springframework.util.StringUtils

/**
 */
class GhapAuthenticationProvider extends LdapAuthenticationProvider {


    @Autowired
    GhapPasswordComparisonAuthenticator passwordComparisonAuthenticator;

    GhapAuthenticationProvider(LdapAuthenticator authenticator, LdapAuthoritiesPopulator authoritiesPopulator) {
        super(authenticator, authoritiesPopulator)
    }

    GhapAuthenticationProvider(LdapAuthenticator authenticator) {
        super(authenticator)
    }

    @Override
    Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Assert.isInstanceOf(UsernamePasswordAuthenticationToken.class, authentication,
                messages.getMessage("LdapAuthenticationProvider.onlySupports",
                        "Only UsernamePasswordAuthenticationToken is supported"));

        final UsernamePasswordAuthenticationToken userToken = (UsernamePasswordAuthenticationToken) authentication;

        String username = userToken.getName();
        String password = (String) authentication.getCredentials();

        if (logger.isDebugEnabled()) {
            logger.debug("Processing authentication request for user: " + username);
        }

        if (!StringUtils.hasLength(username)) {
            throw new BadCredentialsException(messages.getMessage("LdapAuthenticationProvider.emptyUsername",
                    "Empty Username"));
        }

        Assert.notNull(password, "Null password was supplied in authentication token");

        DirContextOperations userData
        try {
            userData = doAuthentication(userToken);
            updateLogonCount(userData)
        } catch (CredentialsExpiredException e) {
            userData = passwordComparisonAuthenticator.findUser(authentication);
            updateLogonCount(userData)
        } catch (Throwable e) {
            logger.error("error during login", e);
            if (GhapBindAuthenticator.isUserMustResetPassword(e)) {
                userData = passwordComparisonAuthenticator.findUser(authentication);
                updateLogonCount(userData)
            } else if (GhapBindAuthenticator.isAccountLocked(e)) {
                throw new LockedException("Account Locked");
            } else {
                throw e;
            }
        }
        userData.addAttributeValue("pwd", password)

        UserDetails user = userDetailsContextMapper.mapUserFromContext(userData, authentication.getName(),
                loadUserAuthorities(userData, authentication.getName(), (String)authentication.getCredentials()));

        return createSuccessfulAuthentication(userToken, user);
    }

    protected void updateLogonCount(DirContextOperations userData) {
        try {
            passwordComparisonAuthenticator.updateLogonCount(userData)
        } catch (Throwable e) {
            logger.error("error update logoncount attr. This is impossible for AD", e);
        }
    }
}
