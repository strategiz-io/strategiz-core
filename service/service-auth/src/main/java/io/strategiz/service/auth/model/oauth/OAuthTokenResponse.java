package io.strategiz.service.auth.model.oauth;

/**
 * Model for OAuth token response
 */
public class OAuthTokenResponse {
    private final String accessToken;
    private final String tokenType;
    private final Long expiresIn;

    public OAuthTokenResponse(String accessToken) {
        this(accessToken, "Bearer", null);
    }

    public OAuthTokenResponse(String accessToken, String tokenType, Long expiresIn) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }
} 