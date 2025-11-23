package io.strategiz.client.schwab;

import io.strategiz.client.base.http.BaseHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;

/**
 * Client for Charles Schwab API integration.
 * Handles OAuth authentication and API calls to Schwab's trading platform.
 */
@Component
public class SchwabClient extends BaseHttpClient {
    
    private static final Logger log = LoggerFactory.getLogger(SchwabClient.class);
    
    private static final String DEFAULT_BASE_URL = "https://api.schwabapi.com";
    private static final String AUTH_BASE_URL = "https://api.schwabapi.com/v1";
    
    @Value("${oauth.providers.schwab.client-id:}")
    private String clientId;
    
    @Value("${oauth.providers.schwab.client-secret:}")
    private String clientSecret;
    
    @Value("${oauth.providers.schwab.redirect-uri}")
    private String redirectUri;
    
    private final RestTemplate restTemplate;
    
    public SchwabClient() {
        super(DEFAULT_BASE_URL);
        this.restTemplate = new RestTemplate();
        log.info("SchwabClient initialized with base URL: {}", DEFAULT_BASE_URL);
    }
    
    /**
     * Generate OAuth authorization URL for Charles Schwab.
     * 
     * @param state Security state parameter
     * @return Authorization URL for user to visit
     */
    public String generateAuthorizationUrl(String state) {
        String scope = "readonly"; // Schwab basic scope for account access
        
        return String.format(
            "%s/oauth/authorize?client_id=%s&redirect_uri=%s&response_type=code&state=%s&scope=%s",
            AUTH_BASE_URL,
            clientId,
            redirectUri,
            state,
            scope
        );
    }
    
    /**
     * Exchange authorization code for access token.
     * 
     * @param authorizationCode The authorization code from Schwab
     * @return Token response containing access token and refresh token
     */
    public Map<String, Object> exchangeCodeForTokens(String authorizationCode) {
        String tokenUrl = AUTH_BASE_URL + "/oauth/token";

        // Build the token exchange request (without client credentials in body)
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("code", authorizationCode);
        params.add("redirect_uri", redirectUri);

        // Set up headers with Basic Authentication
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setBasicAuth(clientId, clientSecret);  // Use Basic Auth instead of form params

        // Create the request entity
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        log.debug("Exchanging authorization code for tokens at: {}", tokenUrl);

        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            log.info("Successfully exchanged authorization code for Schwab tokens");
            return response.getBody();
        } else {
            throw new RuntimeException("Token exchange failed with status: " + response.getStatusCode());
        }
    }
    
    /**
     * Refresh expired access token using refresh token.
     * 
     * @param refreshToken The refresh token
     * @return New token response
     */
    public Map<String, Object> refreshAccessToken(String refreshToken) {
        String tokenUrl = AUTH_BASE_URL + "/oauth/token";

        // Build the refresh token request (without client credentials in body)
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "refresh_token");
        params.add("refresh_token", refreshToken);

        // Set up headers with Basic Authentication
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setBasicAuth(clientId, clientSecret);  // Use Basic Auth instead of form params

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            log.info("Successfully refreshed Schwab access token");
            return response.getBody();
        } else {
            throw new RuntimeException("Token refresh failed with status: " + response.getStatusCode());
        }
    }
    
    /**
     * Get user account information from Schwab.
     * 
     * @param accessToken The user's access token
     * @return Account information
     */
    public Map<String, Object> getUserAccountInfo(String accessToken) {
        return makeAuthenticatedRequest("/trader/v1/accounts", accessToken, null);
    }
    
    /**
     * Get account positions from Schwab.
     * 
     * @param accessToken The user's access token
     * @param accountHash The account hash/ID
     * @return Account positions
     */
    public Map<String, Object> getAccountPositions(String accessToken, String accountHash) {
        String endpoint = String.format("/trader/v1/accounts/%s/positions", accountHash);
        return makeAuthenticatedRequest(endpoint, accessToken, null);
    }
    
    /**
     * Get account balance from Schwab.
     * 
     * @param accessToken The user's access token
     * @param accountHash The account hash/ID
     * @return Account balance information
     */
    public Map<String, Object> getAccountBalance(String accessToken, String accountHash) {
        String endpoint = String.format("/trader/v1/accounts/%s", accountHash);
        return makeAuthenticatedRequest(endpoint, accessToken, null);
    }
    
    /**
     * Make authenticated request to Schwab API.
     * 
     * @param endpoint The API endpoint
     * @param accessToken The access token
     * @param params Optional query parameters
     * @return API response
     */
    private Map<String, Object> makeAuthenticatedRequest(String endpoint, String accessToken, Map<String, String> params) {
        String url = DEFAULT_BASE_URL + endpoint;
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(accessToken);
        
        HttpEntity<String> request = new HttpEntity<>(headers);
        
        log.debug("Making authenticated request to Schwab API: {}", endpoint);
        
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
        
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody();
        } else {
            throw new RuntimeException("Authenticated request failed with status: " + response.getStatusCode());
        }
    }
    
    /**
     * Test connection to Schwab API using access token.
     * 
     * @param accessToken The access token to test
     * @return true if connection is valid
     */
    public boolean testConnection(String accessToken) {
        try {
            Map<String, Object> accounts = makeAuthenticatedRequest("/trader/v1/accounts", accessToken, null);
            return accounts != null && !accounts.isEmpty();
        } catch (Exception e) {
            log.debug("Schwab connection test failed: {}", e.getMessage());
            return false;
        }
    }
}