package io.ghap.filter;

import com.github.hburgmeier.jerseyoauth2.api.protocol.IHttpRequest;
import com.github.hburgmeier.jerseyoauth2.api.token.IAccessTokenInfo;
import com.github.hburgmeier.jerseyoauth2.api.token.InvalidTokenException;
import com.github.hburgmeier.jerseyoauth2.api.types.ParameterStyle;
import com.github.hburgmeier.jerseyoauth2.api.types.TokenType;
import com.github.hburgmeier.jerseyoauth2.protocol.impl.HttpHeaders;
import com.github.hburgmeier.jerseyoauth2.protocol.impl.HttpRequestAdapter;
import com.github.hburgmeier.jerseyoauth2.protocol.impl.extractor.FormExtractor;
import com.github.hburgmeier.jerseyoauth2.protocol.impl.extractor.HeaderExtractor;
import com.github.hburgmeier.jerseyoauth2.protocol.impl.extractor.IExtractor;
import com.github.hburgmeier.jerseyoauth2.protocol.impl.extractor.QueryParameterExtractor;
import com.github.hburgmeier.jerseyoauth2.rs.api.token.IAccessTokenVerifier;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.ghap.model.TokenResponse;
import io.ghap.service.OauthService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
@Singleton
public class _03AuthenticationFilter implements Filter {

    public static final String CURRENT_USER = "cu_user";
    public static final String ACCESS_TOKEN = "acc_tok";
    private static final String REDIRECT_URL = "red_url";

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Inject
    private IAccessTokenVerifier tokenVerifier;

    @Inject
    private OauthService oauthService;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        log.info("start authenticate");

        String code = request.getParameter("code");
        if (StringUtils.isNotBlank(code)) {
            log.info("found code in request {}, try to get access token", code);
            String redirectUri = (String) request.getSession().getAttribute(REDIRECT_URL);
            if (StringUtils.isBlank(redirectUri)) {
                redirectUri = oauthService.buildLoginUrl(getRequestUrl(request));
            }
            try {
                TokenResponse tokenResponse = oauthService.exchangeToken(code, redirectUri);
                request.getSession().setAttribute(ACCESS_TOKEN, tokenResponse.getAccessToken());
                log.info("got access token {}", tokenResponse.getAccessToken());
            } catch (WebApplicationException e) {
                log.error("error exchange code token", e);
                redirectToLoginPage(request, response);
                return;
            }
        }

        String token = request.getParameter("token");
        if (StringUtils.isNotBlank(token)) {
            request.getSession().setAttribute(ACCESS_TOKEN, token);
        }
        String accessToken = (String) request.getSession().getAttribute(ACCESS_TOKEN);
        if (accessToken == null) {
            redirectToLoginPage(request, response);
            return;
        }

        try {
            IAccessTokenInfo tokenInfo = tokenVerifier.verifyAccessToken(accessToken);
            request.setAttribute(CURRENT_USER, tokenInfo);
            request.getSession().setAttribute(ACCESS_TOKEN, accessToken);
            log.info("token verified successfully");
        } catch (InvalidTokenException e) {
            log.error("token invalid " + accessToken, e);
            redirectToLoginPage(request, response);
            return;
        }
        chain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {

    }

    protected void redirectToLoginPage(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.getSession().removeAttribute(ACCESS_TOKEN);
        String requestUrl = getRequestUrl(request);
        request.getSession().setAttribute(REDIRECT_URL, requestUrl);
        String redirectUrl = oauthService.buildLoginUrl(requestUrl);
        log.info("no access token in session. Redirect to login {}", redirectUrl);
        response.sendRedirect(redirectUrl);
    }

    public static final String getRequestUrl(HttpServletRequest request) {
        StringBuffer requestURL = request.getRequestURL();
        return requestURL.toString();
    }

    private static class AccessTokenHeaderExtractor extends HeaderExtractor {
        private static final Pattern AUTH_PATTERN = Pattern.compile("([a-zA-Z]+) (.+)");
        private TokenType tokenType;

        public AccessTokenHeaderExtractor(TokenType tokenType) {
            super(HttpHeaders.AUTHORIZATION);
            this.tokenType = tokenType;
        }

        @Override
        public String extractValue(IHttpRequest request) {
            String value = super.extractValue(request);
            String accessToken = null;
            if (StringUtils.isNotEmpty(value)) {
                Matcher mat = AUTH_PATTERN.matcher(value);
                if (mat.matches() && mat.group(1).equalsIgnoreCase(tokenType.toString())) {
                    accessToken = mat.group(2);
                }
            }
            return accessToken;
        }
    }
}
