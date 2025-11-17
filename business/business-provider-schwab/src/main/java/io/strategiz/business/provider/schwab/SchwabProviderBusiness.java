package io.strategiz.business.provider.schwab;

import io.strategiz.business.provider.schwab.model.SchwabConnectionResult;
import io.strategiz.client.schwab.SchwabClient;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Business logic for Charles Schwab provider integration.
 * Handles OAuth flows, API interactions, and business rules specific to Charles Schwab.
 */
@Component
public class SchwabProviderBusiness implements ProviderIntegrationHandler {
    
    private static final Logger log = LoggerFactory.getLogger(SchwabProviderBusiness.class);
    
    private static final String PROVIDER_ID = "schwab";
    private static final String PROVIDER_NAME = "Charles Schwab";
    private static final String PROVIDER_TYPE = "brokerage";
    
    private final SchwabClient schwabClient;
    private final CreateProviderIntegrationRepository createProviderIntegrationRepository;
    private final ReadProviderIntegrationRepository readProviderIntegrationRepository;
    private final UpdateProviderIntegrationRepository updateProviderIntegrationRepository;
    private final CreateProviderDataRepository createProviderDataRepository;
    private final ReadProviderDataRepository readProviderDataRepository;
    private final UpdateProviderDataRepository updateProviderDataRepository;
    private final SecretManager secretManager;
    
    // OAuth Configuration
    @Value("${oauth.providers.schwab.client-id:}")
    private String clientId;
    
    @Value("${oauth.providers.schwab.client-secret:}")
    private String clientSecret;
    
    @Value("${oauth.providers.schwab.redirect-uri:https://127.0.0.1:8443/v1/providers/callback/schwab}")
    private String redirectUri;
    
    @Value("${oauth.providers.schwab.auth-url:https://api.schwabapi.com/v1/oauth/authorize}")
    private String authUrl;
    
    @Value("${oauth.providers.schwab.scope:readonly}")
    private String scope;

    @Autowired
    public SchwabProviderBusiness(
            SchwabClient schwabClient,
            CreateProviderIntegrationRepository createProviderIntegrationRepository,
            ReadProviderIntegrationRepository readProviderIntegrationRepository,
            UpdateProviderIntegrationRepository updateProviderIntegrationRepository,
            @Autowired(required = false) CreateProviderDataRepository createProviderDataRepository,
            @Autowired(required = false) ReadProviderDataRepository readProviderDataRepository,
            @Autowired(required = false) UpdateProviderDataRepository updateProviderDataRepository,
            @Qualifier("vaultSecretService") SecretManager secretManager) {
        this.schwabClient = schwabClient;
        this.createProviderIntegrationRepository = createProviderIntegrationRepository;
        this.readProviderIntegrationRepository = readProviderIntegrationRepository;
        this.updateProviderIntegrationRepository = updateProviderIntegrationRepository;
        this.createProviderDataRepository = createProviderDataRepository;
        this.readProviderDataRepository = readProviderDataRepository;
        this.updateProviderDataRepository = updateProviderDataRepository;
        this.secretManager = secretManager;
    }

