package io.ghap.filter;

import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tuckey.web.filters.urlrewrite.Conf;
import org.tuckey.web.filters.urlrewrite.UrlRewriteFilter;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
@Singleton
public class _05UrlRewriteFilter extends UrlRewriteFilter {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    protected void loadUrlRewriter(FilterConfig filterConfig) throws ServletException {
        super.loadUrlRewriter(filterConfig);
        if (getUrlRewriter(null, null, null) == null) {
            InputStream resource = null;
            URL url;
            try {
                url = filterConfig.getServletContext().getResource(_01LoggingFilter.getResourcePath());
                resource = new FileInputStream(url.getFile());
            } catch (Exception e) {
                throw new ServletException(e);
            }
            Conf conf = new Conf(filterConfig.getServletContext(), resource, _01LoggingFilter.getResourcePath(), url.toString(), false);
            checkConf(conf);
        }
    }
}
