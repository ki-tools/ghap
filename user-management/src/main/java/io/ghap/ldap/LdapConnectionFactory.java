package io.ghap.ldap;

import com.google.common.collect.ImmutableMap;
import io.ghap.auth.AuthenticationException;
import io.ghap.auth.LdapConfiguration;
import io.ghap.auth.LdapPrincipal;
import io.ghap.ldap.template.LdapConnectionTemplate;
import io.ghap.user.dao.mapper.UserEntryMapper;
import io.ghap.user.model.User;
import org.apache.commons.io.IOUtils;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.directory.api.ldap.model.exception.LdapAuthenticationException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.*;
import org.apache.directory.ldap.client.template.PasswordWarning;
import org.apache.directory.ldap.client.template.exception.PasswordException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// should not be Singleton since configuration will not be injected
public class LdapConnectionFactory {

    protected Logger log = LoggerFactory.getLogger(this.getClass());

    @Inject LdapConfiguration ldapConfiguration;
    @Inject InstallCert installCert;

    public LdapConnection get(LdapPrincipal ldapPrincipal) throws LdapException {

        String name = ldapPrincipal.getName();
        LdapConnection connection = null;
        try {
            connection = getConnection();
            Dn rootDn = ldapConfiguration.getRootDn(connection);

            PasswordWarning result;
            Dn dn;
            long start = System.currentTimeMillis();
            LdapPrincipal admin = ldapConfiguration.getAdmin();

            boolean withRealm = (name.split("@").length == 2);
            if(withRealm) {
                log.info("User name with realm was provided for binding. Dn: " + name);
                String[] nameAndRealm = name.split("@");

                if(admin != null) {
                    try {
                        start = System.currentTimeMillis();
                        connection.bind(admin.getName(), admin.getPassword());
                        log.info("Admin user search time: " + (System.currentTimeMillis() - start));
                    } catch (LdapAuthenticationException e){
                        throw new WebApplicationException( Response.status(503).entity(e.getMessage()).build() );
                    }
                }
                else {
                    start = System.currentTimeMillis();
                    connection.anonymousBind();
                    log.info("Anonymous user binding time: " + (System.currentTimeMillis() - start));
                }

                String filter = String.format("(&(objectClass=user)(userPrincipalName=%s))", name);


                start = System.currentTimeMillis();
                User user = new LdapConnectionTemplate(connection).searchFirst(rootDn, filter, SearchScope.SUBTREE, UserEntryMapper.getInstance());
                log.info("Search user \""+name+"\" by \"userPrincipalName\" time: " + (System.currentTimeMillis() - start));

                if(user == null){
                    filter = String.format("(&(objectClass=user)(sAMAccountName=%s))", nameAndRealm[0]);

                    String dnStr = Stream.of(nameAndRealm[1].split("\\."))
                            .map(it -> "dc=" + it)
                            .collect( Collectors.joining(",") );

                    rootDn = new Dn( dnStr );
                    start = System.currentTimeMillis();
                    user = new LdapConnectionTemplate(connection).searchFirst(rootDn, filter, SearchScope.SUBTREE, UserEntryMapper.getInstance());
                    log.info("Search user \""+name+"\" by \"sAMAccountName\" time: " + (System.currentTimeMillis() - start));
                }
                if(user == null){
                    throw new AuthenticationException("User name or password invalid", nameAndRealm[1]);
                }
                dn = new Dn( user.getDn() );
            }
            else {
                boolean withDn = name.contains(",") && name.contains("=");
                if( withDn ) {
                    dn = new Dn(name);
                    log.info("DN was provided for binding. Dn: " + name);
                }
                else {
                    log.info("User name was provided for binding. Dn: " + name);
                    if(admin != null) {
                        try {
                            start = System.currentTimeMillis();
                            connection.bind(admin.getName(), admin.getPassword());
                            log.info("Admin user search time: " + (System.currentTimeMillis() - start));
                        } catch (LdapAuthenticationException e){
                            throw new WebApplicationException( Response.status(503).entity(e.getMessage()).build() );
                        }
                    }
                    else {
                        start = System.currentTimeMillis();
                        connection.anonymousBind();
                        log.info("Anonymous user binding time: " + (System.currentTimeMillis() - start));
                    }

                    String filter = String.format("(&(objectClass=user)(sAMAccountName=%s))", name);
                    start = System.currentTimeMillis();
                    User user = new LdapConnectionTemplate(connection).searchFirst(rootDn, filter, SearchScope.SUBTREE, UserEntryMapper.getInstance());
                    log.info("Search user \""+name+"\" by \"sAMAccountName\" time: " + (System.currentTimeMillis() - start));
                    if(user == null){
                        throw new WebApplicationException( Response.status(401).entity(ResultCodeEnum.INVALID_CREDENTIALS.getMessage()).build() );
                    }
                    dn = new Dn( user.getDn() );
                }
            }


            start = System.currentTimeMillis();
            result = new LdapConnectionTemplate(connection).authenticateConnection(dn, ldapPrincipal.getPassword());
            log.info("Authenticate connection time for \""+dn+"\": " + (System.currentTimeMillis() - start));

            ldapPrincipal.setDn(dn);

            if(result != null){
                //TODO bind "result" object to somewere to be available during "sign-in"
                //to allow to send worning to user with response from "auth/sign-in"
            }
        } catch (LdapException e) {
            // return connection to pool
            IOUtils.closeQuietly(connection);
            throw e;
        } catch (PasswordException passwordException){
            IOUtils.closeQuietly(connection);
            ResultCodeEnum res = passwordException.getResultCode();

            Exception e = passwordException;
            if(res == null || passwordException.getMessage() == null){
                e = passwordException.getLdapException() != null ? passwordException.getLdapException():passwordException;
                log.error("Cannot bind LDAP connection for \"" + name + "\"("+ldapConfiguration.getLdapHost()+":"+ldapConfiguration.getLdapPort()+")", e);
            }

            Map error = ImmutableMap.of(
                    "message", e.getMessage() == null ? e.toString():e.getMessage(),
                    "resultCode",    res == null ? "":res.getMessage(),
                    "code",   res == null ? "": res.getResultCode()
            );
            throw new WebApplicationException( Response.status(401).entity(error).build() );
        } catch (Throwable e){
            // return connection to pool
            IOUtils.closeQuietly(connection);
            throw e;
        }

        return connection;
    }