    /**
     * Generate OAuth authorization URL for Charles Schwab
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
        
        log.info("Generated Schwab OAuth URL for user: {}", userId);
        return authorizationUrl;
    }
    
    /**
     * Handle OAuth callback from Charles Schwab
     * 
     * @param userId The user completing OAuth
     * @param authorizationCode The authorization code from Schwab
     * @param state The state parameter for validation
     * @return Connection result with tokens and account info
     * @throws StrategizException if OAuth exchange fails
     */
    public SchwabConnectionResult handleOAuthCallback(String userId, String authorizationCode, String state) {
        validateRequired("userId", userId);
        validateRequired("authorizationCode", authorizationCode);
        validateRequired("state", state);
        
        try {
            // Exchange authorization code for access token
            Map<String, Object> tokenData = schwabClient.exchangeCodeForTokens(authorizationCode);
            
            String accessToken = (String) tokenData.get("access_token");
            String refreshToken = (String) tokenData.get("refresh_token");
            Integer expiresIn = (Integer) tokenData.get("expires_in");
            
            // Get user account information from Schwab
            Map<String, Object> accountInfo = schwabClient.getUserAccountInfo(accessToken);
            
            // Build connection result
            SchwabConnectionResult result = new SchwabConnectionResult();
            result.setUserId(userId);
            result.setProviderId(PROVIDER_ID);
            result.setProviderName(PROVIDER_NAME);
            result.setAccessToken(accessToken);
            result.setRefreshToken(refreshToken);
            result.setExpiresAt(Instant.now().plusSeconds(expiresIn != null ? expiresIn : 1800)); // 30 min default
            result.setAccountInfo(accountInfo);
            result.setConnectedAt(Instant.now());
            result.setStatus("connected");
            
            log.info("Successfully connected Charles Schwab for user: {}", userId);
            return result;
            
        } catch (StrategizException e) {
            log.error("Failed to handle Schwab OAuth callback for user: {}: {}", userId, e.getMessage());
            throw new StrategizException(ErrorCode.EXTERNAL_SERVICE_ERROR, 
                "Failed to complete Charles Schwab OAuth: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to handle Schwab OAuth callback for user: {}", userId, e);
            
            // Enhanced error logging for debugging
            if (e instanceof RestClientException) {
                RestClientException restError = (RestClientException) e;
                log.error("REST client error details: {}", restError.getMessage());
                
                if (restError instanceof org.springframework.web.client.HttpClientErrorException) {
                    org.springframework.web.client.HttpClientErrorException httpError = 
                        (org.springframework.web.client.HttpClientErrorException) restError;
                    log.error("HTTP Status: {}", httpError.getStatusCode());
                    log.error("Response Body: {}", httpError.getResponseBodyAsString());
                    log.error("Response Headers: {}", httpError.getResponseHeaders());
                }
            }
            
            throw new StrategizException(ErrorCode.EXTERNAL_SERVICE_ERROR, 
                "Failed to complete Charles Schwab OAuth: " + e.getMessage());
        }
    }
    
    /**
     * Complete OAuth flow and store tokens using dual storage
     */
    public void completeOAuthFlow(String userId, String authorizationCode, String state) {
        log.info("Completing Charles Schwab OAuth flow for user: {}", userId);
        
        try {
            // Handle OAuth callback
            SchwabConnectionResult result = handleOAuthCallback(userId, authorizationCode, state);
            
            // Store tokens in Vault
            storeTokensInVault(userId, result.getAccessToken(), result.getRefreshToken(), result.getExpiresAt());
            
            // Create or update provider integration in Firestore
            Optional<ProviderIntegrationEntity> existingIntegration = 
                readProviderIntegrationRepository.findByUserIdAndProviderId(userId, PROVIDER_ID);
            
            if (existingIntegration.isPresent()) {
                // Update existing integration - just enable it
                ProviderIntegrationEntity entity = existingIntegration.get();
                entity.setStatus("connected"); // Mark as connected/enabled

                // Tokens should be stored securely elsewhere (e.g., Vault)

                updateProviderIntegrationRepository.updateWithUserId(entity, userId);
                log.info("Updated Schwab integration status to connected for user: {}", userId);
            } else {
                // Create new integration with simplified entity
                ProviderIntegrationEntity entity = new ProviderIntegrationEntity(PROVIDER_ID, "oauth", userId);
                entity.setStatus("connected"); // Mark as connected/enabled

                // Tokens should be stored securely elsewhere (e.g., Vault)

                // Capabilities are implied by the provider type, not stored in entity

                createProviderIntegrationRepository.createForUser(entity, userId);
                log.info("Created new Charles Schwab integration for user: {}", userId);
            }

            // Fetch and store portfolio data after OAuth completion (synchronous)
            try {
                fetchAndStorePortfolioData(userId, result.getAccessToken());
                log.info("Successfully fetched and stored Schwab portfolio data during OAuth for user: {}", userId);
            } catch (Exception e) {
                log.error("Failed to fetch portfolio data for user: {} (OAuth still succeeded)", userId, e);
                // Don't throw - OAuth is complete, credentials are stored, data can be synced later
            }

        } catch (Exception e) {
            log.error("Error completing Charles Schwab OAuth flow for user: {}", userId, e);
            throw new RuntimeException("Failed to complete OAuth flow", e);
        }
    }
    
