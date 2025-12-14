package io.strategiz.business.provider.alpaca;

import io.strategiz.business.provider.alpaca.model.AlpacaConnectionResult;
import io.strategiz.business.provider.alpaca.model.AlpacaDisconnectionResult;
import io.strategiz.business.provider.alpaca.model.AlpacaTokenRefreshResult;
import io.strategiz.client.alpaca.client.AlpacaClient;
import io.strategiz.client.alpaca.client.AlpacaDataClient;
import io.strategiz.client.alpaca.auth.AlpacaOAuthClient;
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
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.framework.exception.ErrorCode;
import io.strategiz.framework.secrets.controller.SecretManager;
import io.strategiz.business.portfolio.PortfolioSummaryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private static final String PROVIDER_TYPE = "equity";
    private static final String PROVIDER_CATEGORY = "brokerage";
    private static final String DEFAULT_ENVIRONMENT = "paper"; // Default to paper for safety

    private final AlpacaClient alpacaClient;
    private final AlpacaDataClient alpacaDataClient;
    private final AlpacaOAuthClient alpacaOAuthClient;
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
    @Value("${oauth.providers.alpaca.client-id:}")
    private String clientId;

    @Value("${oauth.providers.alpaca.client-secret:}")
    private String clientSecret;

    @Value("${oauth.providers.alpaca.redirect-uri}")
    private String redirectUri;

    @Value("${oauth.providers.alpaca.auth-url:https://app.alpaca.markets/oauth/authorize}")
    private String authUrl;

    @Value("${oauth.providers.alpaca.scope:account:read trading:write data:read}")
    private String scope;

    @Autowired
    public AlpacaProviderBusiness(
            AlpacaClient alpacaClient,
            AlpacaDataClient alpacaDataClient,
            AlpacaOAuthClient alpacaOAuthClient,
            CreateProviderIntegrationRepository createProviderIntegrationRepository,
            ReadProviderIntegrationRepository readProviderIntegrationRepository,
            UpdateProviderIntegrationRepository updateProviderIntegrationRepository,
            @Autowired(required = false) CreateProviderDataRepository createProviderDataRepository,
            @Autowired(required = false) ReadProviderDataRepository readProviderDataRepository,
            @Autowired(required = false) UpdateProviderDataRepository updateProviderDataRepository,
            @Qualifier("vaultSecretService") SecretManager secretManager) {
        this.alpacaClient = alpacaClient;
        this.alpacaDataClient = alpacaDataClient;
        this.alpacaOAuthClient = alpacaOAuthClient;
        this.createProviderIntegrationRepository = createProviderIntegrationRepository;
        this.readProviderIntegrationRepository = readProviderIntegrationRepository;
        this.updateProviderIntegrationRepository = updateProviderIntegrationRepository;
        this.createProviderDataRepository = createProviderDataRepository;
        this.readProviderDataRepository = readProviderDataRepository;
        this.updateProviderDataRepository = updateProviderDataRepository;
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
            // Use the AlpacaOAuthClient to exchange the code for tokens
            log.info("Exchanging authorization code for tokens using AlpacaOAuthClient");
            return alpacaOAuthClient.exchangeCodeForTokens(authorizationCode);
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
        return alpacaDataClient.getAccount(accessToken);
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
            // Use the AlpacaDataClient to make authenticated requests
            log.debug("Making authenticated request to endpoint: {}", endpoint);

            // Determine which client method to call based on the endpoint
            if (endpoint.equals("/v2/account")) {
                return alpacaDataClient.getAccount(accessToken);
            } else if (endpoint.equals("/v2/positions")) {
                return (Map<String, Object>) (Object) alpacaDataClient.getPositions(accessToken);
            } else {
                // For other endpoints, use the generic OAuth request method
                return alpacaClient.oauthRequest(
                    org.springframework.http.HttpMethod.GET,
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
            // Use the AlpacaOAuthClient to refresh the token
            log.info("Refreshing access token using AlpacaOAuthClient");
            return alpacaOAuthClient.refreshAccessToken(refreshToken);
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
            // Use the AlpacaOAuthClient to revoke the token
            log.info("Revoking access token using AlpacaOAuthClient");
            boolean success = alpacaOAuthClient.revokeAccessToken(accessToken);

            if (success) {
                log.info("Successfully revoked Alpaca access token");
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
        log.info("Testing Alpaca connection for user: {}", userId);
        
        // Alpaca uses OAuth, so we can't test with API keys
        // Return true to indicate OAuth flow should be initiated
        log.info("Alpaca requires OAuth flow - cannot test with API keys");
        return true;
    }
    
    @Override
    public ProviderIntegrationResult createIntegration(CreateProviderIntegrationRequest request, String userId) {
        // Extract environment from request, default to paper for safety
        String environment = request.getEnvironment();
        if (environment == null || environment.trim().isEmpty()) {
            environment = DEFAULT_ENVIRONMENT;
            log.info("No environment specified, defaulting to: {}", environment);
        }

        // Validate environment
        if (!environment.equals("paper") && !environment.equals("live")) {
            throw new StrategizException(ErrorCode.VALIDATION_ERROR,
                "Invalid environment: " + environment + ". Must be 'paper' or 'live'");
        }

        log.info("Creating Alpaca {} integration for user: {}", environment, userId);

        // Load environment-specific OAuth configuration from Vault
        String vaultEnvironmentKey = "alpaca-" + environment;
        loadOAuthConfigForEnvironment(vaultEnvironmentKey);

        log.info("OAuth configuration for {} - clientId: {}, redirectUri: {}",
                environment,
                clientId != null ? "configured" : "null",
                redirectUri != null ? redirectUri : "null");

        // For OAuth providers, we don't store credentials during signup
        // Instead, we'll initiate the OAuth flow
        try {
            // Create simplified provider integration entity for Firestore
            // Only store essential fields: providerId, connectionType, isEnabled
            ProviderIntegrationEntity entity = new ProviderIntegrationEntity(PROVIDER_ID, "oauth", userId);
            entity.setStatus("disconnected"); // Not connected until OAuth is complete
            entity.setEnvironment(environment); // Store which environment (paper/live)

            // Save to Firestore
            ProviderIntegrationEntity savedEntity = createProviderIntegrationRepository.createForUser(entity, userId);
            log.info("Created pending Alpaca {} provider integration for user: {}", environment, userId);

            // Generate OAuth URL for the response
            String state = generateSecureState(userId, environment);
            String authUrl = generateAuthorizationUrl(userId, state);
            log.info("Generated OAuth URL for {}: {}", environment, authUrl);

            // Build result with OAuth URL
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("oauthUrl", authUrl);
            metadata.put("state", state);
            metadata.put("environment", environment);
            log.info("Returning metadata with oauthUrl: {}", metadata.get("oauthUrl"));
            metadata.put("oauthRequired", true);
            metadata.put("authMethod", "oauth2");
            metadata.put("scope", scope);

            ProviderIntegrationResult result = new ProviderIntegrationResult();
            result.setSuccess(true);
            result.setMessage("OAuth URL generated successfully for " + environment + " environment");
            result.setMetadata(metadata);
            return result;

        } catch (Exception e) {
            log.error("Error creating Alpaca {} integration for user: {}", environment, userId, e);
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
            // Extract environment from state parameter (format: userId-environment-uuid)
            String environment = extractEnvironmentFromState(state);
            log.info("Extracted environment from state: {}", environment);

            // Handle OAuth callback
            AlpacaConnectionResult result = handleOAuthCallback(userId, authorizationCode, state);

            // Store tokens in Vault with environment suffix
            storeTokensInVault(userId, environment, result.getAccessToken(), result.getRefreshToken(), result.getExpiresAt());

            // Create or update provider integration in Firestore
            Optional<ProviderIntegrationEntity> existingIntegration = readProviderIntegrationRepository.findByUserIdAndProviderId(userId, PROVIDER_ID);

            if (existingIntegration.isPresent()) {
                // Update existing integration - just enable it
                ProviderIntegrationEntity entity = existingIntegration.get();
                entity.setStatus("connected"); // Mark as connected
                entity.setEnvironment(environment); // Store environment

                updateProviderIntegrationRepository.updateWithUserId(entity, userId);
                log.info("Updated Alpaca {} integration status to connected for user: {}", environment, userId);
            } else {
                // Create new integration with simplified entity
                ProviderIntegrationEntity entity = new ProviderIntegrationEntity(PROVIDER_ID, "oauth", userId);
                entity.setStatus("connected"); // Mark as connected
                entity.setEnvironment(environment); // Store environment

                ProviderIntegrationEntity savedEntity = createProviderIntegrationRepository.createForUser(entity, userId);
                log.info("Created new Alpaca {} integration for user: {}", environment, userId);
            }

            // Fetch and store portfolio data after OAuth completion (synchronous, like Coinbase)
            try {
                fetchAndStorePortfolioData(userId, result.getAccessToken());
                log.info("Successfully fetched and stored Alpaca {} portfolio data during OAuth for user: {}", environment, userId);
            } catch (Exception e) {
                log.error("Failed to fetch portfolio data for user: {} (OAuth still succeeded)", userId, e);
                // Don't throw - OAuth is complete, credentials are stored, data can be synced later
            }

        } catch (Exception e) {
            log.error("Error completing Alpaca OAuth flow for user: {}", userId, e);
            throw new RuntimeException("Failed to complete OAuth flow", e);
        }
    }

    /**
     * Sync provider data for a specific user
     * Retrieves access token from Vault and fetches latest portfolio data from Alpaca
     *
     * @param userId The user ID
     * @return ProviderDataEntity with synced data
     * @throws RuntimeException if sync fails
     */
    public ProviderDataEntity syncProviderData(String userId) {
        log.info("Syncing Alpaca provider data for user: {}", userId);

        try {
            // Retrieve access token from Vault
            String secretPath = "secret/strategiz/users/" + userId + "/providers/alpaca";
            Map<String, Object> secretData = secretManager.readSecretAsMap(secretPath);

            if (secretData == null || secretData.isEmpty()) {
                throw new RuntimeException("No Alpaca tokens found in Vault for user: " + userId);
            }

            String accessToken = (String) secretData.get("accessToken");
            if (accessToken == null || accessToken.isEmpty()) {
                throw new RuntimeException("Access token not found in Vault for user: " + userId);
            }

            log.debug("Retrieved Alpaca access token from Vault for user: {}", userId);

            // Fetch and store portfolio data (same process as during provider connection)
            fetchAndStorePortfolioData(userId, accessToken);

            // Retrieve and return the stored data
            ProviderDataEntity providerData = readProviderDataRepository.getProviderData(userId, PROVIDER_ID);

            if (providerData != null) {
                log.info("Successfully synced Alpaca data for user: {}", userId);
                return providerData;
            } else {
                log.warn("No provider data found after sync for user: {}", userId);
                throw new RuntimeException("No data found after sync");
            }

        } catch (Exception e) {
            log.error("Failed to sync Alpaca provider data for user: {}", userId, e);
            throw new RuntimeException("Failed to sync provider data: " + e.getMessage(), e);
        }
    }

    /**
     * Fetch portfolio data from Alpaca and store in provider_data collection
     * This is called after OAuth completion to pre-populate the dashboard
     */
    private void fetchAndStorePortfolioData(String userId, String accessToken) {
        log.info("Fetching Alpaca portfolio data for user: {}", userId);

        if (createProviderDataRepository == null) {
            log.warn("Provider data repository not available - skipping portfolio data fetch");
            return;
        }

        try {
            // Fetch account info from Alpaca
            Map<String, Object> accountInfo = alpacaDataClient.getAccount(accessToken);

            // Fetch positions from Alpaca
            List<Map<String, Object>> positions = alpacaDataClient.getPositions(accessToken);

            // Transform to ProviderDataEntity with Holdings
            ProviderDataEntity portfolioData = transformToProviderDataEntity(accountInfo, positions, userId, accessToken);

            // Store in Firestore
            storeProviderData(userId, portfolioData);

            log.info("Successfully stored Alpaca portfolio data for user: {} with {} holdings",
                userId, portfolioData.getHoldings() != null ? portfolioData.getHoldings().size() : 0);

        } catch (Exception e) {
            log.error("Failed to fetch and store Alpaca portfolio data for user: {}", userId, e);
            // Don't throw - OAuth is complete, data can be synced later
        }
    }

    /**
     * Transform Alpaca API response to ProviderDataEntity
     * @param accountInfo The account info response from Alpaca API
     * @param positions The positions response from Alpaca API
     * @param userId The user ID
     * @param accessToken OAuth access token
     */
    private ProviderDataEntity transformToProviderDataEntity(Map<String, Object> accountInfo,
                                                             List<Map<String, Object>> positions,
                                                             String userId,
                                                             String accessToken) {
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

        // Extract account cash balance
        if (accountInfo != null) {
            try {
                // Extract cash (buying_power or cash)
                String cashStr = (String) accountInfo.get("cash");
                if (cashStr != null) {
                    cashBalance = new BigDecimal(cashStr).setScale(2, RoundingMode.HALF_UP);
                }

                // Extract portfolio value
                String portfolioValueStr = (String) accountInfo.get("portfolio_value");
                if (portfolioValueStr != null) {
                    totalValue = new BigDecimal(portfolioValueStr).setScale(2, RoundingMode.HALF_UP);
                }

                log.info("Alpaca account - cash: ${}, portfolio value: ${}", cashBalance, totalValue);
            } catch (Exception e) {
                log.error("Error extracting account balance from Alpaca", e);
            }
        }

        // Process positions
        if (positions != null && !positions.isEmpty()) {
            for (Map<String, Object> position : positions) {
                try {
                    // Extract position data
                    String symbol = (String) position.get("symbol");
                    if (symbol == null) {
                        log.warn("Skipping position with no symbol");
                        continue;
                    }

                    String qtyStr = (String) position.get("qty");
                    if (qtyStr == null || qtyStr.equals("0")) {
                        continue;
                    }

                    BigDecimal quantity = new BigDecimal(qtyStr).setScale(8, RoundingMode.HALF_UP);

                    // Skip zero positions
                    if (quantity.compareTo(BigDecimal.ZERO) == 0) {
                        continue;
                    }

                    // Create Holding
                    ProviderDataEntity.Holding holding = new ProviderDataEntity.Holding();
                    holding.setAsset(symbol);
                    holding.setName(symbol); // Alpaca uses symbol as name
                    holding.setQuantity(quantity);

                    // Extract current price
                    String currentPriceStr = (String) position.get("current_price");
                    if (currentPriceStr != null) {
                        BigDecimal currentPrice = new BigDecimal(currentPriceStr).setScale(2, RoundingMode.HALF_UP);
                        holding.setCurrentPrice(currentPrice);
                    }

                    // Extract market value
                    String marketValueStr = (String) position.get("market_value");
                    if (marketValueStr != null) {
                        BigDecimal currentValue = new BigDecimal(marketValueStr).setScale(2, RoundingMode.HALF_UP);
                        holding.setCurrentValue(currentValue);
                    }

                    // Extract cost basis
                    String costBasisStr = (String) position.get("cost_basis");
                    if (costBasisStr != null) {
                        BigDecimal costBasis = new BigDecimal(costBasisStr).setScale(2, RoundingMode.HALF_UP);
                        holding.setCostBasis(costBasis);
                    }

                    // Extract average entry price
                    String avgEntryPriceStr = (String) position.get("avg_entry_price");
                    if (avgEntryPriceStr != null) {
                        BigDecimal avgBuyPrice = new BigDecimal(avgEntryPriceStr).setScale(2, RoundingMode.HALF_UP);
                        holding.setAverageBuyPrice(avgBuyPrice);
                    }

                    // Calculate profit/loss if we have both values
                    if (holding.getCurrentValue() != null && holding.getCostBasis() != null) {
                        BigDecimal profitLoss = holding.getCurrentValue().subtract(holding.getCostBasis());
                        holding.setProfitLoss(profitLoss);

                        // Calculate profit/loss percentage
                        if (holding.getCostBasis().compareTo(BigDecimal.ZERO) > 0) {
                            BigDecimal profitLossPercent = profitLoss
                                .divide(holding.getCostBasis(), 4, RoundingMode.HALF_UP)
                                .multiply(new BigDecimal("100"))
                                .setScale(2, RoundingMode.HALF_UP);
                            holding.setProfitLossPercent(profitLossPercent);
                        }
                    }

                    // Extract change today (unrealized_intraday_pl)
                    String intraDayPlStr = (String) position.get("unrealized_intraday_pl");
                    if (intraDayPlStr != null) {
                        BigDecimal priceChange24h = new BigDecimal(intraDayPlStr).setScale(2, RoundingMode.HALF_UP);
                        holding.setPriceChange24h(priceChange24h);
                    }

                    // Set asset type
                    holding.setAssetType("stock");

                    // Store original symbol
                    holding.setOriginalSymbol(symbol);

                    holdings.add(holding);

                    log.debug("Added Alpaca position: {} - qty={}, value=${}",
                        symbol, quantity, holding.getCurrentValue());

                } catch (Exception e) {
                    log.error("Error transforming Alpaca position", e);
                    // Continue with other positions
                }
            }
        }

        entity.setHoldings(holdings);
        entity.setTotalValue(totalValue);
        entity.setCashBalance(cashBalance);

        // Calculate total profit/loss from holdings
        BigDecimal totalProfitLoss = BigDecimal.ZERO;
        for (ProviderDataEntity.Holding holding : holdings) {
            if (holding.getProfitLoss() != null) {
                totalProfitLoss = totalProfitLoss.add(holding.getProfitLoss());
            }
        }
        entity.setTotalProfitLoss(totalProfitLoss);

        // Calculate total profit/loss percentage
        BigDecimal totalCostBasis = BigDecimal.ZERO;
        for (ProviderDataEntity.Holding holding : holdings) {
            if (holding.getCostBasis() != null) {
                totalCostBasis = totalCostBasis.add(holding.getCostBasis());
            }
        }
        if (totalCostBasis.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal totalProfitLossPercent = totalProfitLoss
                .divide(totalCostBasis, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
            entity.setTotalProfitLossPercent(totalProfitLossPercent);
        }

        return entity;
    }

    /**
     * Store provider data entity in Firestore
     */
    private void storeProviderData(String userId, ProviderDataEntity entity) {
        try {
            log.info("=== ALPACA PROVIDER DATA STORAGE ===");
            log.info("User ID: {}", userId);
            log.info("Provider ID: {}", PROVIDER_ID);
            log.info("Holdings count: {}", entity.getHoldings() != null ? entity.getHoldings().size() : 0);
            log.info("Total value: ${}", entity.getTotalValue());
            log.info("Cash balance: ${}", entity.getCashBalance());
            log.info("Total P/L: ${}", entity.getTotalProfitLoss());

            if (entity.getHoldings() != null && !entity.getHoldings().isEmpty()) {
                log.info("First holding: asset={}, quantity={}, value=${}",
                    entity.getHoldings().get(0).getAsset(),
                    entity.getHoldings().get(0).getQuantity(),
                    entity.getHoldings().get(0).getCurrentValue());
            }

            // Use createOrReplace to handle both create and update
            createProviderDataRepository.createOrReplaceProviderData(userId, PROVIDER_ID, entity);
            log.info("Successfully stored Alpaca provider data at path: users/{}/provider_data/{}", userId, PROVIDER_ID);

            // Refresh portfolio summary to include this provider's data
            if (portfolioSummaryManager != null) {
                try {
                    portfolioSummaryManager.refreshPortfolioSummary(userId);
                    log.info("Refreshed portfolio summary after storing Alpaca data for user: {}", userId);
                } catch (Exception e) {
                    log.warn("Failed to refresh portfolio summary for user {}: {}", userId, e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Failed to store Alpaca provider data for user: {}", userId, e);
            throw e;
        }
    }

    private void storeTokensInVault(String userId, String environment, String accessToken, String refreshToken, Instant expiresAt) {
        try {
            // Store tokens with environment suffix (e.g., alpaca-paper, alpaca-live)
            String secretPath = "secret/strategiz/users/" + userId + "/providers/alpaca-" + environment;

            Map<String, Object> secretData = new HashMap<>();
            secretData.put("accessToken", accessToken);
            secretData.put("refreshToken", refreshToken);
            secretData.put("expiresAt", expiresAt.toString());
            secretData.put("provider", PROVIDER_ID);
            secretData.put("environment", environment);
            secretData.put("storedAt", Instant.now().toString());

            // Convert map to JSON string for storage
            ObjectMapper objectMapper = new ObjectMapper();
            String secretJson = objectMapper.writeValueAsString(secretData);
            secretManager.createSecret(secretPath, secretJson);
            log.debug("Stored Alpaca {} OAuth tokens in Vault for user: {}", environment, userId);

        } catch (Exception e) {
            log.error("Failed to store Alpaca {} tokens for user: {}", environment, userId, e);
            throw new RuntimeException("Failed to store tokens", e);
        }
    }

    /**
     * Extract environment from state parameter
     * State format: userId-environment-uuid
     */
    private String extractEnvironmentFromState(String state) {
        try {
            String[] parts = state.split("-");
            if (parts.length >= 3) {
                return parts[1]; // environment is second part
            }
            log.warn("Could not extract environment from state, defaulting to paper");
            return DEFAULT_ENVIRONMENT;
        } catch (Exception e) {
            log.error("Error extracting environment from state: {}", e.getMessage());
            return DEFAULT_ENVIRONMENT;
        }
    }
    
    private String generateSecureState(String userId, String environment) {
        // Generate a secure random state parameter that includes environment
        return userId + "-" + environment + "-" + UUID.randomUUID().toString();
    }

    /**
     * Load OAuth configuration for specific environment from Vault
     * @param vaultKey The Vault key (e.g., "alpaca-paper" or "alpaca-live")
     */
    private void loadOAuthConfigForEnvironment(String vaultKey) {
        try {
            String secretPath = "secret/strategiz/oauth/" + vaultKey;
            Map<String, Object> secretData = secretManager.readSecretAsMap(secretPath);

            if (secretData == null || secretData.isEmpty()) {
                throw new StrategizException(ErrorCode.INTERNAL_ERROR,
                    "No OAuth configuration found in Vault for: " + vaultKey);
            }

            // Override instance variables with environment-specific values
            this.clientId = (String) secretData.get("client-id");
            this.clientSecret = (String) secretData.get("client-secret");

            // Use redirect-uri based on active profile (prod uses prod URI, dev uses local)
            String redirectUriLocal = (String) secretData.get("redirect-uri-local");
            String redirectUriProd = (String) secretData.get("redirect-uri-prod");
            String activeProfiles = System.getProperty("spring.profiles.active",
                System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE", "dev"));
            this.redirectUri = activeProfiles.contains("prod") ? redirectUriProd : redirectUriLocal;

            log.debug("Loaded OAuth config for {} from Vault", vaultKey);

        } catch (Exception e) {
            log.error("Failed to load OAuth config for {}: {}", vaultKey, e.getMessage());
            throw new StrategizException(ErrorCode.INTERNAL_ERROR,
                "Failed to load OAuth configuration for " + vaultKey);
        }
    }
}