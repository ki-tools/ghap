package io.ghap.service;

import io.ghap.model.TokenResponse;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
public interface OauthService {
    String buildLoginUrl(String currentUrl);

    TokenResponse exchangeToken(String code, String redirectUri);
}
