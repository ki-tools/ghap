package io.ghap.ldap

import org.springframework.security.core.SpringSecurityCoreVersion

/**
 * Created by Juan Vazquez.
 * URL: http://javazquez.com/juan
 * Code is provide for educational purposes. Any use in a production system is at your own risk.
 */
import org.springframework.security.core.userdetails.User

class LdapUserDetails extends User {

    private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;

    // extra instance variables final String fullname final String email final String title
    String fullname
    String email
    String dn;
    boolean passwordExpiredFlag;
    boolean firstTimeLogon;
    String password;

    LdapUserDetails(String username, String password, boolean enabled, boolean accountNonExpired,
                  boolean credentialsNonExpired, boolean accountNonLocked, Collection authorities,
                  String fullname, String email, String dn) {

        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities)
        this.fullname = fullname
        this.email = email
        this.dn = dn;
        this.password = password
    }

    @Override
    void eraseCredentials() {
        //do nothing
    }

    String getPassword() {
        return password
    }

    void setPassword(String password) {
        this.password = password
    }
}
