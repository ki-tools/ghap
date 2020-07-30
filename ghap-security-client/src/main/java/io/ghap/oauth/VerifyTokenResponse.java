package io.ghap.oauth;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.Set;

/**
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class VerifyTokenResponse {

    private String audience;
    private Set<String> scopes;
    private TokenPrincipal principal;
    private Long expires_in;

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public Set<String> getScopes() {
        return scopes;
    }

    public void setScopes(Set<String> scopes) {
        this.scopes = scopes;
    }

    public TokenPrincipal getPrincipal() {
        return principal;
    }

    public void setPrincipal(TokenPrincipal principal) {
        this.principal = principal;
    }

    public Long getExpires_in() {
        return expires_in;
    }

    public void setExpires_in(Long expires_in) {
        this.expires_in = expires_in;
    }
}
