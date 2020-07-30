package io.ghap.banner.domain;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.persist.PersistService;

import javax.servlet.*;
import java.io.IOException;

/**
 */
@Singleton
public class PersistFilter implements Filter {

    private final PersistService persistService;

    @Inject
    public PersistFilter(PersistService persistService) {
        this.persistService = persistService;
    }

    public void init(FilterConfig filterConfig) throws ServletException {
        persistService.start();
    }

    public void destroy() {
        persistService.stop();
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        filterChain.doFilter(servletRequest, servletResponse);
    }
}
