package io.ghap.oauth

import grails.plugin.springsecurity.web.authentication.RequestHolderAuthenticationFilter
import io.ghap.exception.PrivacyPolicyNotAcceptedException
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException

import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 */
class GhapRequestHolderAuthenticationFilter extends RequestHolderAuthenticationFilter{

    @Override
    void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (((HttpServletRequest)request).getServletPath().contains("/index.gsp")) {
            def session = ((HttpServletRequest) request).getSession(false);
            if (session) {
                session.invalidate();
                ((HttpServletResponse)response).sendRedirect(((HttpServletRequest)request).getContextPath())
                return
            }
        }
        super.doFilter(request, response, chain)
    }

    @Override
    Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        /*
        if (!"on".equalsIgnoreCase(request.getParameter("user-policy"))) {
            throw new PrivacyPolicyNotAcceptedException("privacy policy not accepted")
        }
        */
        def authentication = super.attemptAuthentication(request, response)
        Cookie cookie = new Cookie("ppolicyread", "read");
        cookie.setMaxAge(-1);
        cookie.setPath("/");
        response.addCookie(cookie);
        return authentication
    }

    private Cookie findCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length <= 0) {
            return null;
        }
        for (Cookie cookie: cookies) {
            if (name.equalsIgnoreCase(cookie.getName())) {
                return cookie;
            }
        }
        return null;
    }
}
