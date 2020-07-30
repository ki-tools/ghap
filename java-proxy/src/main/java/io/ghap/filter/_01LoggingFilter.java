package io.ghap.filter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.config.ConfigurationManager;
import io.ghap.service.PrepareRulesService;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.servlet.*;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.UUID;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
@Singleton
public class _01LoggingFilter implements Filter {

    public static final String DEV_RESOURCE = "/src/main/webapp/WEB-INF/urlrewrite.xml";
    public static final String OTHER_RESOURCE = "/WEB-INF/urlrewrite.xml";
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Inject
    private PrepareRulesService prepareRulesService;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("init logging filter");
        prepareRulesService.scheduleRegistryUpdate();
        String shinyUrl = ConfigurationManager.getConfigInstance().getString("shiny.url");
        InputStream resource = null;
        String resourcePath = getResourcePath();
        log.info("urlrewritepath = {}", resourcePath);
        try {
            URL url = filterConfig.getServletContext().getResource(resourcePath);
            String file = url.getFile();
            log.info("urlrewrite path = {}", file);
            resource = new FileInputStream(file);
        } catch (Exception e) {
            throw new ServletException(e);
        }
        if (resource != null) {
            log.info("urlrewrite resource is not null");
            try {
                String content = IOUtils.toString(resource, "UTF-8");
                log.info("old content = {}", content);
                content = content.replace("%{baseUrl}", shinyUrl);
                log.info("new content = {}", content);
                String file = filterConfig.getServletContext().getResource(resourcePath).getFile();
                log.info("file for urlrewrite xml {}", file);
                IOUtils.write(content,
                        new FileOutputStream(file), "UTF-8");
            } catch (IOException e) {
                throw new ServletException(e);
            }
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        MDC.put("traceId", UUID.randomUUID().toString());
        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            MDC.remove("traceId");
        }
    }

    @Override
    public void destroy() {

    }

    public static String getResourcePath() {
        return isDevEnv() ? DEV_RESOURCE : OTHER_RESOURCE;
    }

    public static boolean isDevEnv() {
        String env = ConfigurationManager.getDeploymentContext().getDeploymentEnvironment();
        return (env == null || "dev".equalsIgnoreCase(env));
    }
}
