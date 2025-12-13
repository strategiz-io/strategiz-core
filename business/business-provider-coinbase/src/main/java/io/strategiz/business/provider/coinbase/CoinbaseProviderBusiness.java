package io.strategiz.business.provider.coinbase;

import io.strategiz.business.provider.coinbase.model.CoinbaseConnectionResult;
import io.strategiz.business.provider.coinbase.model.CoinbaseDisconnectionResult;
import io.strategiz.business.provider.coinbase.model.CoinbaseTokenRefreshResult;
import io.strategiz.client.coinbase.CoinbaseClient;
import io.strategiz.client.coinbase.CoinbaseDataClient;
import io.strategiz.client.coinbase.CoinbaseOAuthClient;
import io.strategiz.data.provider.entity.ProviderIntegrationEntity;
import io.strategiz.data.provider.entity.ProviderStatus;
import io.strategiz.data.provider.entity.ProviderDataEntity;
import io.strategiz.data.provider.repository.CreateProviderIntegrationRepository;
import io.strategiz.data.provider.repository.ReadProviderIntegrationRepository;
import io.strategiz.data.provider.repository.UpdateProviderIntegrationRepository;
import io.strategiz.data.provider.repository.CreateProviderDataRepository;
import io.strategiz.data.provider.repository.ReadProviderDataRepository;
import io.strategiz.data.provider.repository.UpdateProviderDataRepository;
import io.strategiz.business.base.provider.model.CreateProviderIntegrationRequest;
import io.strategiz.business.base.provider.model.ProviderIntegrationResult;
import io.strategiz.business.base.provider.ProviderIntegrationHandler;
import io.strategiz.business.portfolio.PortfolioSummaryManager;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.framework.exception.ErrorCode;
import io.strategiz.framework.secrets.controller.SecretManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.util.UriComponentsBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Business logic for Coinbase provider integration.
 * Handles OAuth flows, API interactions, and business rules specific to Coinbase.
 */
@Component
public class CoinbaseProviderBusiness implements ProviderIntegrationHandler {
    
    private static final Logger log = LoggerFactory.getLogger(CoinbaseProviderBusiness.class);
    
    private static final String PROVIDER_ID = "coinbase";
    private static final String PROVIDER_NAME = "Coinbase";
    private static final String PROVIDER_TYPE = "crypto";
    private static final String PROVIDER_CATEGORY = "exchange";

    private final CoinbaseClient coinbaseClient;
    private final CoinbaseDataClient coinbaseDataClient;
    private final CoinbaseOAuthClient coinbaseOAuthClient;
    private final CreateProviderIntegrationRepository createProviderIntegrationRepository;
    private final ReadProviderIntegrationRepository readProviderIntegrationRepository;
    private final UpdateProviderIntegrationRepository updateProviderIntegrationRepository;
    private final CreateProviderDataRepository createProviderDataRepository;
    private final ReadProviderDataRepository readProviderDataRepository;
    private final UpdateProviderDataRepository updateProviderDataRepository;
    private final SecretManager secretManager;

    @Autowired(required = false)
    private PortfolioSummaryManager portfolioSummaryManager;

    // OAuth Configuration
    @Value("${oauth.providers.coinbase.client-id:}")
    private String clientId;
    
    @Value("${oauth.providers.coinbase.client-secret:}")
    private String clientSecret;
    
    @Value("${oauth.providers.coinbase.redirect-uri}")
    private String redirectUri;
    
    @Value("${oauth.providers.coinbase.auth-url:https://www.coinbase.com/oauth/authorize}")
    private String authUrl;
    
    @Value("${oauth.providers.coinbase.scope:wallet:accounts:read,wallet:transactions:read,wallet:buys:read,wallet:sells:read}")
    private String scope;

