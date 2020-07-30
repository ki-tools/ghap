package io.ghap.project.domain;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.persist.PersistService;
import com.google.inject.persist.UnitOfWork;

import javax.servlet.*;
import java.io.IOException;

/**
 */
@Singleton
public class PersistFilter implements Filter {

    private final UnitOfWork unitOfWork;
    private final PersistService persistService;

    @Inject
    public PersistFilter(UnitOfWork unitOfWork, PersistService persistService) {
        this.unitOfWork = unitOfWork;
        this.persistService = persistService;
    }

    public void init(FilterConfig filterConfig) throws ServletException {
        persistService.start();
    }

    public void destroy() {
        persistService.stop();
    }

    public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse,
                         final FilterChain filterChain) throws IOException, ServletException {
        unitOfWork.begin();
        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            unitOfWork.end();
        }
    }
}