    private void storeTokensInVault(String userId, String accessToken, String refreshToken, Instant expiresAt) {
        try {
            String secretPath = "secret/strategiz/users/" + userId + "/providers/schwab";
            
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
            log.debug("Stored Charles Schwab OAuth tokens in Vault for user: {}", userId);
            
        } catch (Exception e) {
            log.error("Failed to store Charles Schwab tokens for user: {}", userId, e);
            throw new RuntimeException("Failed to store tokens", e);
        }
    }
    
    private void validateOAuthConfiguration() {
        if (clientId == null || clientId.trim().isEmpty()) {
            throw new StrategizException(ErrorCode.VALIDATION_ERROR, 
                "Charles Schwab OAuth client ID is not configured");
        }
        if (clientSecret == null || clientSecret.trim().isEmpty()) {
            throw new StrategizException(ErrorCode.VALIDATION_ERROR, 
                "Charles Schwab OAuth client secret is not configured");
        }
        if (redirectUri == null || redirectUri.trim().isEmpty()) {
            throw new StrategizException(ErrorCode.VALIDATION_ERROR, 
                "Charles Schwab OAuth redirect URI is not configured");
        }
    }
    
    private void validateRequired(String paramName, String paramValue) {
        if (paramValue == null || paramValue.trim().isEmpty()) {
            throw new StrategizException(ErrorCode.VALIDATION_ERROR, 
                "Required parameter '" + paramName + "' is missing or empty");
        }
    }
    
    private String generateSecureState(String userId) {
        return userId + "-" + UUID.randomUUID().toString();
    }
    
    // ProviderIntegrationHandler interface implementation
    
    @Override
    public boolean testConnection(CreateProviderIntegrationRequest request, String userId) {
        log.info("Testing Charles Schwab connection for user: {}", userId);
        
        // Schwab uses OAuth, so we can't test with API keys
        // Return true to indicate OAuth flow should be initiated
        log.info("Charles Schwab requires OAuth flow - cannot test with API keys");
        return true;
    }
    
    @Override
    public ProviderIntegrationResult createIntegration(CreateProviderIntegrationRequest request, String userId) {
        log.info("Creating Charles Schwab integration for user: {}", userId);
        log.info("OAuth configuration - clientId: {}, redirectUri: {}", 
                clientId != null ? "configured" : "null", 
                redirectUri != null ? redirectUri : "null");
        
        try {
            // Create simplified provider integration entity for Firestore
            ProviderIntegrationEntity entity = new ProviderIntegrationEntity(PROVIDER_ID, "oauth", userId);
            entity.setStatus("disconnected"); // Not enabled until OAuth is complete
            
            // Save to Firestore
            ProviderIntegrationEntity savedEntity = createProviderIntegrationRepository.createForUser(entity, userId);
            log.info("Created pending Charles Schwab provider integration for user: {}", userId);
            
            // Generate OAuth URL for the response
            String state = generateSecureState(userId);
            String authUrl = generateAuthorizationUrl(userId, state);
            log.info("Generated Charles Schwab OAuth URL: {}", authUrl);
            
            // Build result with OAuth URL
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("oauthUrl", authUrl);
            metadata.put("state", state);
            metadata.put("oauthRequired", true);
            metadata.put("authMethod", "oauth2");
            metadata.put("scope", scope);
            
            ProviderIntegrationResult result = new ProviderIntegrationResult();
            result.setSuccess(true);
            result.setMessage("Charles Schwab OAuth URL generated successfully");
            result.setMetadata(metadata);
            return result;
                
        } catch (Exception e) {
            log.error("Error creating Charles Schwab integration for user: {}", userId, e);
            throw new RuntimeException("Failed to create Charles Schwab integration", e);
        }
    }
    
