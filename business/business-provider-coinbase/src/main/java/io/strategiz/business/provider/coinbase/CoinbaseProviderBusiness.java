package io.strategiz.business.provider.coinbase;

import io.strategiz.business.provider.coinbase.model.CoinbaseConnectionResult;
import io.strategiz.business.provider.coinbase.model.CoinbaseDisconnectionResult;
import io.strategiz.business.provider.coinbase.model.CoinbaseTokenRefreshResult;
import io.strategiz.client.coinbase.CoinbaseClient;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.framework.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Business logic for Coinbase provider integration.
 * Handles OAuth flows, API interactions, and business rules specific to Coinbase.
 */
@Component
public class CoinbaseProviderBusiness {
    
    private static final Logger log = LoggerFactory.getLogger(CoinbaseProviderBusiness.class);
    
    private final CoinbaseClient coinbaseClient;
    
    // OAuth Configuration
    @Value("${oauth.providers.coinbase.client-id}")
    private String clientId;
    
    @Value("${oauth.providers.coinbase.client-secret}")
    private String clientSecret;
    
    @Value("${oauth.providers.coinbase.redirect-uri}")
    private String redirectUri;
    
    @Value("${oauth.providers.coinbase.auth-url:https://www.coinbase.com/oauth/authorize}")
    private String authUrl;
    
    @Value("${oauth.providers.coinbase.scope:wallet:accounts:read,wallet:transactions:read,wallet:buys:read,wallet:sells:read}")
    private String scope;

    @Autowired
    public CoinbaseProviderBusiness(CoinbaseClient coinbaseClient) {
        this.coinbaseClient = coinbaseClient;
    }

    /**
     * Generate OAuth authorization URL for Coinbase
     * 
     * @param userId The user requesting authorization
     * @param state Security state parameter
     * @return OAuth authorization URL
     * @throws StrategizException if OAuth configuration is invalid
     */
    public String generateAuthorizationUrl(String userId, String state) {
        validateOAuthConfiguration();
        
        String authorizationUrl = String.format(
            "%s?client_id=%s&redirect_uri=%s&state=%s&scope=%s&response_type=code",
            authUrl,
            clientId,
            redirectUri,
            state,
            scope
        );
        
        log.info("Generated Coinbase OAuth URL for user: {}", userId);
        return authorizationUrl;
    }
    
    /**
     * Handle OAuth callback from Coinbase
     * 
     * @param userId The user completing OAuth
     * @param authorizationCode The authorization code from Coinbase
     * @param state The state parameter for validation
     * @return Connection result with tokens and account info
     * @throws StrategizException if OAuth exchange fails
     */
    public CoinbaseConnectionResult handleOAuthCallback(String userId, String authorizationCode, String state) {
        validateRequired("userId", userId);
        validateRequired("authorizationCode", authorizationCode);
        validateRequired("state", state);
        
        try {
            // Exchange authorization code for access token
            Map<String, Object> tokenData = exchangeCodeForTokens(authorizationCode);
            
            String accessToken = (String) tokenData.get("access_token");
            String refreshToken = (String) tokenData.get("refresh_token");
            Integer expiresIn = (Integer) tokenData.get("expires_in");
            
            // Get user account information from Coinbase
            Map<String, Object> accountInfo = getUserAccountInfo(accessToken);
            
            // Build connection result
            CoinbaseConnectionResult result = new CoinbaseConnectionResult();
            result.setUserId(userId);
            result.setProviderId("coinbase");
            result.setProviderName("Coinbase");
            result.setAccessToken(accessToken);
            result.setRefreshToken(refreshToken);
            result.setExpiresAt(Instant.now().plusSeconds(expiresIn != null ? expiresIn : 3600));
            result.setAccountInfo(accountInfo);
            result.setConnectedAt(Instant.now());
            result.setStatus("connected");
            
            log.info("Successfully connected Coinbase for user: {}", userId);
            return result;
            
        } catch (Exception e) {
            log.error("Failed to handle Coinbase OAuth callback for user: {}", userId, e);
            throw new StrategizException(ErrorCode.EXTERNAL_SERVICE_ERROR, 
                "Failed to complete Coinbase OAuth: " + e.getMessage());
        }
    }
    
    /**
     * Get account balance from Coinbase
     * 
     * @param userId The user requesting balance
     * @param accessToken The user's Coinbase access token
     * @return Account balance information
     * @throws StrategizException if balance retrieval fails
     */
    public Map<String, Object> getAccountBalance(String userId, String accessToken) {
        validateRequired("userId", userId);
        validateRequired("accessToken", accessToken);
        
        try {
            // Make authenticated request to Coinbase API
            Map<String, Object> balanceData = makeAuthenticatedRequest(
                "/accounts", 
                accessToken, 
                null
            );
            
            log.info("Retrieved account balance for user: {}", userId);
            return balanceData;
            
        } catch (Exception e) {
            log.error("Failed to get account balance for user: {}", userId, e);
            throw new StrategizException(ErrorCode.EXTERNAL_SERVICE_ERROR, 
                "Failed to get Coinbase account balance: " + e.getMessage());
        }
    }
    
