package io.ghap.filter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.ghap.model.ShinyApp;
import io.ghap.model.ShinyAppDescriptor;
import io.ghap.service.ProjectService;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
@Singleton
public class _04AuthorizationFilter implements Filter {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private static final String NA = "N/A";

    @Inject
    private ProjectService projectService;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        Boolean registryJson = (Boolean) request.getAttribute(_02CheckAppFilter.REGISTRY_JSON);
        if (BooleanUtils.isTrue(registryJson)) {
            log.info("accessing registry json");
            chain.doFilter(servletRequest, servletResponse);
            return;
        }
        log.info("start authorize user");
        ShinyAppDescriptor descriptor = (ShinyAppDescriptor) request.getAttribute(_02CheckAppFilter.SHINY_APP);
        ShinyApp application = descriptor.getApplication();
        String grant = removeNa(application.getGrant());
        String project = removeNa(application.getProject());
        String accessToken = (String) request.getSession().getAttribute(_03AuthenticationFilter.ACCESS_TOKEN);
        log.info("grant = {}, project = {}, accessToken = {}", grant, project, accessToken);
        List<String> projectAndGrants = new ArrayList<>();
        projectAndGrants.addAll(Arrays.asList(project.split(",")));
        projectAndGrants.addAll(Arrays.asList(grant.split(",")));
        boolean checkPerformed = false;
        //TODO ask about or/and condition
        for (String projectGrant : projectAndGrants) {
            if (StringUtils.isBlank(projectGrant)) {
                continue;
            }
            String[] split = projectGrant.split("/");
            if (split.length != 1 && split.length != 2) {
                log.error("cannot parse project grant string {}", projectGrant);
                continue;
            }
            String projectName = split[0].trim();
            checkPerformed = true;
            if (split.length == 1) {
                if (StringUtils.isNotBlank(projectName) && projectService.isUserHasProjectPermission(accessToken, projectName)) {
                    chain.doFilter(servletRequest, servletResponse);
                    return;
                }
            } else {
                String grantName = split[1];
                if (StringUtils.isNotBlank(grantName) && StringUtils.isNotBlank(projectName) && projectService.isUserHasGrantPermission(accessToken, projectName, grantName)) {
                    chain.doFilter(servletRequest, servletResponse);
                    return;
                }
            }
        }

        if (checkPerformed) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        chain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {

    }

    private String removeNa(String source) {
        if (source == null) {
            return StringUtils.EMPTY;
        }
        return source.replace(NA, "");
    }
}
