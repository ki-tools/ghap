package io.ghap.oauth;

import com.github.hburgmeier.jerseyoauth2.api.protocol.IHttpRequest;
import com.github.hburgmeier.jerseyoauth2.api.protocol.IResourceAccessRequest;
import com.github.hburgmeier.jerseyoauth2.api.protocol.OAuth2ParseException;
import com.github.hburgmeier.jerseyoauth2.api.types.ParameterStyle;
import com.github.hburgmeier.jerseyoauth2.api.types.TokenType;
import com.github.hburgmeier.jerseyoauth2.protocol.impl.RequestFactory;
import com.github.hburgmeier.jerseyoauth2.protocol.impl.resourceaccess.ResourceAccessParser;
import com.github.hburgmeier.jerseyoauth2.protocol.impl.resourceaccess.ResourceAccessRequest;

import java.util.EnumSet;

/**
 */
public class GhapRequestFactory extends RequestFactory {

    protected ResourceAccessParser resourceAccessParser = new GhapResourceAccessParser();

    @Override
    public IResourceAccessRequest parseResourceAccessRequest(IHttpRequest request,
                                                             EnumSet<ParameterStyle> parameterStyles, EnumSet<TokenType> tokenTypes) throws OAuth2ParseException {
        ResourceAccessRequest accessRequest = resourceAccessParser.parse(request, parameterStyles, tokenTypes);
        accessRequest.validate();
        return accessRequest;
    }
}
