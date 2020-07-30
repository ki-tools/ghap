package io.ghap.auth;

import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import io.ghap.ldap.LdapConnectionProvider;
import org.apache.directory.ldap.client.api.LdapConnection;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import static javax.xml.bind.DatatypeConverter.parseBase64Binary;


/**
 * Simple authentication filter.
 *
 * Returns response with http status 401 when proper authentication is not provided in incoming request.
 *
 */
@Provider
@Singleton
public class SecurityFilter implements ContainerRequestFilter {

    private static final String REALM = "BASIC authentication";

    @Inject
    Injector injector;

    @Inject
    OauthCient oauthCient;

    @Context
    HttpServletRequest httpServletRequest;

    @Inject
    LdapConnectionProvider<LdapConnection> ldapConnectionProvider;

    @Override
    public ContainerRequest filter(ContainerRequest request) {
        if("OPTIONS".equalsIgnoreCase(request.getMethod())){
            return request;
        }

        LdapPrincipal principal = null;
        String accessToken;

        /*
        //FIXME remove before commit !!!!
        if(true){
            principal = new LdapPrincipal( "cn=Administrator,cn=users,dc=ad,dc=loc", "" );
        }
        */
        //if( false && !isBasicAuth(request) ) {
        if( !isBasicAuth(request) ) {
            if( (accessToken = request.getHeaderValue(HttpHeaders.AUTHORIZATION)) != null){
                principal = oauthCient.request(accessToken);
                if(principal == null){
                    throw new WebApplicationException(401);
                }
            } else {
                principal = formAuth(request);
                if(principal == null)
                    principal = sessionAuth();
            }
        }

        if(principal == null)
            principal = basicAuth(request);

        if(principal != null)
            request.setSecurityContext(new LdapSecurityContext(request, principal, ldapConnectionProvider));

        return request;
    }

    private LdapPrincipal sessionAuth(){
        HttpSession session = httpServletRequest.getSession();
        if( session != null ){
            Object username = session.getAttribute("username");
            Object password = session.getAttribute("password");

            if(username !=null && password != null &&
                  username instanceof String && password instanceof String){
                return new LdapPrincipal( (String)username, (String)password);
            }
        }
        return null;
    }

    private LdapPrincipal formAuth(ContainerRequest request){

        final String method = request.getMethod().toLowerCase();

        MultivaluedMap<String, String> params = null;

        switch (method) {
            case "get":
                params = request.getQueryParameters(true);
                break;
            case "post":
                params = request.getFormParameters();
                break;
            default:
                return null;
        }

        String username = params.getFirst("username");
        String password = params.getFirst("password");

        return (username !=null && password != null) ? new LdapPrincipal(username, password) : null;
    }

    private LdapPrincipal basicAuth(ContainerRequest request) {
        //GET, POST, PUT, DELETE, ...
        String method = request.getMethod();
        // myresource/get/56bCA for example
        String path = request.getPath(true);

        //We do allow wadl to be retrieve
        if(method.equals("GET") && (
                path.equals("application.wadl") ||
                path.equals("application.wadl/xsd0.xsd") ||
                path.equals("health")
        )){
            return null;
        }
        if(method.equals("POST") && (
                path.startsWith("user/password/token/") ||
                path.startsWith("user/password/request/") ||
                path.startsWith("issues/submit-error")
        )){
            return null;
        }

        // Extract authentication credentials
        String authentication = request.getHeaderValue(HttpHeaders.AUTHORIZATION);
        if (authentication == null) {
            throw new AuthenticationException("Authentication credentials are required for path \""+path+"\", method: " + method, REALM);
        }
        if (!authentication.startsWith("Basic ")) {
            return null;
            // additional checks should be done here
            // "Only HTTP Basic authentication is supported"
        }
        authentication = authentication.replaceFirst("[B|b]asic ", "");
        byte[] decodedBytes = parseBase64Binary(authentication);

        //If the decode fails in any case
        if(decodedBytes == null || decodedBytes.length == 0){
            return null;
        }

        //Now we can convert the byte[] into a splitted array :
        //  - the first one is login,
        //  - the second one password
        String[] values = new String(decodedBytes).split(":", 2);
        if (values.length < 2) {
            throw new WebApplicationException(400);
            // "Invalid syntax for username and password"
        }
        String username = values[0];
        String password = values[1];
        if ((username == null) || (password == null)) {
            throw new WebApplicationException(400);
            // "Missing username or password"
        }

        return new LdapPrincipal(username, password);
    }

    private boolean isBasicAuth(ContainerRequest request){
        String authentication = request.getHeaderValue(HttpHeaders.AUTHORIZATION);
        return authentication != null && authentication.toLowerCase().startsWith("basic ");
    }

}