    @Autowired
    public CoinbaseProviderBusiness(
            CoinbaseClient coinbaseClient,
            CoinbaseDataClient coinbaseDataClient,
            CoinbaseOAuthClient coinbaseOAuthClient,
            CreateProviderIntegrationRepository createProviderIntegrationRepository,
            ReadProviderIntegrationRepository readProviderIntegrationRepository,
            UpdateProviderIntegrationRepository updateProviderIntegrationRepository,
            @Autowired(required = false) CreateProviderDataRepository createProviderDataRepository,
            @Autowired(required = false) ReadProviderDataRepository readProviderDataRepository,
            @Autowired(required = false) UpdateProviderDataRepository updateProviderDataRepository,
            @Qualifier("vaultSecretService") SecretManager secretManager) {
        this.coinbaseClient = coinbaseClient;
        this.coinbaseDataClient = coinbaseDataClient;
        this.coinbaseOAuthClient = coinbaseOAuthClient;
        this.createProviderIntegrationRepository = createProviderIntegrationRepository;
        this.readProviderIntegrationRepository = readProviderIntegrationRepository;
        this.updateProviderIntegrationRepository = updateProviderIntegrationRepository;
        this.createProviderDataRepository = createProviderDataRepository;
        this.readProviderDataRepository = readProviderDataRepository;
        this.updateProviderDataRepository = updateProviderDataRepository;
        this.secretManager = secretManager;
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
            
        } catch (StrategizException e) {
            log.error("Failed to handle Coinbase OAuth callback for user: {}: {}", userId, e.getMessage());
            // Re-throw StrategizException with improved error message
            throw new StrategizException(ErrorCode.EXTERNAL_SERVICE_ERROR, 
                "Failed to complete Coinbase OAuth: " + e.getMessage());
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
            
        } catch (StrategizException e) {
            log.error("Failed to get account balance for user: {}: {}", userId, e.getMessage());
            // Re-throw StrategizException with context
            throw new StrategizException(ErrorCode.EXTERNAL_SERVICE_ERROR, 
                "Failed to get Coinbase account balance: " + e.getMessage());
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
            // Use the CoinbaseOAuthClient to exchange the code for tokens
            log.info("Exchanging authorization code for tokens using CoinbaseOAuthClient");
            return coinbaseOAuthClient.exchangeCodeForTokens(authorizationCode);
        } catch (StrategizException e) {
            // Re-throw StrategizException with additional context
            log.error("Failed to exchange authorization code for tokens: {}", e.getMessage());
            throw new StrategizException(ErrorCode.EXTERNAL_SERVICE_ERROR, 
                "Token exchange failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during token exchange: {}", e.getMessage());
            throw new StrategizException(ErrorCode.EXTERNAL_SERVICE_ERROR, 
                "Token exchange failed: " + e.getMessage());
        }
    }
    
    private Map<String, Object> getUserAccountInfo(String accessToken) {
        return makeAuthenticatedRequest("/user", accessToken, null);
    }
    
    private Map<String, Object> makeAuthenticatedRequest(String endpoint, String accessToken, Map<String, String> params) {
        try {
            // Use the CoinbaseClient to make authenticated requests
            log.debug("Making authenticated request to endpoint: {}", endpoint);
            
            // Determine which client method to call based on the endpoint
            if (endpoint.equals("/user")) {
                return coinbaseClient.getCurrentUser(accessToken);
            } else if (endpoint.equals("/accounts")) {
                return coinbaseClient.getAccounts(accessToken);
            } else if (endpoint.startsWith("/accounts/") && endpoint.contains("/transactions")) {
                String accountId = endpoint.split("/")[2];
                return coinbaseClient.getTransactions(accessToken, accountId, params);
            } else if (endpoint.startsWith("/accounts/")) {
                String accountId = endpoint.split("/")[2];
                return coinbaseClient.getAccount(accessToken, accountId);
            } else {
                // For other endpoints, use the generic OAuth request method
                return coinbaseClient.oauthRequest(
                    HttpMethod.GET,
                    endpoint,
                    accessToken,
                    params,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
                );
            }
        } catch (StrategizException e) {
            log.error("Failed to make authenticated request to {}: {}", endpoint, e.getMessage());
            throw e; // Re-throw StrategizException as-is
        } catch (Exception e) {
            log.error("Unexpected error making authenticated request to {}: {}", endpoint, e.getMessage());
            throw new StrategizException(ErrorCode.EXTERNAL_SERVICE_ERROR, 
                "Authenticated request failed: " + e.getMessage());
        }
    }
    
    private Map<String, Object> refreshTokens(String refreshToken) {
        try {
            // Use the CoinbaseOAuthClient to refresh the token
            log.info("Refreshing access token using CoinbaseOAuthClient");
            return coinbaseOAuthClient.refreshAccessToken(refreshToken);
        } catch (StrategizException e) {
            // Re-throw StrategizException with additional context
            log.error("Failed to refresh access token: {}", e.getMessage());
            throw new StrategizException(ErrorCode.EXTERNAL_SERVICE_ERROR, 
                "Token refresh failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during token refresh: {}", e.getMessage());
            throw new StrategizException(ErrorCode.EXTERNAL_SERVICE_ERROR, 
                "Token refresh failed: " + e.getMessage());
        }
    }
    
    private void revokeAccessToken(String accessToken) {
        try {
            // Use the CoinbaseOAuthClient to revoke the token
            log.info("Revoking access token using CoinbaseOAuthClient");
            boolean success = coinbaseOAuthClient.revokeAccessToken(accessToken);
            
            if (success) {
                log.info("Successfully revoked Coinbase access token");
            } else {
                log.warn("Token revocation may have failed, but continuing anyway");
            }
        } catch (Exception e) {
            log.error("Failed to revoke access token: {}", e.getMessage());
            // Don't throw exception for revocation failures - log and continue
        }
    }
    
    // ProviderIntegrationHandler interface implementation
    
    @Override
    public boolean testConnection(CreateProviderIntegrationRequest request, String userId) {
        log.info("Testing Coinbase connection for user: {}", userId);
        
        // Coinbase uses OAuth, so we can't test with API keys
        // Return true to indicate OAuth flow should be initiated
        log.info("Coinbase requires OAuth flow - cannot test with API keys");
        return true;
    }
    
    @Override
    public ProviderIntegrationResult createIntegration(CreateProviderIntegrationRequest request, String userId) {
        log.info("Creating Coinbase integration for user: {}", userId);
        log.info("OAuth configuration - clientId: {}, redirectUri: {}", 
                clientId != null ? "configured" : "null", 
                redirectUri != null ? redirectUri : "null");
        
        // For OAuth providers, we don't store credentials during signup
        // Instead, we'll initiate the OAuth flow
        try {
            // Create simplified provider integration entity for Firestore
            // Only store essential fields: providerId, connectionType, isEnabled
            ProviderIntegrationEntity entity = new ProviderIntegrationEntity(PROVIDER_ID, "oauth", userId);
            entity.setStatus("disconnected"); // Not connected until OAuth is complete
            
            // Save to Firestore
            ProviderIntegrationEntity savedEntity = createProviderIntegrationRepository.createForUser(entity, userId);
            log.info("Created pending Coinbase provider integration for user: {}", userId);
            
            // Generate OAuth URL for the response
            String state = generateSecureState(userId);
            String authUrl = generateAuthorizationUrl(userId, state);
            log.info("Generated OAuth URL: {}", authUrl);
            
            // Build result with OAuth URL
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("oauthUrl", authUrl);
            metadata.put("state", state);
            log.info("Returning metadata with oauthUrl: {}", metadata.get("oauthUrl"));
            metadata.put("oauthRequired", true);
            metadata.put("authMethod", "oauth2");
            metadata.put("scope", scope);
            
            ProviderIntegrationResult result = new ProviderIntegrationResult();
            result.setSuccess(true);
            result.setMessage("OAuth URL generated successfully");
            result.setMetadata(metadata);
            return result;
                
        } catch (Exception e) {
            log.error("Error creating Coinbase integration for user: {}", userId, e);
            throw new RuntimeException("Failed to create Coinbase integration", e);
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
        log.info("Completing Coinbase OAuth flow for user: {}", userId);

        try {
            // Handle OAuth callback
            CoinbaseConnectionResult result = handleOAuthCallback(userId, authorizationCode, state);

            // Store tokens in Vault
            storeTokensInVault(userId, result.getAccessToken(), result.getRefreshToken(), result.getExpiresAt());

            // Create or update provider integration in Firestore
            Optional<ProviderIntegrationEntity> existingIntegration = readProviderIntegrationRepository.findByUserIdAndProviderId(userId, PROVIDER_ID);

            if (existingIntegration.isPresent()) {
                // Update existing integration - just enable it
                ProviderIntegrationEntity entity = existingIntegration.get();
                entity.setStatus("connected"); // Mark as connected

                // We don't store tokens or metadata in the entity anymore
                // Those should be stored securely elsewhere (e.g., Vault)

                updateProviderIntegrationRepository.updateWithUserId(entity, userId);
                log.info("Updated Coinbase integration status to connected for user: {}", userId);
            } else {
                // Create new integration with simplified entity
                ProviderIntegrationEntity entity = new ProviderIntegrationEntity(PROVIDER_ID, "oauth", userId);
                entity.setStatus("connected"); // Mark as connected

                // We don't store tokens or metadata in the entity anymore
                // Those should be stored securely elsewhere (e.g., Vault)

                ProviderIntegrationEntity savedEntity = createProviderIntegrationRepository.createForUser(entity, userId);
                log.info("Created new Coinbase integration for user: {}", userId);
            }

            // Fetch and store portfolio data after OAuth completion (synchronous, like Kraken)
            // This MUST succeed for the signup flow to complete properly
            fetchAndStorePortfolioData(userId, result.getAccessToken());
            log.info("Successfully fetched and stored Coinbase portfolio data during OAuth for user: {}", userId);

        } catch (Exception e) {
            log.error("Error completing Coinbase OAuth flow for user: {}", userId, e);
            throw new RuntimeException("Failed to complete OAuth flow", e);
        }
    }

    /**
     * Sync provider data for a specific user
     * Retrieves access token from Vault and fetches latest portfolio data from Coinbase
     *
     * @param userId The user ID
     * @return ProviderDataEntity with synced data
     * @throws RuntimeException if sync fails
     */
    public ProviderDataEntity syncProviderData(String userId) {
        log.info("Syncing Coinbase provider data for user: {}", userId);

        try {
            // Retrieve access token from Vault
            String secretPath = "secret/strategiz/users/" + userId + "/providers/coinbase";
            Map<String, Object> secretData = secretManager.readSecretAsMap(secretPath);

            if (secretData == null || secretData.isEmpty()) {
                throw new RuntimeException("No Coinbase tokens found in Vault for user: " + userId);
            }

            String accessToken = (String) secretData.get("accessToken");
            if (accessToken == null || accessToken.isEmpty()) {
                throw new RuntimeException("Access token not found in Vault for user: " + userId);
            }

            log.debug("Retrieved Coinbase access token from Vault for user: {}", userId);

            // Fetch and store portfolio data (same process as during provider connection)
            fetchAndStorePortfolioData(userId, accessToken);

            // Retrieve and return the stored data
            ProviderDataEntity providerData = readProviderDataRepository.getProviderData(userId, PROVIDER_ID);

            if (providerData != null) {
                log.info("Successfully synced Coinbase data for user: {}", userId);
                return providerData;
            } else {
                log.warn("No provider data found after sync for user: {}", userId);
                throw new RuntimeException("No data found after sync");
            }

        } catch (Exception e) {
            log.error("Failed to sync Coinbase provider data for user: {}", userId, e);
            throw new RuntimeException("Failed to sync provider data: " + e.getMessage(), e);
        }
    }

    private void storeTokensInVault(String userId, String accessToken, String refreshToken, Instant expiresAt) {
        try {
            String secretPath = "secret/strategiz/users/" + userId + "/providers/coinbase";
            
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
            log.debug("Stored Coinbase OAuth tokens in Vault for user: {}", userId);
            
        } catch (Exception e) {
            log.error("Failed to store Coinbase tokens for user: {}", userId, e);
            throw new RuntimeException("Failed to store tokens", e);
        }
    }
    
    private String generateSecureState(String userId) {
        // Generate a secure random state parameter
        return userId + "-" + UUID.randomUUID().toString();
    }

    /**
     * Fetch portfolio data from Coinbase and store in provider_data collection
     * This is called after OAuth completion to pre-populate the dashboard
     */
    private void fetchAndStorePortfolioData(String userId, String accessToken) {
        log.info("Fetching Coinbase portfolio data for user: {}", userId);

        if (createProviderDataRepository == null) {
            log.warn("Provider data repository not available - skipping portfolio data fetch");
            return;
        }

        try {
            // Fetch accounts from Coinbase
            Map<String, Object> accountsResponse = coinbaseClient.getAccounts(accessToken);

            // Transform to ProviderDataEntity with Holdings (pass access token for price fetching)
            ProviderDataEntity portfolioData = transformToProviderDataEntity(accountsResponse, userId, accessToken);

            // Store in Firestore
            storeProviderData(userId, portfolioData);

            log.info("Successfully stored Coinbase portfolio data for user: {} with {} holdings",
                userId, portfolioData.getHoldings() != null ? portfolioData.getHoldings().size() : 0);

        } catch (Exception e) {
            log.error("Failed to fetch and store Coinbase portfolio data for user: {}", userId, e);
            throw new RuntimeException("Failed to fetch Coinbase portfolio data: " + e.getMessage(), e);
        }
    }

    /**
     * Transform Coinbase API accounts response to ProviderDataEntity
     * @param accountsResponse The accounts response from Coinbase API
     * @param userId The user ID
     * @param accessToken OAuth access token for fetching prices
     */
    private ProviderDataEntity transformToProviderDataEntity(Map<String, Object> accountsResponse, String userId, String accessToken) {
        ProviderDataEntity entity = new ProviderDataEntity();
        entity.setProviderId(PROVIDER_ID);
        entity.setProviderName(PROVIDER_NAME);
        entity.setProviderType(PROVIDER_TYPE);
        entity.setProviderCategory(PROVIDER_CATEGORY);
        entity.setSyncStatus("success");
        entity.setLastUpdatedAt(Instant.now());

        List<ProviderDataEntity.Holding> holdings = new ArrayList<>();
        BigDecimal totalValue = BigDecimal.ZERO;
        BigDecimal cashBalance = BigDecimal.ZERO;

        if (accountsResponse == null || !accountsResponse.containsKey("data")) {
            log.warn("No account data in Coinbase response");
            entity.setHoldings(holdings);
            entity.setTotalValue(BigDecimal.ZERO);
            entity.setCashBalance(BigDecimal.ZERO);
            return entity;
        }

        List<Map<String, Object>> accounts = (List<Map<String, Object>>) accountsResponse.get("data");

        for (Map<String, Object> account : accounts) {
            try {
                // DEBUG: Log the entire account object to see what fields are available
                log.info("=== COINBASE ACCOUNT DATA ===");
                log.info("Account fields: {}", account.keySet());
                log.info("Full account data: {}", account);

                // Extract account data - currency can be either String or Object
                String currency = null;
                Object currencyObj = account.get("currency");
                if (currencyObj instanceof String) {
                    currency = (String) currencyObj;
                } else if (currencyObj instanceof Map) {
                    // Coinbase returns currency as an object with 'code' and 'name'
                    Map<String, Object> currencyMap = (Map<String, Object>) currencyObj;
                    currency = (String) currencyMap.get("code");
                }

                if (currency == null) {
                    log.warn("Skipping account with no currency code");
                    continue;
                }

                Map<String, Object> balance = (Map<String, Object>) account.get("balance");

                if (balance == null) {
                    continue;
                }

                String amountStr = (String) balance.get("amount");
                if (amountStr == null || amountStr.equals("0") || amountStr.equals("0.00") || amountStr.equals("0.00000000")) {
                    continue;
                }

                BigDecimal quantity = new BigDecimal(amountStr).setScale(8, RoundingMode.HALF_UP);

                // Skip zero balances
                if (quantity.compareTo(BigDecimal.ZERO) == 0) {
                    continue;
                }

                // Create Holding
                ProviderDataEntity.Holding holding = new ProviderDataEntity.Holding();
                holding.setAsset(currency);

                // Clean up the name - remove "Staked " prefix and " Wallet" suffix
                String accountName = (String) account.get("name");
                String cleanedName = accountName;
                boolean isStaked = false;

                if (accountName != null) {
                    // Check if it's a staked asset
                    if (accountName.startsWith("Staked ")) {
                        isStaked = true;
                        cleanedName = accountName.substring(7); // Remove "Staked " prefix
                    }

                    // Remove " Wallet" suffix if present
                    if (cleanedName.endsWith(" Wallet")) {
                        cleanedName = cleanedName.substring(0, cleanedName.length() - 7);
                    }
                }

                holding.setName(cleanedName);
                holding.setIsStaked(isStaked);
                holding.setQuantity(quantity);

                // Fetch real-time spot price from Coinbase
                BigDecimal currentPrice = fetchSpotPrice(accessToken, currency);
                holding.setCurrentPrice(currentPrice);

                // Calculate current value (quantity × price)
                BigDecimal currentValue = quantity.multiply(currentPrice).setScale(2, RoundingMode.HALF_UP);
                holding.setCurrentValue(currentValue);

                // Calculate cost basis and average buy price from transaction history
                String accountId = (String) account.get("id");
                CostBasisData costBasisData = calculateCostBasis(accessToken, accountId, currency);
                holding.setCostBasis(costBasisData.getTotalCostBasis());
                holding.setAverageBuyPrice(costBasisData.getAverageBuyPrice());

                // Store original metadata
                holding.setOriginalSymbol(currency);

                holdings.add(holding);

                // Accumulate totals (use current value, not quantity)
                totalValue = totalValue.add(currentValue);

                // Track fiat currencies as cash balance
                if (isFiatCurrency(currency)) {
                    cashBalance = cashBalance.add(quantity);
                }

            } catch (Exception e) {
                log.error("Error transforming Coinbase account", e);
                // Continue with other accounts
            }
        }

        entity.setHoldings(holdings);
        entity.setTotalValue(totalValue);
        entity.setCashBalance(cashBalance);

        return entity;
    }

    /**
     * Check if symbol is a fiat currency.
     */
    private boolean isFiatCurrency(String symbol) {
        return symbol != null &&
               (symbol.equals("USD") || symbol.equals("EUR") || symbol.equals("GBP") ||
                symbol.equals("CAD") || symbol.equals("JPY") || symbol.equals("AUD"));
    }

    /**
     * Store provider data entity in Firestore
     */
    private void storeProviderData(String userId, ProviderDataEntity entity) {
        try {
            log.info("=== COINBASE PROVIDER DATA STORAGE ===");
            log.info("User ID: {}", userId);
            log.info("Provider ID: {}", PROVIDER_ID);
            log.info("Holdings count: {}", entity.getHoldings() != null ? entity.getHoldings().size() : 0);
            log.info("Total value: {}", entity.getTotalValue());
            log.info("Cash balance: {}", entity.getCashBalance());

            if (entity.getHoldings() != null && !entity.getHoldings().isEmpty()) {
                log.info("First holding: asset={}, quantity={}",
                    entity.getHoldings().get(0).getAsset(),
                    entity.getHoldings().get(0).getQuantity());
            }

            // Use createOrReplace to handle both create and update
            createProviderDataRepository.createOrReplaceProviderData(userId, PROVIDER_ID, entity);
            log.info("Successfully stored Coinbase provider data at path: users/{}/provider_data/{}", userId, PROVIDER_ID);

            // Refresh portfolio summary to include this provider's data
            if (portfolioSummaryManager != null) {
                try {
                    portfolioSummaryManager.refreshPortfolioSummary(userId);
                    log.info("Refreshed portfolio summary after storing Coinbase data for user: {}", userId);
                } catch (Exception e) {
                    log.warn("Failed to refresh portfolio summary for user {}: {}", userId, e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Failed to store Coinbase provider data for user: {}", userId, e);
            throw e;
        }
    }

    /**
     * Fetch current spot price for a currency from Coinbase
     * @param accessToken OAuth access token
     * @param currency Currency code (e.g., "BTC", "ETH")
     * @return Current spot price in USD, or BigDecimal.ONE if unable to fetch
     */
    private BigDecimal fetchSpotPrice(String accessToken, String currency) {
        try {
            // Skip price fetching for fiat currencies (they're always 1:1 with USD)
            if (isFiatCurrency(currency)) {
                return BigDecimal.ONE;
            }

            // Build currency pair (e.g., "BTC-USD")
            String currencyPair = currency + "-USD";

            log.debug("Fetching spot price for {}", currencyPair);

            // Call Coinbase API for spot price
            Map<String, Object> priceData = coinbaseDataClient.getSpotPrice(accessToken, currencyPair);

            // Extract price from response
            // Response format: {"amount": "43250.50", "currency": "USD"}
            if (priceData != null && priceData.containsKey("amount")) {
                String priceStr = (String) priceData.get("amount");
                BigDecimal price = new BigDecimal(priceStr).setScale(8, RoundingMode.HALF_UP);
                log.debug("Fetched spot price for {}: ${}", currency, price);
                return price;
            }

            log.warn("Unable to fetch price for {}, using 1.0 as fallback", currency);
            return BigDecimal.ONE;

        } catch (Exception e) {
            log.warn("Error fetching spot price for {}: {}, using 1.0 as fallback", currency, e.getMessage());
            return BigDecimal.ONE;
        }
    }

    /**
     * Calculate cost basis and average buy price from transaction history
     * @param accessToken OAuth access token
     * @param accountId Account ID
     * @param currency Currency code (e.g., "BTC", "ETH")
     * @return CostBasisData with total cost basis and average buy price
     */
    private CostBasisData calculateCostBasis(String accessToken, String accountId, String currency) {
        try {
            log.debug("Calculating cost basis for {} in account {}", currency, accountId);

            // Fetch transactions for this account (limit to 100 most recent for performance)
            List<Map<String, Object>> transactions = coinbaseDataClient.getTransactions(accessToken, accountId, 100);

            BigDecimal totalCostBasis = BigDecimal.ZERO;
            BigDecimal totalQuantityBought = BigDecimal.ZERO;

            for (Map<String, Object> transaction : transactions) {
                try {
                    String type = (String) transaction.get("type");

                    // Only process "buy" transactions for cost basis
                    if (!"buy".equals(type)) {
                        continue;
                    }

                    // Extract native_amount (USD spent)
                    Map<String, Object> nativeAmount = (Map<String, Object>) transaction.get("native_amount");
                    if (nativeAmount == null) {
                        continue;
                    }

                    String nativeAmountStr = (String) nativeAmount.get("amount");
                    if (nativeAmountStr == null) {
                        continue;
                    }

                    // Extract amount (crypto received)
                    Map<String, Object> amount = (Map<String, Object>) transaction.get("amount");
                    if (amount == null) {
                        continue;
                    }

                    String amountStr = (String) amount.get("amount");
                    if (amountStr == null) {
                        continue;
                    }

                    // Parse values
                    BigDecimal nativeAmountValue = new BigDecimal(nativeAmountStr).abs(); // Use abs() in case it's negative
                    BigDecimal cryptoAmount = new BigDecimal(amountStr).abs();

                    // Accumulate totals
                    totalCostBasis = totalCostBasis.add(nativeAmountValue);
                    totalQuantityBought = totalQuantityBought.add(cryptoAmount);

                    log.debug("Buy transaction: {} {} for ${}", cryptoAmount, currency, nativeAmountValue);

                } catch (Exception e) {
                    log.warn("Error processing transaction for cost basis: {}", e.getMessage());
                    // Continue with next transaction
                }
            }

            // Calculate average buy price
            BigDecimal averageBuyPrice = null;
            if (totalQuantityBought.compareTo(BigDecimal.ZERO) > 0) {
                averageBuyPrice = totalCostBasis.divide(totalQuantityBought, 8, RoundingMode.HALF_UP);
                log.debug("Calculated cost basis for {}: totalCost=${}, totalQty={}, avgPrice=${}",
                    currency, totalCostBasis, totalQuantityBought, averageBuyPrice);
            } else {
                log.debug("No buy transactions found for {}", currency);
            }

            return new CostBasisData(
                totalCostBasis.compareTo(BigDecimal.ZERO) > 0 ? totalCostBasis : null,
                averageBuyPrice
            );

        } catch (Exception e) {
            log.warn("Error calculating cost basis for {}: {}", currency, e.getMessage());
            return new CostBasisData(null, null);
        }
    }

    /**
     * Helper class to hold cost basis calculation results
     */
    private static class CostBasisData {
        private final BigDecimal totalCostBasis;
        private final BigDecimal averageBuyPrice;

        public CostBasisData(BigDecimal totalCostBasis, BigDecimal averageBuyPrice) {
            this.totalCostBasis = totalCostBasis;
            this.averageBuyPrice = averageBuyPrice;
        }

        public BigDecimal getTotalCostBasis() {
            return totalCostBasis;
        }

        public BigDecimal getAverageBuyPrice() {
            return averageBuyPrice;
        }
    }

}
 