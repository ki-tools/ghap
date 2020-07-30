package io.ghap.auth;

import io.ghap.web.Bootstrap;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiSession;
import org.apache.wiki.auth.SessionMonitor;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.HttpHeaders;

import static javax.xml.bind.DatatypeConverter.parseBase64Binary;

public class UserRoleFilter implements Filter {

    private WikiEngine m_engine = null;

    public void init(FilterConfig config) throws ServletException {

        ServletContext context = config.getServletContext();
        Bootstrap.configure(context);


        m_engine = WikiEngine.getInstance(context, null);

        // TODO: fetch any services we need to determine the user ?
        // e.g. database handler, ldap service, from session attribute or servlet context ?
    }

    public void doFilter(ServletRequest req, ServletResponse response,
                         FilterChain next) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        OauthClient oauthClient = OauthClient.getInstance();

        if("OPTIONS".equalsIgnoreCase(request.getMethod())){
            next.doFilter(req, response);
            return;
        }

        HttpSession httpSession = request.getSession();
        WikiSession session = SessionMonitor.getInstance(m_engine).find(httpSession);
        if( session.isAuthenticated() ){
            next.doFilter(req, response);
            return;
        }

        LdapPrincipal principal = null;
        String accessToken;

        /*
        //FIXME remove before commit !!!!
        if(true){
            principal = new LdapPrincipal( "cn=Administrator,cn=users,dc=ad,dc=loc", "" );
        }
        */
        try {
            //if( false && !isBasicAuth(request) ) {
            if (!isBasicAuth(request)) {

                accessToken = request.getHeader(HttpHeaders.AUTHORIZATION);
                if(accessToken == null){
                    accessToken = request.getParameter("access_token");
                }

                if ( accessToken != null) {
                    principal = oauthClient.request(accessToken);
                    if (principal == null) {
                        throw new AuthenticationException(401);
                    }
                } else {
                    principal = formAuth(request);
                    if (principal == null)
                        principal = sessionAuth(request);
                }
            }

            if (principal == null)
                principal = basicAuth(request);

            if (principal == null) {
                throw new AuthenticationException("Authentication credentials are required for path \""+request.getPathInfo()+"\", method: " + request.getMethod());
            }
            /*
            if(principal != null && accessToken != null){
                principal.setAccessToken(accessToken);
            }
            */
            /*
            if( principal != null){
                HttpSession session = request.getSession();
                session.setAttribute("username", principal.getName());
                session.setAttribute("password", principal.getPassword());

                if(accessToken != null) {
                    session.setAttribute("access-token", accessToken);
                }
            }
            */
        } catch (AuthenticationException ex){
            String oauthPath = OauthClient.getInstance().getOauthPath();
            String path = getFullURL(request);
            String suffix = "authorize?client_id=projectservice&response_type=token&redirect_uri=" + URLEncoder.encode(path, "UTF-8");
            ((HttpServletResponse)response).sendRedirect(oauthPath + suffix);
            return;
        }

        // call our request wrapper , which overrides getUserPrincipal and is UserInRole

        next.doFilter(new UserRoleRequestWrapper(principal, request), response);
    }

    public void destroy() {
    }

    private static String getFullURL(HttpServletRequest request) {
        StringBuffer requestURL = request.getRequestURL();
        String queryString = request.getQueryString();

        if (queryString == null) {
            return requestURL.toString() + "?tokenViaQueryString";
        } else {
            return requestURL.append('?').append(queryString).toString() + "&tokenViaQueryString";
        }
    }

    private LdapPrincipal sessionAuth(HttpServletRequest request){
        HttpSession session = request.getSession();
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

    private LdapPrincipal formAuth(HttpServletRequest request){

        final String method = request.getMethod().toLowerCase();

        Map params;

        switch (method) {
            case "get":
                params = request.getParameterMap();
                break;
            case "post":
                params = request.getParameterMap();
                break;
            default:
                return null;
        }

        String username = (String) params.get("username");
        String password = (String) params.get("password");

        return (username !=null && password != null) ? new LdapPrincipal(username, password) : null;
    }

    private LdapPrincipal basicAuth(HttpServletRequest request) throws AuthenticationException {
        //GET, POST, PUT, DELETE, ...
        String method = request.getMethod();
        // myresource/get/56bCA for example
        String path = request.getPathInfo() == null ? "":request.getPathInfo();

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
                        path.startsWith("user/password/request/")
        )){
            return null;
        }

        // Extract authentication credentials
        String authentication = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authentication == null) {
            throw new AuthenticationException("Authentication credentials are required for path \""+path+"\", method: " + method);
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
            throw new AuthenticationException(400);
            // "Invalid syntax for username and password"
        }
        String username = values[0];
        String password = values[1];
        if ((username == null) || (password == null)) {
            throw new AuthenticationException(400);
            // "Missing username or password"
        }

        return new LdapPrincipal(username, password);
    }

    private boolean isBasicAuth(HttpServletRequest request){
        String authentication = request.getHeader(HttpHeaders.AUTHORIZATION);
        return authentication != null && authentication.toLowerCase().startsWith("basic ");
    }
}
