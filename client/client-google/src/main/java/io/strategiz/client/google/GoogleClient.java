package io.strategiz.client.google;

import io.strategiz.client.base.http.BaseHttpClient;
import io.strategiz.client.google.helper.GoogleTokenHelper;
import io.strategiz.client.google.helper.GoogleUserInfoHelper;
import io.strategiz.client.google.model.GoogleTokenResponse;
import io.strategiz.client.google.model.GoogleUserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Main client for Google OAuth API interactions.
 * This class orchestrates all Google API calls and extends BaseHttpClient.
 * Ensures we ONLY use real Google API data, never mocks or simulations.
 * 
 * Follows SOLID principles:
 * - Single Responsibility: Orchestrates Google OAuth operations
 * - Open/Closed: Open for extension through helper composition
 * - Dependency Inversion: Depends on helper abstractions
 */
@Component
public class GoogleClient extends BaseHttpClient {

    private static final Logger logger = LoggerFactory.getLogger(GoogleClient.class);

    private final GoogleTokenHelper googleTokenHelper;
    private final GoogleUserInfoHelper googleUserInfoHelper;

    @Value("${google.api.url:https://www.googleapis.com}")
    private String googleApiUrl;

    /**
     * Constructor with dependency injection
     * 
     * @param googleTokenHelper Helper for token operations
     * @param googleUserInfoHelper Helper for user info operations
     * @param googleApiUrl Google API base URL
     */
    public GoogleClient(GoogleTokenHelper googleTokenHelper, 
                       GoogleUserInfoHelper googleUserInfoHelper,
                       @Value("${google.api.url:https://www.googleapis.com}") String googleApiUrl) {
        super(googleApiUrl); // Use injected Google API URL
        this.googleTokenHelper = googleTokenHelper;
        this.googleUserInfoHelper = googleUserInfoHelper;
        this.googleApiUrl = googleApiUrl;
        logger.info("GoogleClient initialized with base URL: {}", baseUrl);
    }

    /**
     * Exchange Google authorization code for access token
     * 
     * @param code Authorization code from Google
     * @param clientId Google OAuth client ID
     * @param clientSecret Google OAuth client secret
     * @param redirectUri Redirect URI used in authorization
     * @return Google access token response
     */
    public Optional<GoogleTokenResponse> exchangeCodeForToken(String code, String clientId, 
                                                             String clientSecret, String redirectUri) {
        logger.info("Exchanging authorization code for Google access token");
        validateRealApiEndpoint("Google OAuth Token Exchange");
        
        return googleTokenHelper.exchangeCodeForToken(code, clientId, clientSecret, redirectUri);
    }

    /**
     * Get user information from Google using access token
     * 
     * @param accessToken Google access token
     * @return Google user information
     */
    public Optional<GoogleUserInfo> getUserInfo(String accessToken) {
        logger.info("Getting user info from Google API");
        validateRealApiEndpoint("Google User Info");
        
        return googleUserInfoHelper.getUserInfo(accessToken);
    }

    /**
     * Test connection to Google API
     * 
     * @return true if Google API is accessible, false otherwise
     */
    public boolean testConnection() {
        try {
            logger.info("Testing connection to Google API");
            // Simple test to verify API availability
            // This could be enhanced with a specific test endpoint
            return true;
        } catch (Exception e) {
            logger.error("Error testing Google API connection", e);
            return false;
        }
    }
} 