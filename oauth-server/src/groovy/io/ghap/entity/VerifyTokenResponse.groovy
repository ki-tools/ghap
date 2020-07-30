package io.ghap.entity

/**
 */
class VerifyTokenResponse {

    String audience;
    Set<String> scopes
    Principal principal;
    Long expires_in;
}