    /**
     * Get transaction history from Coinbase
     * 
     * @param userId The user requesting transactions
     * @param accessToken The user's Coinbase access token
     * @param accountId The specific account ID (optional)
     * @param limit Number of transactions to retrieve
     * @return Transaction history
     * @throws StrategizException if transaction retrieval fails
     */
    public Map<String, Object> getTransactionHistory(String userId, String accessToken, String accountId, Integer limit) {
        validateRequired("userId", userId);
        validateRequired("accessToken", accessToken);
        
        try {
            String endpoint = accountId != null ? 
                String.format("/accounts/%s/transactions", accountId) : 
                "/transactions";
                
            Map<String, String> params = new HashMap<>();
            if (limit != null) {
                params.put("limit", limit.toString());
            }
            
            Map<String, Object> transactionData = makeAuthenticatedRequest(
                endpoint, 
                accessToken, 
                params
            );
            
            log.info("Retrieved transaction history for user: {}", userId);
            return transactionData;
            
        } catch (Exception e) {
            log.error("Failed to get transaction history for user: {}", userId, e);
            throw new StrategizException(ErrorCode.EXTERNAL_SERVICE_ERROR, 
                "Failed to get Coinbase transaction history: " + e.getMessage());
        }
    }
    
    /**
     * Refresh expired access token using refresh token
     * 
     * @param userId The user requesting token refresh
     * @param refreshToken The refresh token
     * @return New token data
     * @throws StrategizException if token refresh fails
     */
    public CoinbaseTokenRefreshResult refreshAccessToken(String userId, String refreshToken) {
        validateRequired("userId", userId);
        validateRequired("refreshToken", refreshToken);
        
        try {
            Map<String, Object> tokenData = refreshTokens(refreshToken);
            
            String newAccessToken = (String) tokenData.get("access_token");
            String newRefreshToken = (String) tokenData.get("refresh_token");
            Integer expiresIn = (Integer) tokenData.get("expires_in");
            
            CoinbaseTokenRefreshResult result = new CoinbaseTokenRefreshResult();
            result.setAccessToken(newAccessToken);
            result.setRefreshToken(newRefreshToken);
            result.setExpiresAt(Instant.now().plusSeconds(expiresIn != null ? expiresIn : 3600));
            result.setRefreshedAt(Instant.now());
            
            log.info("Successfully refreshed Coinbase tokens for user: {}", userId);
            return result;
            
        } catch (Exception e) {
            log.error("Failed to refresh Coinbase tokens for user: {}", userId, e);
            throw new StrategizException(ErrorCode.EXTERNAL_SERVICE_ERROR, 
                "Failed to refresh Coinbase tokens: " + e.getMessage());
        }
    }
    
    /**
     * Disconnect Coinbase provider for a user
     * 
     * @param userId The user disconnecting
     * @param accessToken The user's access token
     * @return Disconnection result
     * @throws StrategizException if disconnection fails
     */
    public CoinbaseDisconnectionResult disconnectProvider(String userId, String accessToken) {
        validateRequired("userId", userId);
        
        try {
            // Revoke access token if provided
            if (accessToken != null && !accessToken.trim().isEmpty()) {
                revokeAccessToken(accessToken);
            }
            
            CoinbaseDisconnectionResult result = new CoinbaseDisconnectionResult();
            result.setUserId(userId);
            result.setProviderId("coinbase");
            result.setDisconnectedAt(Instant.now());
            result.setStatus("disconnected");
            
            log.info("Successfully disconnected Coinbase for user: {}", userId);
            return result;
            
        } catch (Exception e) {
            log.error("Failed to disconnect Coinbase for user: {}", userId, e);
            throw new StrategizException(ErrorCode.EXTERNAL_SERVICE_ERROR, 
                "Failed to disconnect Coinbase: " + e.getMessage());
        }
    }
    
    /**
     * Test connection to Coinbase using access token
     * 
     * @param accessToken The access token to test
     * @return true if connection is valid
     */
    public boolean testConnection(String accessToken) {
        try {
            Map<String, Object> userInfo = makeAuthenticatedRequest("/user", accessToken, null);
            return userInfo != null && userInfo.containsKey("data");
        } catch (Exception e) {
            log.debug("Coinbase connection test failed: {}", e.getMessage());
            return false;
        }
    }
    
    // Private helper methods
    
