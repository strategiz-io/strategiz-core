package io.strategiz.business.provider.alpaca;

import io.strategiz.business.provider.alpaca.model.AlpacaConnectionResult;
import io.strategiz.business.provider.alpaca.model.AlpacaDisconnectionResult;
import io.strategiz.business.provider.alpaca.model.AlpacaTokenRefreshResult;
// import io.strategiz.data.auth.entity.ProviderIntegrationEntity;
// import io.strategiz.data.auth.repository.ProviderIntegrationRepository;
import io.strategiz.business.base.provider.model.CreateProviderIntegrationRequest;
import io.strategiz.business.base.provider.model.ProviderIntegrationResult;
import io.strategiz.business.base.provider.ProviderIntegrationHandler;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.framework.exception.ErrorCode;
import io.strategiz.framework.secrets.controller.SecretManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Business logic for Alpaca provider integration.
 * Handles OAuth flows, API interactions, and business rules specific to Alpaca.
 */
@Component
public class AlpacaProviderBusiness implements ProviderIntegrationHandler {
    
    private static final Logger log = LoggerFactory.getLogger(AlpacaProviderBusiness.class);
    
    private static final String PROVIDER_ID = "alpaca";
    private static final String PROVIDER_NAME = "Alpaca";
    private static final String PROVIDER_TYPE = "broker";
    
    // private final ProviderIntegrationRepository providerIntegrationRepository;
    private final SecretManager secretManager;
    
    // OAuth Configuration
    @Value("${oauth.providers.alpaca.client-id}")
    private String clientId;
    
    @Value("${oauth.providers.alpaca.client-secret}")
    private String clientSecret;
    
    @Value("${oauth.providers.alpaca.redirect-uri}")
    private String redirectUri;
    
    @Value("${oauth.providers.alpaca.auth-url:https://app.alpaca.markets/oauth/authorize}")
    private String authUrl;
    
    @Value("${oauth.providers.alpaca.token-url:https://api.alpaca.markets/oauth/token}")
    private String tokenUrl;
    
    @Value("${oauth.providers.alpaca.api-url:https://api.alpaca.markets}")
    private String apiUrl;
    
    @Value("${oauth.providers.alpaca.scope:account:read trading:write data:read}")
    private String scope;

    @Autowired
    public AlpacaProviderBusiness(
            // ProviderIntegrationRepository providerIntegrationRepository,
            @Qualifier("vaultSecretService") SecretManager secretManager) {
        // this.providerIntegrationRepository = providerIntegrationRepository;
        this.secretManager = secretManager;
    }

