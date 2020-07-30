package io.ghap.auth;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class CorsFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest)servletRequest;
        HttpServletResponse response = (HttpServletResponse)servletResponse;

        String origin = request.getHeader("origin");

        response.addHeader("Access-Control-Allow-Origin", origin == null ? "*" : origin);
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, PATCH, OPTIONS, PUT, DELETE, X-XSRF-TOKEN");
        response.addHeader("Access-Control-Allow-Credentials", "true");

        String reqHead = request.getHeader("Access-Control-Request-Headers");

        if(null != reqHead && !reqHead.isEmpty()){
            response.addHeader("Access-Control-Allow-Headers", reqHead);
        }
        else {
            response.addHeader("Access-Control-Allow-Headers",
                    "Cache-Control, Pragma, Origin, Authorization, Content-Type, X-Requested-With");
        }
    }

    @Override
    public void destroy() {

    }
}