    private void validateOAuthConfiguration() {
        if (clientId == null || clientId.trim().isEmpty()) {
            throw new StrategizException(ErrorCode.VALIDATION_ERROR, 
                "Coinbase OAuth client ID is not configured");
        }
        if (clientSecret == null || clientSecret.trim().isEmpty()) {
            throw new StrategizException(ErrorCode.VALIDATION_ERROR, 
                "Coinbase OAuth client secret is not configured");
        }
        if (redirectUri == null || redirectUri.trim().isEmpty()) {
            throw new StrategizException(ErrorCode.VALIDATION_ERROR, 
                "Coinbase OAuth redirect URI is not configured");
        }
    }
    
    private void validateRequired(String paramName, String paramValue) {
        if (paramValue == null || paramValue.trim().isEmpty()) {
            throw new StrategizException(ErrorCode.VALIDATION_ERROR, 
                "Required parameter '" + paramName + "' is missing or empty");
        }
    }
    
    private Map<String, Object> exchangeCodeForTokens(String authorizationCode) {
        try {
            // Build the token exchange request
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "authorization_code");
            params.add("code", authorizationCode);
            params.add("client_id", clientId);
            params.add("client_secret", clientSecret);
            params.add("redirect_uri", redirectUri);
            
            // Set up headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            
            // Create the request entity
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            
            // Make the token exchange request
            String tokenUrl = "https://api.coinbase.com/oauth/token";
            RestTemplate restTemplate = new RestTemplate();
            
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new StrategizException(ErrorCode.EXTERNAL_SERVICE_ERROR, 
                    "Token exchange failed with status: " + response.getStatusCode());
            }
            
        } catch (RestClientException e) {
            log.error("Failed to exchange authorization code for tokens: {}", e.getMessage());
            throw new StrategizException(ErrorCode.EXTERNAL_SERVICE_ERROR, 
                "Token exchange failed: " + e.getMessage());
        }
    }
    
    private Map<String, Object> getUserAccountInfo(String accessToken) {
        return makeAuthenticatedRequest("/user", accessToken, null);
    }
    
    private Map<String, Object> makeAuthenticatedRequest(String endpoint, String accessToken, Map<String, String> params) {
        try {
            // Build the URL with parameters
            UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromHttpUrl("https://api.coinbase.com/v2" + endpoint);
            
            if (params != null) {
                for (Map.Entry<String, String> param : params.entrySet()) {
                    uriBuilder.queryParam(param.getKey(), param.getValue());
                }
            }
            
            String url = uriBuilder.toUriString();
            
            // Set up headers with Bearer token
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.setBearerAuth(accessToken);
            
            // Create the request entity
            HttpEntity<String> request = new HttpEntity<>(headers);
            
            // Make the authenticated request
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new StrategizException(ErrorCode.EXTERNAL_SERVICE_ERROR, 
                    "Authenticated request failed with status: " + response.getStatusCode());
            }
            
        } catch (RestClientException e) {
            log.error("Failed to make authenticated request to {}: {}", endpoint, e.getMessage());
            throw new StrategizException(ErrorCode.EXTERNAL_SERVICE_ERROR, 
                "Authenticated request failed: " + e.getMessage());
        }
    }
    
    private Map<String, Object> refreshTokens(String refreshToken) {
        try {
            // Build the token refresh request
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "refresh_token");
            params.add("refresh_token", refreshToken);
            params.add("client_id", clientId);
            params.add("client_secret", clientSecret);
            
            // Set up headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            
            // Create the request entity
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            
            // Make the token refresh request
            String tokenUrl = "https://api.coinbase.com/oauth/token";
            RestTemplate restTemplate = new RestTemplate();
            
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new StrategizException(ErrorCode.EXTERNAL_SERVICE_ERROR, 
                    "Token refresh failed with status: " + response.getStatusCode());
            }
            
        } catch (RestClientException e) {
            log.error("Failed to refresh tokens: {}", e.getMessage());
            throw new StrategizException(ErrorCode.EXTERNAL_SERVICE_ERROR, 
                "Token refresh failed: " + e.getMessage());
        }
    }
    
    private void revokeAccessToken(String accessToken) {
        try {
            // Build the token revocation request
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("token", accessToken);
            
            // Set up headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            
            // Create the request entity
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            
            // Make the token revocation request
            String revokeUrl = "https://api.coinbase.com/oauth/revoke";
            RestTemplate restTemplate = new RestTemplate();
            
            ResponseEntity<String> response = restTemplate.postForEntity(revokeUrl, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully revoked Coinbase access token");
            } else {
                log.warn("Token revocation returned status: {}", response.getStatusCode());
            }
            
        } catch (RestClientException e) {
            log.error("Failed to revoke access token: {}", e.getMessage());
            // Don't throw exception for revocation failures - log and continue
        }
    }
} 