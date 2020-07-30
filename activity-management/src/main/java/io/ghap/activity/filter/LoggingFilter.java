package io.ghap.activity.filter;

import com.google.inject.Singleton;
import org.slf4j.MDC;

import javax.servlet.*;
import java.io.IOException;
import java.util.UUID;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
@Singleton
public class LoggingFilter implements Filter {

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {

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
}
