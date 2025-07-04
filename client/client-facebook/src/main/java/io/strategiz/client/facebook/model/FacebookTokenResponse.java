package io.strategiz.client.facebook.model;

import java.io.Serializable;

/**
 * Model for Facebook OAuth token response
 */
public class FacebookTokenResponse implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private final String accessToken;
    private final String tokenType;
    private final Integer expiresIn;

    public FacebookTokenResponse(String accessToken, String tokenType, Integer expiresIn) {
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

    public Integer getExpiresIn() {
        return expiresIn;
    }

    @Override
    public String toString() {
        return "FacebookTokenResponse{" +
               "tokenType='" + tokenType + '\'' +
               ", expiresIn=" + expiresIn +
               '}';
    }
} 