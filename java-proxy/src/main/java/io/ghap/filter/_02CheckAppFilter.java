package io.ghap.filter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.ghap.model.ShinyAppDescriptor;
import io.ghap.service.PrepareRulesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
@Singleton
public class _02CheckAppFilter implements Filter {

    public static final String SHINY_APP = "sh_app";
    public static final String REGISTRY_JSON = "registry.json";
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Inject
    private PrepareRulesService prepareRulesService;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;

        log.info("request url = {}", request.getRequestURL());
        List<ShinyAppDescriptor> registryFile = prepareRulesService.getRegistryFile();
        String requestPathInfo = request.getRequestURI();
        log.info("request path info = {}", requestPathInfo);
        if (requestPathInfo == null) {
            ((HttpServletResponse) servletResponse).sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String pathInfo = requestPathInfo.replaceFirst("/", "");
        if (pathInfo.contains(REGISTRY_JSON)) {
            request.setAttribute(REGISTRY_JSON, true);
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }
        for (ShinyAppDescriptor descriptor : registryFile) {
            if (pathInfo.contains(descriptor.getApplication().getApplicationRoot())) {
                log.info("found app in shiny with root {}", descriptor.getApplication().getApplicationRoot());
                request.setAttribute(SHINY_APP, descriptor);
                filterChain.doFilter(servletRequest, servletResponse);
                return;
            }
        }
        log.info("application not found. Return 404");
        ((HttpServletResponse) servletResponse).sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Override
    public void destroy() {

    }
}
