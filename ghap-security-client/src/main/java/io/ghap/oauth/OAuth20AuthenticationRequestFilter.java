package io.ghap.oauth;

import com.github.hburgmeier.jerseyoauth2.api.protocol.IRequestFactory;
import com.github.hburgmeier.jerseyoauth2.api.protocol.IResourceAccessRequest;
import com.github.hburgmeier.jerseyoauth2.api.protocol.OAuth2ParseException;
import com.github.hburgmeier.jerseyoauth2.api.token.IAccessTokenInfo;
import com.github.hburgmeier.jerseyoauth2.api.token.InvalidTokenException;
import com.github.hburgmeier.jerseyoauth2.api.types.ParameterStyle;
import com.github.hburgmeier.jerseyoauth2.api.types.TokenType;
import com.github.hburgmeier.jerseyoauth2.rs.api.IRSConfiguration;
import com.github.hburgmeier.jerseyoauth2.rs.api.token.IAccessTokenVerifier;
import com.github.hburgmeier.jerseyoauth2.rs.impl.base.AbstractOAuth2Filter;
import com.github.hburgmeier.jerseyoauth2.rs.impl.base.OAuth2FilterException;
import com.github.hburgmeier.jerseyoauth2.rs.impl.base.context.OAuthPrincipal;
import com.github.hburgmeier.jerseyoauth2.rs.impl.base.context.OAuthSecurityContext;
import com.github.hburgmeier.jerseyoauth2.rs.impl.filter.HttpRequestAdapter;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import org.apache.commons.lang.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 */
public class OAuth20AuthenticationRequestFilter extends AbstractOAuth2Filter implements ContainerRequestFilter {

    private static final String X_SSL_SECURE = "X-SSL-Secure";
    private static final String ERROR_FILTER_REQUEST = "Error in filter request";

    private static final Logger LOGGER = LoggerFactory.getLogger(OAuth20AuthenticationRequestFilter.class);

    private Set<String> requiredScopes;
    private final IAccessTokenVerifier accessTokenVerifier;
    private final IRequestFactory requestFactory;
    private EnumSet<ParameterStyle> parameterStyles;
    private EnumSet<TokenType> tokenTypes;
    private PredicateType predicateType;

    public OAuth20AuthenticationRequestFilter(final IAccessTokenVerifier accessTokenVerifier, final IRSConfiguration configuration,
                                              final IRequestFactory requestFactory) {
        this.accessTokenVerifier = accessTokenVerifier;
        this.requestFactory = requestFactory;
        this.parameterStyles = configuration.getSupportedOAuthParameterStyles();
        this.tokenTypes = configuration.getSupportedTokenTypes();
    }

    @Override
    public ContainerRequest filter(ContainerRequest containerRequest) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            IResourceAccessRequest oauthRequest = requestFactory.parseResourceAccessRequest(new HttpRequestAdapter(containerRequest), parameterStyles, tokenTypes);
            LOGGER.debug("parse request successful");

            URI requestUri = containerRequest.getRequestUri();
            String secureSSL = containerRequest.getHeaderValue(X_SSL_SECURE);
            boolean secure = isRequestSecure(requestUri, secureSSL);
            SecurityContext securityContext = predicateType == PredicateType.OR ?
                    __filterOAuth2Request(oauthRequest, requiredScopes, secure) : filterOAuth2Request(oauthRequest, requiredScopes, secure);

            containerRequest.setSecurityContext(securityContext );
            LOGGER.debug("set SecurityContext. User {}", securityContext.getUserPrincipal().getName());

            return containerRequest;
        } catch (OAuth2ParseException e) {
            LOGGER.debug(ERROR_FILTER_REQUEST, e);
            throw new WebApplicationException(e, buildAuthProblem());
        } catch (InvalidTokenException e) {
            LOGGER.error(ERROR_FILTER_REQUEST, e);
            throw new WebApplicationException(e, buildAuthProblem());
        } catch (OAuth2FilterException e) {
            LOGGER.error(ERROR_FILTER_REQUEST, e);
            throw new WebApplicationException(e, e.getErrorResponse());
        } finally {
            stopWatch.stop();
            System.out.println("filter method execution time " + stopWatch.toString());
        }
    }

    void setRequiredScopes(String[] scopes) {
        this.requiredScopes = new HashSet<>(Arrays.asList(scopes));
    }

    @Override
    protected IAccessTokenVerifier getAccessTokenVerifier() {
        return accessTokenVerifier;
    }

    protected SecurityContext __filterOAuth2Request(IResourceAccessRequest oauthRequest, Set<String> requiredScopes, boolean secureRequest) throws InvalidTokenException, OAuth2FilterException
    {
        String accessToken = oauthRequest.getAccessToken();

        IAccessTokenInfo accessTokenInfo = getAccessTokenVerifier().verifyAccessToken(accessToken);
        if (accessTokenInfo==null)
        {
            throw new InvalidTokenException(accessToken);
        }
        if (accessTokenInfo.getUser()==null)
        {
            LOGGER.error("no user stored in token {}", accessToken);
            throw new OAuth2FilterException(buildUserProblem());
        }

        if (accessTokenInfo.getClientId()==null)
        {
            LOGGER.error("no client stored in token {}", accessToken);
            throw new OAuth2FilterException(buildClientProblem());
        }

        Set<String> authorizedScopes = accessTokenInfo.getAuthorizedScopes();
        if (requiredScopes!=null)
        {
            if (!checkScopes(requiredScopes, authorizedScopes))
            {
                LOGGER.error("Scopes did not match, required {}, actual {}", requiredScopes, authorizedScopes);
                throw new OAuth2FilterException(buildScopeProblem());
            }
        }

        OAuthPrincipal principal = new OAuthPrincipal(accessTokenInfo.getClientId(), accessTokenInfo.getUser(), authorizedScopes);
        return new OAuthSecurityContext(principal, secureRequest);
    }

    protected boolean checkScopes(Set<String> requiredScopes, Set<String> actualScopes)
    {
        if (actualScopes==null && requiredScopes==null)
        {
            return true;
        }
        if (actualScopes==null && requiredScopes!=null && !requiredScopes.isEmpty())
        {
            return false;
        }
        for (String scope: requiredScopes) {
            if (actualScopes.contains(scope)) {
                return true;
            }
        }
        return false;
    }

    public void setPredicateType(PredicateType predicateType) {
        this.predicateType = predicateType;
    }

    @Override
    protected Response buildScopeProblem() {
        return Response.serverError().
                status(HttpURLConnection.HTTP_FORBIDDEN).
                build();
    }

    @Override
    protected Response buildUserProblem() {
        return Response.serverError().
                status(HttpURLConnection.HTTP_UNAUTHORIZED).
                build();
    }

    @Override
    protected Response buildClientProblem() {
        return Response.serverError().
                status(HttpURLConnection.HTTP_UNAUTHORIZED).
                build();
    }

    @Override
    protected Response buildAuthProblem() {
        return Response.serverError().
                status(HttpURLConnection.HTTP_UNAUTHORIZED).
                build();
    }
}