    private LdapConnection getConnection() throws LdapException {

        return new LdapConnectionWrapper( getConnectionPool() );
    }


    private LdapConnectionPool pool;

    @PostConstruct
    private LdapConnectionPool getConnectionPool(){
        if(pool == null){
            synchronized (this) {
                if(pool == null) {

                    // install AD cert
                    if(ldapConfiguration.isUseSsl()){
                        try {
                            // commented since we use "NoVerificationTrustManager"
                            //installCert.install();
                        } catch (Exception e) {
                            log.error("Cannot install AD cert", e);
                        }
                    }

                    LdapConnectionConfig config = new LdapConnectionConfig();
                    config.setLdapHost(ldapConfiguration.getLdapHost());
                    config.setLdapPort(ldapConfiguration.getLdapPort());
                    config.setUseSsl(ldapConfiguration.isUseSsl());
                    config.setUseTls(true);
                    //config.setSslProtocol("SSLv3");
                    config.setTrustManagers(new NoVerificationTrustManager());

                    DefaultLdapConnectionFactory factory = new DefaultLdapConnectionFactory( config );
                    //factory.setTimeOut(30000);

                    // optional, values below are defaults
                    GenericObjectPool.Config poolConfig = new GenericObjectPool.Config();
                    poolConfig.maxActive = 20;
                    poolConfig.maxIdle = 5;
                    /*
                    poolConfig.lifo = true;
                    poolConfig.maxActive = 8;
                    poolConfig.maxIdle = 8;
                    poolConfig.maxWait = -1L;
                    poolConfig.minEvictableIdleTimeMillis = 1000L * 60L * 30L;
                    poolConfig.minIdle = 0;
                    poolConfig.numTestsPerEvictionRun = 3;
                    poolConfig.softMinEvictableIdleTimeMillis = -1L;
                    poolConfig.testOnBorrow = false;
                    poolConfig.testOnReturn = false;
                    poolConfig.testWhileIdle = false;
                    poolConfig.timeBetweenEvictionRunsMillis = -1L;
                    poolConfig.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_BLOCK;
                    */

                    DefaultPoolableLdapConnectionFactory connectionFactory = new DefaultPoolableLdapConnectionFactory( factory );
                    pool =  new LdapConnectionPool(connectionFactory, poolConfig );
                }
            }
        }
        return pool;
    }
}
