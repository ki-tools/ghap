package io.ghap.auth;

import com.google.inject.Injector;
import com.netflix.config.ConfigurationManager;
import com.netflix.governator.annotations.Configuration;
import io.ghap.ldap.LdapUtils;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.template.exception.LdapRuntimeException;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

// should not be Singleton since configuration will not be injected
public class LdapConfiguration {

    @Inject
    Injector injector;

    @Configuration("ldap.config")
    Map<String, ?> ldapConfig;

    public Map<String, ?> getLdapConfig(){
        return (Map<String, ?>) ConfigurationManager.getConfigInstance().getProperty("ldap.config");
    }

    public String getLdapHost() {
        return String.valueOf(ldapConfig.get("host"));
    }

    public int getLdapPort() {
        try {
            return Integer.parseInt(String.valueOf(ldapConfig.get("port")));
        } catch(NumberFormatException e){
            throw new NumberFormatException("LDAP port should be integer value");
        }
    }

    public boolean isUseSsl(){
        String useSsl = String.valueOf(ldapConfig.get("ssl"));
        return "true".equalsIgnoreCase(useSsl);
    }

    public String getLdapRealm() {
        return String.valueOf( ldapConfig.get("realm") );
    }

    public Dn getRootDn(LdapConnection connection){
        try {
            //return new Dn(connection.getRootDse().get("rootDomainNamingContext").getString());
            return LdapUtils.toDn(getLdapRealm());
        } catch (LdapException e) {
            throw new LdapRuntimeException(e);
        }
    }

    public LdapPrincipal getAdmin(){
        if( !ldapConfig.containsKey("adminUserDn") ){
            throw new NullPointerException("LDAP configuration doesn't contain \"adminUserDn\" mandatory property");
        }
        String adminUserDn = String.valueOf( ldapConfig.get("adminUserDn") );

        String adminUserPassword = null;
        if( ldapConfig.containsKey("adminUserPassword") )
            adminUserPassword = String.valueOf( ldapConfig.get("adminUserPassword") );

        return new LdapPrincipal(adminUserDn,adminUserPassword);
    }

    /**
     * Just for binding tests for now
     * But some LDAP connection parameters can depends from HTTP request parameters
     * @return
     */
    private HttpServletRequest getRequest(){
        return injector.getInstance(HttpServletRequest.class);
    }

    public boolean isUserDnTemplateDefined() {
        return ldapConfig.get("userDnTemplate") != null;
    }
}