    /**
     * Generate OAuth authorization URL for Alpaca
     * 
     * @param userId The user requesting authorization
     * @param state Security state parameter
     * @return OAuth authorization URL
     * @throws StrategizException if OAuth configuration is invalid
     */
    public String generateAuthorizationUrl(String userId, String state) {
        validateOAuthConfiguration();
        
        try {
            // Properly URL-encode the redirect URI
            String encodedRedirectUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8.toString());
            String encodedScope = URLEncoder.encode(scope, StandardCharsets.UTF_8.toString());
            
            String authorizationUrl = String.format(
                "%s?response_type=code&client_id=%s&redirect_uri=%s&state=%s&scope=%s",
                authUrl,
                clientId,
                encodedRedirectUri,
                state,
                encodedScope
            );
            
            log.info("Generated Alpaca OAuth URL for user: {}", userId);
            log.debug("OAuth URL: {}", authorizationUrl);
            return authorizationUrl;
        } catch (Exception e) {
            log.error("Failed to generate OAuth URL", e);
            throw new RuntimeException("Failed to generate OAuth URL", e);
        }
    }
    
    /**
     * Handle OAuth callback from Alpaca
     * 
     * @param userId The user completing OAuth
     * @param authorizationCode The authorization code from Alpaca
     * @param state The state parameter for validation
     * @return Connection result with tokens and account info
     * @throws StrategizException if OAuth exchange fails
     */
    public AlpacaConnectionResult handleOAuthCallback(String userId, String authorizationCode, String state) {
        validateRequired("userId", userId);
        validateRequired("authorizationCode", authorizationCode);
        validateRequired("state", state);
        
        try {
            // Exchange authorization code for access token
            Map<String, Object> tokenData = exchangeCodeForTokens(authorizationCode);
            
            String accessToken = (String) tokenData.get("access_token");
            String refreshToken = (String) tokenData.get("refresh_token");
            Integer expiresIn = (Integer) tokenData.get("expires_in");
            
            // Get user account information from Alpaca
            Map<String, Object> accountInfo = getUserAccountInfo(accessToken);
            
            // Build connection result
            AlpacaConnectionResult result = new AlpacaConnectionResult();
            result.setUserId(userId);
            result.setProviderId("alpaca");
            result.setProviderName("Alpaca");
            result.setAccessToken(accessToken);
            result.setRefreshToken(refreshToken);
            result.setExpiresAt(Instant.now().plusSeconds(expiresIn != null ? expiresIn : 3600));
            result.setAccountInfo(accountInfo);
            result.setConnectedAt(Instant.now());
            result.setStatus("connected");
            
            // Determine if this is paper or live trading
            String environment = determineEnvironment(accountInfo);
            result.setEnvironment(environment);
            
            log.info("Successfully connected Alpaca ({}) for user: {}", environment, userId);
            return result;
            
        } catch (Exception e) {
            log.error("Failed to handle Alpaca OAuth callback for user: {}", userId, e);
            throw new StrategizException(ErrorCode.EXTERNAL_SERVICE_ERROR, 
                "Failed to complete Alpaca OAuth: " + e.getMessage());
        }
    }
    
    /**
     * Get account information from Alpaca
     * 
     * @param userId The user requesting account info
     * @param accessToken The user's Alpaca access token
     * @return Account information
     * @throws StrategizException if account retrieval fails
     */
    public Map<String, Object> getAccountInfo(String userId, String accessToken) {
        validateRequired("userId", userId);
        validateRequired("accessToken", accessToken);
        
        try {
            Map<String, Object> accountData = makeAuthenticatedRequest(
                "/v2/account", 
                accessToken, 
                null
            );
            
            log.info("Retrieved account info for user: {}", userId);
            return accountData;
            
        } catch (Exception e) {
            log.error("Failed to get account info for user: {}", userId, e);
            throw new StrategizException(ErrorCode.EXTERNAL_SERVICE_ERROR, 
                "Failed to get Alpaca account info: " + e.getMessage());
        }
    }
    
    /**
     * Get portfolio positions from Alpaca
     * 
     * @param userId The user requesting positions
     * @param accessToken The user's Alpaca access token
     * @return Portfolio positions
     * @throws StrategizException if positions retrieval fails
     */
    public Map<String, Object> getPositions(String userId, String accessToken) {
        validateRequired("userId", userId);
        validateRequired("accessToken", accessToken);
        
        try {
            Map<String, Object> positionsData = makeAuthenticatedRequest(
                "/v2/positions", 
                accessToken, 
                null
            );
            
            log.info("Retrieved positions for user: {}", userId);
            return positionsData;
            
        } catch (Exception e) {
            log.error("Failed to get positions for user: {}", userId, e);
            throw new StrategizException(ErrorCode.EXTERNAL_SERVICE_ERROR, 
                "Failed to get Alpaca positions: " + e.getMessage());
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
    public AlpacaTokenRefreshResult refreshAccessToken(String userId, String refreshToken) {
        validateRequired("userId", userId);
        validateRequired("refreshToken", refreshToken);
        
        try {
            Map<String, Object> tokenData = refreshTokens(refreshToken);
            
            String newAccessToken = (String) tokenData.get("access_token");
            String newRefreshToken = (String) tokenData.get("refresh_token");
            Integer expiresIn = (Integer) tokenData.get("expires_in");
            
            AlpacaTokenRefreshResult result = new AlpacaTokenRefreshResult();
            result.setAccessToken(newAccessToken);
            result.setRefreshToken(newRefreshToken);
            result.setExpiresAt(Instant.now().plusSeconds(expiresIn != null ? expiresIn : 3600));
            result.setRefreshedAt(Instant.now());
            
            log.info("Successfully refreshed Alpaca tokens for user: {}", userId);
            return result;
            
        } catch (Exception e) {
            log.error("Failed to refresh Alpaca tokens for user: {}", userId, e);
            throw new StrategizException(ErrorCode.EXTERNAL_SERVICE_ERROR, 
                "Failed to refresh Alpaca tokens: " + e.getMessage());
        }
    }
    
    /**
     * Disconnect Alpaca provider for a user
     * 
     * @param userId The user disconnecting
     * @param accessToken The user's access token
     * @return Disconnection result
     * @throws StrategizException if disconnection fails
     */
    public AlpacaDisconnectionResult disconnectProvider(String userId, String accessToken) {
        validateRequired("userId", userId);
        
        try {
            // Revoke access token if provided
            if (accessToken != null && !accessToken.trim().isEmpty()) {
                revokeAccessToken(accessToken);
            }
            
            AlpacaDisconnectionResult result = new AlpacaDisconnectionResult();
            result.setUserId(userId);
            result.setProviderId("alpaca");
            result.setDisconnectedAt(Instant.now());
            result.setStatus("disconnected");
            
            log.info("Successfully disconnected Alpaca for user: {}", userId);
            return result;
            
        } catch (Exception e) {
            log.error("Failed to disconnect Alpaca for user: {}", userId, e);
            throw new StrategizException(ErrorCode.EXTERNAL_SERVICE_ERROR, 
                "Failed to disconnect Alpaca: " + e.getMessage());
        }
    }
    
    /**
     * Test connection to Alpaca using access token
     * 
     * @param accessToken The access token to test
     * @return true if connection is valid
     */
    public boolean testConnection(String accessToken) {
        try {
            Map<String, Object> accountInfo = makeAuthenticatedRequest("/v2/account", accessToken, null);
            return accountInfo != null && accountInfo.containsKey("id");
        } catch (Exception e) {
            log.debug("Alpaca connection test failed: {}", e.getMessage());
            return false;
        }
    }
    
    // Private helper methods
    
    private void validateOAuthConfiguration() {
        if (clientId == null || clientId.trim().isEmpty()) {
            throw new StrategizException(ErrorCode.VALIDATION_ERROR, 
                "Alpaca OAuth client ID is not configured");
        }
        if (clientSecret == null || clientSecret.trim().isEmpty()) {
            throw new StrategizException(ErrorCode.VALIDATION_ERROR, 
                "Alpaca OAuth client secret is not configured");
        }
        if (redirectUri == null || redirectUri.trim().isEmpty()) {
            throw new StrategizException(ErrorCode.VALIDATION_ERROR, 
                "Alpaca OAuth redirect URI is not configured");
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
        return makeAuthenticatedRequest("/v2/account", accessToken, null);
    }
    
    private String determineEnvironment(Map<String, Object> accountInfo) {
        // Check if this is a paper trading account
        if (accountInfo != null && accountInfo.containsKey("trading_blocked")) {
            Boolean tradingBlocked = (Boolean) accountInfo.get("trading_blocked");
            if (tradingBlocked != null && tradingBlocked) {
                return "paper";
            }
        }
        // Check for pattern matching on account status
        Object status = accountInfo.get("status");
        if (status != null && status.toString().toLowerCase().contains("paper")) {
            return "paper";
        }
        return "live";
    }
    
    private Map<String, Object> makeAuthenticatedRequest(String endpoint, String accessToken, Map<String, String> params) {
        try {
            // Build the URL with parameters
            UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromHttpUrl(apiUrl + endpoint);
            
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
            // Alpaca doesn't have a specific revoke endpoint in the OAuth spec
            // But we can make a request to invalidate the token
            log.info("Revoking Alpaca access token (logout)");
            // For now, we'll just log that we're disconnecting
            // In a real implementation, you might call a logout endpoint if available
            
        } catch (Exception e) {
            log.error("Failed to revoke access token: {}", e.getMessage());
            // Don't throw exception for revocation failures - log and continue
        }
    }
    
    // ProviderIntegrationHandler interface implementation
    
    @Override
    public boolean testConnection(CreateProviderIntegrationRequest request, String userId) {
        log.info("Testing Alpaca connection for user: {}", userId);
        
        // Alpaca uses OAuth, so we can't test with API keys
        // Return true to indicate OAuth flow should be initiated
        log.info("Alpaca requires OAuth flow - cannot test with API keys");
        return true;
    }
    
    @Override
    public ProviderIntegrationResult createIntegration(CreateProviderIntegrationRequest request, String userId) {
        log.info("Creating Alpaca integration for user: {}", userId);
        
        // For OAuth providers, we don't store credentials during signup
        // Instead, we'll initiate the OAuth flow
        try {
            // Create provider integration entity for Firestore
            // ProviderIntegrationEntity entity = new ProviderIntegrationEntity(
            //     PROVIDER_ID, PROVIDER_NAME, PROVIDER_TYPE);
            // 
            // entity.setStatus("pending_oauth");
            // entity.setEnabled(false); // Will be enabled after OAuth completion
            // entity.setSupportsTrading(true);
            // entity.setPermissions(Arrays.asList("read", "trade"));
            // entity.putMetadata("oauthRequired", true);
            // entity.putMetadata("authMethod", "oauth2");
            // entity.putMetadata("scope", scope);
            // 
            // // Save to Firestore user subcollection
            // ProviderIntegrationEntity savedEntity = providerIntegrationRepository.saveForUser(userId, entity);
            // log.info("Created pending Alpaca provider integration for user: {}", userId);
            
            // Generate OAuth URL for the response
            String state = generateSecureState(userId);
            String authUrl = generateAuthorizationUrl(userId, state);
            
            // Build result with OAuth URL
            Map<String, Object> metadata = new HashMap<>(); // new HashMap<>(savedEntity.getMetadata());
            metadata.put("oauthUrl", authUrl);
            metadata.put("state", state);
            metadata.put("oauthRequired", true);
            metadata.put("authMethod", "oauth2");
            metadata.put("scope", scope);
            
            ProviderIntegrationResult result = new ProviderIntegrationResult();
            result.setSuccess(true);
            result.setMessage("OAuth authorization required");
            result.setMetadata(metadata);
            return result;
                
        } catch (Exception e) {
            log.error("Error creating Alpaca integration for user: {}", userId, e);
            throw new RuntimeException("Failed to create Alpaca integration", e);
        }
    }
    
    @Override
    public String getProviderId() {
        return PROVIDER_ID;
    }
    
    /**
     * Complete OAuth flow and store tokens using dual storage
     */
    public void completeOAuthFlow(String userId, String authorizationCode, String state) {
        log.info("Completing Alpaca OAuth flow for user: {}", userId);
        
        try {
            // Handle OAuth callback
            AlpacaConnectionResult result = handleOAuthCallback(userId, authorizationCode, state);
            
            // Store tokens in Vault
            storeTokensInVault(userId, result.getAccessToken(), result.getRefreshToken(), result.getExpiresAt());
            
            // Update provider integration in Firestore
            // var existingIntegration = providerIntegrationRepository.findByUserIdAndProviderId(userId, PROVIDER_ID);
            // if (existingIntegration.isPresent()) {
            //     ProviderIntegrationEntity entity = existingIntegration.get();
            //     entity.setStatus("connected");
            //     entity.setEnabled(true);
            //     entity.setConnectedAt(result.getConnectedAt());
            //     entity.setLastTestedAt(Instant.now());
            //     entity.putMetadata("accountInfo", result.getAccountInfo());
            //     entity.putMetadata("environment", result.getEnvironment());
            //     entity.putMetadata("oauthCompleted", true);
            //     
            //     providerIntegrationRepository.saveForUser(userId, entity);
            //     log.info("Updated Alpaca integration status to connected for user: {}", userId);
            // }
            
        } catch (Exception e) {
            log.error("Error completing Alpaca OAuth flow for user: {}", userId, e);
            throw new RuntimeException("Failed to complete OAuth flow", e);
        }
    }
    
    private void storeTokensInVault(String userId, String accessToken, String refreshToken, Instant expiresAt) {
        try {
            String secretPath = "secret/strategiz/users/" + userId + "/providers/alpaca";
            
            Map<String, Object> secretData = new HashMap<>();
            secretData.put("accessToken", accessToken);
            secretData.put("refreshToken", refreshToken);
            secretData.put("expiresAt", expiresAt.toString());
            secretData.put("provider", PROVIDER_ID);
            secretData.put("storedAt", Instant.now().toString());
            
            // Convert map to JSON string for storage
            ObjectMapper objectMapper = new ObjectMapper();
            String secretJson = objectMapper.writeValueAsString(secretData);
            secretManager.createSecret(secretPath, secretJson);
            log.debug("Stored Alpaca OAuth tokens in Vault for user: {}", userId);
            
        } catch (Exception e) {
            log.error("Failed to store Alpaca tokens for user: {}", userId, e);
            throw new RuntimeException("Failed to store tokens", e);
        }
    }
    
    private String generateSecureState(String userId) {
        // Generate a secure random state parameter
        return userId + "-" + UUID.randomUUID().toString();
    }
}