    @Override
    public String getProviderId() {
        return PROVIDER_ID;
    }

    /**
     * Fetch portfolio data from Schwab and store in provider_data collection
     * This is called after OAuth completion to pre-populate the dashboard
     */
    private void fetchAndStorePortfolioData(String userId, String accessToken) {
        log.info("Fetching Schwab portfolio data for user: {}", userId);

        if (createProviderDataRepository == null) {
            log.warn("Provider data repository not available - skipping portfolio data fetch");
            return;
        }

        try {
            // Fetch account list from Schwab
            Map<String, Object> accountsResponse = schwabClient.getUserAccountInfo(accessToken);

            // Transform to ProviderDataEntity with Holdings
            ProviderDataEntity portfolioData = transformToProviderDataEntity(accountsResponse, userId, accessToken);

            // Store in Firestore
            storeProviderData(userId, portfolioData);

            log.info("Successfully stored Schwab portfolio data for user: {} with {} holdings",
                userId, portfolioData.getHoldings() != null ? portfolioData.getHoldings().size() : 0);

        } catch (Exception e) {
            log.error("Failed to fetch and store Schwab portfolio data for user: {}", userId, e);
            // Don't throw - OAuth is complete, data can be synced later
        }
    }

    /**
     * Transform Schwab API accounts response to ProviderDataEntity
     * @param accountsResponse The accounts response from Schwab API
     * @param userId The user ID
     * @param accessToken OAuth access token for fetching positions
     */
    private ProviderDataEntity transformToProviderDataEntity(Map<String, Object> accountsResponse, String userId, String accessToken) {
        ProviderDataEntity entity = new ProviderDataEntity();
        entity.setProviderId(PROVIDER_ID);
        entity.setProviderName(PROVIDER_NAME);
        entity.setAccountType("brokerage");
        entity.setSyncStatus("success");
        entity.setLastUpdatedAt(Instant.now());

        List<ProviderDataEntity.Holding> holdings = new ArrayList<>();
        BigDecimal totalValue = BigDecimal.ZERO;
        BigDecimal cashBalance = BigDecimal.ZERO;

        if (accountsResponse == null || accountsResponse.isEmpty()) {
            log.warn("No account data in Schwab response");
            entity.setHoldings(holdings);
            entity.setTotalValue(BigDecimal.ZERO);
            entity.setCashBalance(BigDecimal.ZERO);
            return entity;
        }

        // Schwab returns an array of account objects directly (not nested under "data")
        List<Map<String, Object>> accounts = null;
        if (accountsResponse instanceof List) {
            accounts = (List<Map<String, Object>>) accountsResponse;
        } else if (accountsResponse.containsKey("accounts")) {
            accounts = (List<Map<String, Object>>) accountsResponse.get("accounts");
        }

        if (accounts == null || accounts.isEmpty()) {
            log.warn("No accounts found in Schwab response");
            entity.setHoldings(holdings);
            entity.setTotalValue(BigDecimal.ZERO);
            entity.setCashBalance(BigDecimal.ZERO);
            return entity;
        }

        for (Map<String, Object> account : accounts) {
            try {
                // Extract account hash for fetching positions
                String accountHash = (String) account.get("hashValue");
                if (accountHash == null) {
                    log.warn("Skipping account with no hashValue");
                    continue;
                }

                // Get account type
                String accountType = (String) account.get("type");
                log.debug("Processing Schwab account: type={}, hash={}", accountType, accountHash);

                // Fetch positions for this account
                Map<String, Object> positionsResponse = schwabClient.getAccountPositions(accessToken, accountHash);

                if (positionsResponse == null) {
                    log.warn("No positions response for account {}", accountHash);
                    continue;
                }

                // Extract securities positions
                Map<String, Object> securitiesAccount = (Map<String, Object>) positionsResponse.get("securitiesAccount");
                if (securitiesAccount == null) {
                    log.warn("No securitiesAccount data for account {}", accountHash);
                    continue;
                }

                // Get cash balance from currentBalances
                Map<String, Object> currentBalances = (Map<String, Object>) securitiesAccount.get("currentBalances");
                if (currentBalances != null) {
                    Object cashEquivalentObj = currentBalances.get("cashBalance");
                    if (cashEquivalentObj != null) {
                        BigDecimal accountCash = new BigDecimal(cashEquivalentObj.toString()).setScale(2, RoundingMode.HALF_UP);
                        cashBalance = cashBalance.add(accountCash);
                    }
                }

                // Process positions
                List<Map<String, Object>> positions = (List<Map<String, Object>>) securitiesAccount.get("positions");
                if (positions == null || positions.isEmpty()) {
                    log.debug("No positions in account {}", accountHash);
                    continue;
                }

                for (Map<String, Object> position : positions) {
                    try {
                        // Extract instrument details
                        Map<String, Object> instrument = (Map<String, Object>) position.get("instrument");
                        if (instrument == null) {
                            continue;
                        }

                        String symbol = (String) instrument.get("symbol");
                        String assetType = (String) instrument.get("assetType");

                        if (symbol == null) {
                            continue;
                        }

                        // Get quantity
                        Object longQuantityObj = position.get("longQuantity");
                        if (longQuantityObj == null) {
                            continue;
                        }
                        BigDecimal quantity = new BigDecimal(longQuantityObj.toString()).setScale(8, RoundingMode.HALF_UP);

                        if (quantity.compareTo(BigDecimal.ZERO) == 0) {
                            continue;
                        }

                        // Get market value
                        Object marketValueObj = position.get("marketValue");
                        BigDecimal marketValue = marketValueObj != null ?
                            new BigDecimal(marketValueObj.toString()).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

                        // Calculate current price
                        BigDecimal currentPrice = quantity.compareTo(BigDecimal.ZERO) > 0 ?
                            marketValue.divide(quantity, 8, RoundingMode.HALF_UP) : BigDecimal.ZERO;

                        // Get average cost (cost basis per share)
                        Object averagePriceObj = position.get("averagePrice");
                        BigDecimal averagePrice = averagePriceObj != null ?
                            new BigDecimal(averagePriceObj.toString()).setScale(8, RoundingMode.HALF_UP) : null;

                        // Calculate total cost basis
                        BigDecimal costBasis = (averagePrice != null && quantity.compareTo(BigDecimal.ZERO) > 0) ?
                            averagePrice.multiply(quantity).setScale(2, RoundingMode.HALF_UP) : null;

                        // Create Holding
                        ProviderDataEntity.Holding holding = new ProviderDataEntity.Holding();
                        holding.setAsset(symbol);
                        holding.setName((String) instrument.get("description"));
                        holding.setQuantity(quantity);
                        holding.setCurrentPrice(currentPrice);
                        holding.setCurrentValue(marketValue);
                        holding.setCostBasis(costBasis);
                        holding.setAverageBuyPrice(averagePrice);
                        holding.setOriginalSymbol(symbol);
                        holding.setIsStaked(false);

                        holdings.add(holding);

                        // Accumulate total value
                        totalValue = totalValue.add(marketValue);

                    } catch (Exception e) {
                        log.error("Error transforming Schwab position", e);
                        // Continue with other positions
                    }
                }

            } catch (Exception e) {
                log.error("Error processing Schwab account", e);
                // Continue with other accounts
            }
        }

        entity.setHoldings(holdings);
        entity.setTotalValue(totalValue.add(cashBalance)); // Include cash in total value
        entity.setCashBalance(cashBalance);

        return entity;
    }

    /**
     * Store provider data entity in Firestore
     */
    private void storeProviderData(String userId, ProviderDataEntity entity) {
        try {
            log.info("=== SCHWAB PROVIDER DATA STORAGE ===");
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
            log.info("Successfully stored Schwab provider data at path: users/{}/provider_data/{}", userId, PROVIDER_ID);

        } catch (Exception e) {
            log.error("Failed to store Schwab provider data for user: {}", userId, e);
            throw e;
        }
    }
}