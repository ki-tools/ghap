package io.ghap.ldap

import org.apache.commons.lang.time.DateUtils
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.ldap.core.ContextSource

/**
 * Created by Juan Vazquez.
 * URL: http://javazquez.com/juan
 * Code is provide for educational purposes. Any use in a production system is at your own risk.
 */
import org.springframework.ldap.core.DirContextAdapter
import org.springframework.ldap.core.DirContextOperations
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.ldap.SpringSecurityLdapTemplate
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper

import javax.naming.directory.SearchControls

class LdapUserDetailsContextMapper implements UserDetailsContextMapper {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    GrailsApplication grailsApplication;

    ContextSource contextSource;
    private final SpringSecurityLdapTemplate ldapTemplate;
    private final SearchControls searchControls = new SearchControls();

    LdapUserDetailsContextMapper(ContextSource contextSource) {
        this.contextSource = contextSource
        ldapTemplate = new SpringSecurityLdapTemplate(contextSource);
        ldapTemplate.setSearchControls(searchControls);
    }

    UserDetails mapUserFromContext(DirContextOperations ctx, String username, Collection authorities) {

        //Grab the specific Active Directory information you want

        def object = ctx.originalAttrs.attrs['userprincipalname']
        String fullname = (object && object.values) ? object.values[0] : 'Unknown';

        def indexOf = fullname.indexOf('@')
        if (indexOf > 0) {
            fullname = fullname.substring(0, indexOf);
        }

        def attr = ctx.originalAttrs.attrs['mail']
        String email = (attr && attr?.values) ? attr?.values[0]?.toString().toLowerCase() : null;

        def passwordMaxAge = getPasswordMaxAge();

        def pwdLastSetAttr = ctx.originalAttrs.attrs['pwdlastset']?.values[0]
        def logonCountAttr = ctx.originalAttrs.attrs['logoncount']?.values[0]
        Date pwdLastSet = LdapUtils.fromLdapDate(pwdLastSetAttr);
        Integer accountControlAttributes = Integer.valueOf(ctx.originalAttrs.attrs['useraccountcontrol']?.values[0]?.toString())
        log.debug("name = ${fullname} pwd last set = ${pwdLastSet}, account control attribute = ${accountControlAttributes}, lastLogon = ${logonCountAttr}, pwdLastSetAttr = ${pwdLastSetAttr}")

        def userDetails = new LdapUserDetails(username, ctx.originalAttrs.attrs['pwd'].values[0], true, true, true, true,
                authorities, fullname, email, ctx.dn.toString())
        userDetails.passwordExpiredFlag = !(LdapUtils.isPasswordNeverExpires(accountControlAttributes) || pwdLastSetAttr == '-1') && (pwdLastSetAttr == '0' || (passwordMaxAge != null && pwdLastSet != null && DateUtils.addSeconds(pwdLastSet, passwordMaxAge).before(new Date())) || LdapUtils.isUserMustResetPwd(accountControlAttributes));
        //works only for samba. Need to find workaround for AD
        userDetails.firstTimeLogon = isLoginFirstTime(logonCountAttr);
        log.error("name = ${fullname} use password expired flag = ${userDetails.passwordExpiredFlag}");
        return userDetails
    }

    void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
        throw new IllegalStateException("Only retrieving data from LDAP is currently supported")
    }

    private Integer getPasswordMaxAge() {
        def lookup = ldapTemplate.lookup(grailsApplication.config.grails.plugin.springsecurity.ldap.search.base) as DirContextAdapter
        return LdapUtils.getPasswordMaxAgeSeconds(lookup?.attributes?.get('maxPwdAge'))
    }

    private boolean isLoginFirstTime(def logonCount) {
        return "0".equals(logonCount);
    }
}
