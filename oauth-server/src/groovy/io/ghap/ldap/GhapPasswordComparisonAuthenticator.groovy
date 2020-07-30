package io.ghap.ldap

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.springframework.ldap.NameNotFoundException
import org.springframework.ldap.core.DirContextOperations
import org.springframework.ldap.core.support.BaseLdapPathContextSource
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.authentication.encoding.LdapShaPasswordEncoder
import org.springframework.security.authentication.encoding.PasswordEncoder
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.codec.Utf8
import org.springframework.security.ldap.SpringSecurityLdapTemplate
import org.springframework.security.ldap.authentication.AbstractLdapAuthenticator
import org.springframework.util.Assert

import javax.naming.directory.BasicAttribute
import javax.naming.directory.DirContext
import javax.naming.directory.ModificationItem
import java.nio.charset.StandardCharsets

/**
 */
class GhapPasswordComparisonAuthenticator extends AbstractLdapAuthenticator {

    private static final Log logger = LogFactory.getLog(GhapPasswordComparisonAuthenticator.class);

    public GhapPasswordComparisonAuthenticator(BaseLdapPathContextSource contextSource) {
        super(contextSource);
    }

    public DirContextOperations authenticate(final Authentication authentication) {
        Assert.isInstanceOf(UsernamePasswordAuthenticationToken.class, authentication,
                "Can only process UsernamePasswordAuthenticationToken objects");
        // locate the user and check the password

        DirContextOperations user = null;
        String username = authentication.getName();
        String password = (String)authentication.getCredentials();

        SpringSecurityLdapTemplate ldapTemplate = new SpringSecurityLdapTemplate(getContextSource());

        for (String userDn : getUserDns(username)) {
            try {
                user = ldapTemplate.retrieveEntry(userDn, getUserAttributes());
            } catch (NameNotFoundException ignore) {
            }
            if (user != null) {
                break;
            }
        }

        if (user == null && getUserSearch() != null) {
            user = getUserSearch().searchForUser(username);
        }

        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username, username);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Performing LDAP compare of password attribute '" + passwordAttributeName + "' for user '" +
                    user.getDn() +"'");
        }

        String encodedPassword = passwordEncoder.encodePassword(password, null);
        password = "\"" + password + "\"";
        byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_16LE);

        if (!ldapTemplate.compare(user.getDn().toString(), passwordAttributeName, passwordBytes)) {
            logger.error("password mismatch for " + user.getDn())
            throw new BadCredentialsException(messages.getMessage("PasswordComparisonAuthenticator.badCredentials",
                    "Bad credentials"));
        }

        return user;
    }

    public void setPasswordAttributeName(String passwordAttribute) {
        Assert.hasLength(passwordAttribute, "passwordAttributeName must not be empty or null");
        this.passwordAttributeName = passwordAttribute;
    }

    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        Assert.notNull(passwordEncoder, "passwordEncoder must not be null.");
        this.passwordEncoder = passwordEncoder;
    }

    public DirContextOperations findUser(final Authentication authentication) {
        Assert.isInstanceOf(UsernamePasswordAuthenticationToken.class, authentication,
                "Can only process UsernamePasswordAuthenticationToken objects");
        // locate the user and check the password

        DirContextOperations user = null;
        String username = authentication.getName();
        String password = (String)authentication.getCredentials();

        SpringSecurityLdapTemplate ldapTemplate = new SpringSecurityLdapTemplate(getContextSource());

        for (String userDn : getUserDns(username)) {
            try {
                user = ldapTemplate.retrieveEntry(userDn, getUserAttributes());
            } catch (NameNotFoundException ignore) {
            }
            if (user != null) {
                break;
            }
        }

        if (user == null && getUserSearch() != null) {
            user = getUserSearch().searchForUser(username);
        }

        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username, username);
        }
        return user;
    }

    public void updateLogonCount(DirContextOperations user) {
        logger.debug("start update logon count for " + user.getDn())
        SpringSecurityLdapTemplate ldapTemplate = new SpringSecurityLdapTemplate(getContextSource());
        Integer logonCountAttribute = 1;
        def logonCountAttr = user.originalAttrs.attrs['logoncount']?.values[0]
        logger.debug("logon count attr from user = ${logonCountAttr}")
        if (logonCountAttr) {
            try {
                int value = Integer.valueOf(logonCountAttr)
                if (value > 0) {
                    logonCountAttribute++
                }
            } catch (NumberFormatException e) {
                //do nothing
            }
        }
        logger.debug("logon count = " + logonCountAttribute +  " for " + user.getDn())
        ModificationItem item = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("logoncount", Integer.toString(logonCountAttribute)))
        ldapTemplate.modifyAttributes(user.getDn(), [item].toArray() as ModificationItem[]);
    }
}
