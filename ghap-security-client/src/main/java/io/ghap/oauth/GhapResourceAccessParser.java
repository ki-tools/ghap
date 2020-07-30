package io.ghap.oauth;

import com.github.hburgmeier.jerseyoauth2.api.protocol.IHttpRequest;
import com.github.hburgmeier.jerseyoauth2.api.protocol.OAuth2ParseException;
import com.github.hburgmeier.jerseyoauth2.api.types.ParameterStyle;
import com.github.hburgmeier.jerseyoauth2.api.types.TokenType;
import com.github.hburgmeier.jerseyoauth2.protocol.impl.resourceaccess.ResourceAccessParser;
import com.github.hburgmeier.jerseyoauth2.protocol.impl.resourceaccess.ResourceAccessRequest;

import java.util.EnumSet;

/**
 */
public class GhapResourceAccessParser extends ResourceAccessParser {

    @Override
    public ResourceAccessRequest parse(IHttpRequest request, EnumSet<ParameterStyle> parameterStyles, EnumSet<TokenType> tokenTypes) throws OAuth2ParseException {
        try {
            return super.parse(request, parameterStyles, tokenTypes);
        } catch (OAuth2ParseException e) {
            String token = request.getQueryParameter("token");
            if (token == null) {
                throw e;
            }
            return new ResourceAccessRequest(token, TokenType.BEARER);
        }
    }
}
