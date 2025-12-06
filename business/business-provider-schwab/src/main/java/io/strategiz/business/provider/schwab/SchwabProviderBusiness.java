package io.strategiz.business.provider.schwab;

import io.strategiz.business.provider.schwab.model.SchwabConnectionResult;
import io.strategiz.client.schwab.SchwabClient;
import io.strategiz.client.schwab.error.SchwabErrors;
import io.strategiz.data.provider.entity.ProviderIntegrationEntity;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Business logic for Charles Schwab provider integration.
 * Handles OAuth flows, API interactions, and business rules specific to Charles Schwab.
 *
 * Key features:
 * - OAuth 2.0 flow with automatic token refresh
 * - Portfolio data fetching with positions
 * - Transaction history for cost basis calculation
 * - Real-time quote fetching via Schwab Market Data API
 * - P&L calculations
 */
@Component
public class SchwabProviderBusiness implements ProviderIntegrationHandler {

    private static final Logger log = LoggerFactory.getLogger(SchwabProviderBusiness.class);

    private static final String PROVIDER_ID = "schwab";
    private static final String PROVIDER_NAME = "Charles Schwab";
    private static final String PROVIDER_TYPE = "equity";
    private static final String PROVIDER_CATEGORY = "brokerage";

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
            log.info("Exchanging Schwab authorization code for tokens for user: {}", userId);
            Map<String, Object> tokenData = schwabClient.exchangeCodeForTokens(authorizationCode);

            String accessToken = (String) tokenData.get("access_token");
            String refreshToken = (String) tokenData.get("refresh_token");
            Integer expiresIn = (Integer) tokenData.get("expires_in");

            if (accessToken == null || accessToken.isEmpty()) {
                throw new StrategizException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                    "Schwab token exchange did not return an access token");
            }

            log.info("Successfully obtained Schwab tokens for user: {}", userId);

            // Build connection result - tokens are the critical piece
            SchwabConnectionResult result = new SchwabConnectionResult();
            result.setUserId(userId);
            result.setProviderId(PROVIDER_ID);
            result.setProviderName(PROVIDER_NAME);
            result.setAccessToken(accessToken);
            result.setRefreshToken(refreshToken);
            result.setExpiresAt(Instant.now().plusSeconds(expiresIn != null ? expiresIn : 1800)); // 30 min default
            result.setConnectedAt(Instant.now());
            result.setStatus("connected");

            // Try to get user account information from Schwab (optional - don't fail OAuth if this fails)
            try {
                log.info("Fetching Schwab account info for user: {}", userId);
                List<Map<String, Object>> accounts = schwabClient.getAccounts(accessToken);
                result.setAccountInfo(accounts != null && !accounts.isEmpty() ? accounts.get(0) : null);
                log.info("Successfully fetched Schwab account info for user: {}", userId);
            } catch (Exception accountsError) {
                // Log but don't fail - account data can be synced later
                log.warn("Could not fetch Schwab accounts during OAuth callback (will sync later): {}",
                    accountsError.getMessage());
            }

            log.info("Successfully connected Charles Schwab for user: {}", userId);
            return result;

        } catch (StrategizException e) {
            log.error("Failed to handle Schwab OAuth callback for user: {}: {}", userId, e.getMessage());
            throw new StrategizException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                "Failed to complete Charles Schwab OAuth: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to handle Schwab OAuth callback for user: {}", userId, e);
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
                entity.setStatus("connected");
                updateProviderIntegrationRepository.updateWithUserId(entity, userId);
                log.info("Updated Schwab integration status to connected for user: {}", userId);
            } else {
                // Create new integration
                ProviderIntegrationEntity entity = new ProviderIntegrationEntity(PROVIDER_ID, "oauth", userId);
                entity.setStatus("connected");
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

            // Update the provider integration status to 'error' with error message
            try {
                Optional<ProviderIntegrationEntity> existingIntegration =
                    readProviderIntegrationRepository.findByUserIdAndProviderId(userId, PROVIDER_ID);
                if (existingIntegration.isPresent()) {
                    ProviderIntegrationEntity entity = existingIntegration.get();
                    entity.setStatus("error");
                    entity.setErrorMessage(e.getMessage());
                    updateProviderIntegrationRepository.updateWithUserId(entity, userId);
                    log.info("Updated Schwab integration status to error for user: {}", userId);
                }
            } catch (Exception updateError) {
                log.error("Failed to update error status for user: {}", userId, updateError);
            }

            throw new StrategizException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                "Failed to complete Charles Schwab OAuth flow: " + e.getMessage());
        }
    }

    /**
     * Sync provider data for a specific user.
     * Retrieves access token from Vault and fetches latest portfolio data from Schwab.
     *
     * @param userId The user ID
     * @return ProviderDataEntity with synced data
     * @throws RuntimeException if sync fails
     */
    public ProviderDataEntity syncProviderData(String userId) {
        log.info("Syncing Schwab provider data for user: {}", userId);

        try {
            // Get access token, refreshing if needed
            String accessToken = getValidAccessToken(userId);

            // Fetch and store portfolio data
            fetchAndStorePortfolioData(userId, accessToken);

            // Retrieve and return the stored data
            ProviderDataEntity providerData = readProviderDataRepository.getProviderData(userId, PROVIDER_ID);

            if (providerData != null) {
                log.info("Successfully synced Schwab data for user: {}", userId);
                return providerData;
            } else {
                log.warn("No provider data found after sync for user: {}", userId);
                throw new StrategizException(ErrorCode.RESOURCE_NOT_FOUND,
                    "No Charles Schwab provider data found after sync for user: " + userId);
            }

        } catch (StrategizException e) {
            log.error("Failed to sync Schwab provider data for user: {}: {}", userId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Failed to sync Schwab provider data for user: {}", userId, e);
            throw new StrategizException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                "Failed to sync Charles Schwab provider data: " + e.getMessage());
        }
    }

    /**
     * Get a valid access token, refreshing if necessary.
     * Schwab access tokens expire after 30 minutes.
     */
    private String getValidAccessToken(String userId) {
        // Use dot notation for secretManager - it will convert to:
        // secret/data/strategiz/users/{userId}/providers/schwab
        String secretKey = "users." + userId + ".providers.schwab.accessToken";
        Map<String, Object> secretData = secretManager.readSecretAsMap(secretKey);

        if (secretData == null || secretData.isEmpty()) {
            throw new StrategizException(ErrorCode.RESOURCE_NOT_FOUND,
                "No Charles Schwab tokens found in Vault for user: " + userId);
        }

        String accessToken = (String) secretData.get("accessToken");
        String refreshToken = (String) secretData.get("refreshToken");
        String expiresAtStr = (String) secretData.get("expiresAt");

        if (accessToken == null || accessToken.isEmpty()) {
            throw new StrategizException(ErrorCode.RESOURCE_NOT_FOUND,
                "Charles Schwab access token not found in Vault for user: " + userId);
        }

        // Check if token is expired or about to expire (within 5 minutes)
        if (expiresAtStr != null) {
            try {
                Instant expiresAt = Instant.parse(expiresAtStr);
                Instant fiveMinutesFromNow = Instant.now().plusSeconds(300);

                if (expiresAt.isBefore(fiveMinutesFromNow) && refreshToken != null) {
                    log.info("Schwab access token expired or expiring soon, refreshing for user: {}", userId);
                    return refreshAccessToken(userId, refreshToken);
                }
            } catch (Exception e) {
                log.warn("Could not parse token expiration time, using existing token: {}", e.getMessage());
            }
        }

        return accessToken;
    }

    /**
     * Refresh the access token using the refresh token.
     */
    private String refreshAccessToken(String userId, String refreshToken) {
        try {
            Map<String, Object> tokenData = schwabClient.refreshAccessToken(refreshToken);

            String newAccessToken = (String) tokenData.get("access_token");
            String newRefreshToken = (String) tokenData.get("refresh_token");
            Integer expiresIn = (Integer) tokenData.get("expires_in");

            // Use new refresh token if provided, otherwise keep the old one
            if (newRefreshToken == null) {
                newRefreshToken = refreshToken;
            }

            Instant expiresAt = Instant.now().plusSeconds(expiresIn != null ? expiresIn : 1800);

            // Store updated tokens
            storeTokensInVault(userId, newAccessToken, newRefreshToken, expiresAt);

            log.info("Successfully refreshed Schwab access token for user: {}", userId);
            return newAccessToken;

        } catch (Exception e) {
            log.error("Failed to refresh Schwab access token for user: {}", userId, e);
            throw new StrategizException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                "Failed to refresh Charles Schwab access token. Please reconnect your account.");
        }
    }

    /**
     * Fetch portfolio data from Schwab and store in provider_data collection.
     * This includes positions, balances, and transaction history for cost basis calculation.
     */
    private void fetchAndStorePortfolioData(String userId, String accessToken) {
        log.info("Fetching Schwab portfolio data for user: {}", userId);

        if (createProviderDataRepository == null) {
            log.warn("Provider data repository not available - skipping portfolio data fetch");
            return;
        }

        try {
            // Fetch accounts with positions from Schwab
            List<Map<String, Object>> accountsWithPositions = schwabClient.getAccountsWithPositions(accessToken);

            // Transform to ProviderDataEntity with Holdings
            ProviderDataEntity portfolioData = transformToProviderDataEntity(accountsWithPositions, userId, accessToken);

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
     * Transform Schwab API accounts response to ProviderDataEntity.
     * Schwab returns account data with securitiesAccount nested structure.
     *
     * Note: Schwab provides averagePrice directly in position data, so we don't need
     * to fetch transactions separately for cost basis calculation.
     */
    private ProviderDataEntity transformToProviderDataEntity(List<Map<String, Object>> accounts,
                                                             String userId, String accessToken) {
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
        BigDecimal totalProfitLoss = BigDecimal.ZERO;

        if (accounts == null || accounts.isEmpty()) {
            log.warn("No account data in Schwab response");
            entity.setHoldings(holdings);
            entity.setTotalValue(BigDecimal.ZERO);
            entity.setCashBalance(BigDecimal.ZERO);
            return entity;
        }

        // Collect all symbols for batch quote request
        List<String> allSymbols = new ArrayList<>();

        for (Map<String, Object> account : accounts) {
            try {
                // Get securities account data (where positions and balances live)
                Map<String, Object> securitiesAccount = (Map<String, Object>) account.get("securitiesAccount");
                if (securitiesAccount == null) {
                    log.warn("No securitiesAccount data in account");
                    continue;
                }

                // Get cash balance from currentBalances
                Map<String, Object> currentBalances = (Map<String, Object>) securitiesAccount.get("currentBalances");
                if (currentBalances != null) {
                    BigDecimal accountCash = extractBigDecimal(currentBalances, "cashBalance");
                    if (accountCash != null) {
                        cashBalance = cashBalance.add(accountCash);
                    }
                }

                // Process positions
                List<Map<String, Object>> positions = (List<Map<String, Object>>) securitiesAccount.get("positions");
                if (positions == null || positions.isEmpty()) {
                    log.debug("No positions in account");
                    continue;
                }

                // Collect symbols for batch quote
                for (Map<String, Object> position : positions) {
                    Map<String, Object> instrument = (Map<String, Object>) position.get("instrument");
                    if (instrument != null) {
                        String symbol = (String) instrument.get("symbol");
                        if (symbol != null && !allSymbols.contains(symbol)) {
                            allSymbols.add(symbol);
                        }
                    }
                }

                // Process each position - Schwab provides averagePrice directly
                for (Map<String, Object> position : positions) {
                    try {
                        ProviderDataEntity.Holding holding = transformPosition(position);
                        if (holding != null) {
                            holdings.add(holding);

                            // Accumulate totals
                            if (holding.getCurrentValue() != null) {
                                totalValue = totalValue.add(holding.getCurrentValue());
                            }
                            if (holding.getProfitLoss() != null) {
                                totalProfitLoss = totalProfitLoss.add(holding.getProfitLoss());
                            }
                        }
                    } catch (Exception e) {
                        log.error("Error transforming Schwab position", e);
                    }
                }

            } catch (Exception e) {
                log.error("Error processing Schwab account", e);
            }
        }

        // Fetch real-time quotes for all symbols and update prices
        if (!allSymbols.isEmpty()) {
            try {
                Map<String, Object> quotes = schwabClient.getQuotes(accessToken, allSymbols);
                updateHoldingsWithQuotes(holdings, quotes);
                // Recalculate totals after price updates
                totalValue = recalculateTotalValue(holdings, cashBalance);
            } catch (Exception e) {
                log.warn("Could not fetch real-time quotes: {}", e.getMessage());
            }
        }

        // Add cash as a visible holding if there's a cash balance
        if (cashBalance.compareTo(BigDecimal.ZERO) > 0) {
            ProviderDataEntity.Holding cashHolding = new ProviderDataEntity.Holding();
            cashHolding.setAsset("CASH");
            cashHolding.setName("Cash & Money Market");
            cashHolding.setAssetType("cash");
            cashHolding.setQuantity(BigDecimal.ONE);
            cashHolding.setCurrentPrice(cashBalance);
            cashHolding.setCurrentValue(cashBalance);
            cashHolding.setCostBasis(cashBalance); // Cash has no gain/loss
            cashHolding.setProfitLoss(BigDecimal.ZERO);
            cashHolding.setProfitLossPercent(BigDecimal.ZERO);
            holdings.add(cashHolding);
            log.info("Added cash holding with value: {}", cashBalance);
        }

        entity.setHoldings(holdings);
        entity.setTotalValue(totalValue); // totalValue already includes cash from recalculateTotalValue
        entity.setCashBalance(cashBalance);
        entity.setTotalProfitLoss(totalProfitLoss);

        // Calculate total P&L percent
        BigDecimal totalCostBasis = holdings.stream()
            .map(ProviderDataEntity.Holding::getCostBasis)
            .filter(cb -> cb != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalCostBasis.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal totalProfitLossPercent = totalProfitLoss
                .divide(totalCostBasis, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
            entity.setTotalProfitLossPercent(totalProfitLossPercent);
        }

        return entity;
    }

    /**
     * Transform a single Schwab position to a Holding.
     * Schwab provides averagePrice directly in position data for cost basis calculation.
     */
    private ProviderDataEntity.Holding transformPosition(Map<String, Object> position) {
        Map<String, Object> instrument = (Map<String, Object>) position.get("instrument");
        if (instrument == null) {
            return null;
        }

        String symbol = (String) instrument.get("symbol");
        if (symbol == null) {
            return null;
        }

        // Get quantity (longQuantity for long positions)
        BigDecimal quantity = extractBigDecimal(position, "longQuantity");
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        // Get market value
        BigDecimal marketValue = extractBigDecimal(position, "marketValue");
        if (marketValue == null) {
            marketValue = BigDecimal.ZERO;
        }

        // Calculate current price from market value / quantity
        BigDecimal currentPrice = quantity.compareTo(BigDecimal.ZERO) > 0 ?
            marketValue.divide(quantity, 8, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        // Get average price from position data - Schwab provides this directly
        BigDecimal averagePrice = extractBigDecimal(position, "averagePrice");

        // Calculate cost basis from averagePrice * quantity
        BigDecimal costBasis = null;
        if (averagePrice != null && quantity.compareTo(BigDecimal.ZERO) > 0) {
            costBasis = averagePrice.multiply(quantity).setScale(2, RoundingMode.HALF_UP);
        }

        // Calculate P&L
        BigDecimal profitLoss = null;
        BigDecimal profitLossPercent = null;
        if (costBasis != null && marketValue != null) {
            profitLoss = marketValue.subtract(costBasis).setScale(2, RoundingMode.HALF_UP);
            if (costBasis.compareTo(BigDecimal.ZERO) > 0) {
                profitLossPercent = profitLoss
                    .divide(costBasis, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            }
        }

        // Get day P&L from position data - Schwab provides these directly
        BigDecimal currentDayProfitLoss = extractBigDecimal(position, "currentDayProfitLoss");
        BigDecimal currentDayProfitLossPercent = extractBigDecimal(position, "currentDayProfitLossPercentage");

        // Create Holding
        ProviderDataEntity.Holding holding = new ProviderDataEntity.Holding();
        holding.setAsset(symbol);
        holding.setName((String) instrument.get("description"));
        holding.setQuantity(quantity.setScale(8, RoundingMode.HALF_UP));
        holding.setCurrentPrice(currentPrice);
        holding.setCurrentValue(marketValue.setScale(2, RoundingMode.HALF_UP));
        holding.setCostBasis(costBasis);
        holding.setAverageBuyPrice(averagePrice);
        holding.setProfitLoss(profitLoss);
        holding.setProfitLossPercent(profitLossPercent);
        holding.setPriceChange24h(currentDayProfitLoss);
        holding.setOriginalSymbol(symbol);
        holding.setIsStaked(false);

        // Set asset type based on instrument type
        String assetType = (String) instrument.get("assetType");
        if (assetType != null) {
            holding.setAssetType(assetType.toLowerCase());
        }

        return holding;
    }

    /**
     * Calculate cost basis from transaction history.
     * Uses TRADE transactions (buy/sell) to calculate average cost.
     */
    private Map<String, CostBasisData> calculateCostBasisFromTransactions(String accessToken, String accountHash) {
        Map<String, CostBasisData> costBasisMap = new HashMap<>();

        try {
            // Fetch trade transactions for the last year
            List<Map<String, Object>> transactions = schwabClient.getTradeTransactions(accessToken, accountHash);

            if (transactions == null || transactions.isEmpty()) {
                log.debug("No trade transactions found for account {}", accountHash);
                return costBasisMap;
            }

            // Group transactions by symbol and calculate cost basis
            Map<String, List<Map<String, Object>>> txBySymbol = new HashMap<>();
            for (Map<String, Object> tx : transactions) {
                Map<String, Object> transactionItem = (Map<String, Object>) tx.get("transactionItem");
                if (transactionItem != null) {
                    Map<String, Object> instrument = (Map<String, Object>) transactionItem.get("instrument");
                    if (instrument != null) {
                        String symbol = (String) instrument.get("symbol");
                        if (symbol != null) {
                            txBySymbol.computeIfAbsent(symbol, k -> new ArrayList<>()).add(tx);
                        }
                    }
                }
            }

            // Calculate cost basis for each symbol
            for (Map.Entry<String, List<Map<String, Object>>> entry : txBySymbol.entrySet()) {
                String symbol = entry.getKey();
                List<Map<String, Object>> symbolTxs = entry.getValue();

                BigDecimal totalCost = BigDecimal.ZERO;
                BigDecimal totalQuantity = BigDecimal.ZERO;

                for (Map<String, Object> tx : symbolTxs) {
                    Map<String, Object> transactionItem = (Map<String, Object>) tx.get("transactionItem");
                    if (transactionItem == null) continue;

                    String instruction = (String) transactionItem.get("instruction");
                    BigDecimal amount = extractBigDecimal(transactionItem, "amount");
                    BigDecimal cost = extractBigDecimal(transactionItem, "cost");
                    BigDecimal price = extractBigDecimal(transactionItem, "price");

                    if (amount == null) continue;

                    // Only count BUY transactions for cost basis
                    if ("BUY".equalsIgnoreCase(instruction)) {
                        totalQuantity = totalQuantity.add(amount.abs());
                        if (cost != null) {
                            totalCost = totalCost.add(cost.abs());
                        } else if (price != null) {
                            totalCost = totalCost.add(price.multiply(amount.abs()));
                        }
                    }
                }

                if (totalQuantity.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal avgPrice = totalCost.divide(totalQuantity, 8, RoundingMode.HALF_UP);
                    costBasisMap.put(symbol, new CostBasisData(totalCost.setScale(2, RoundingMode.HALF_UP), avgPrice));
                    log.debug("Calculated cost basis for {}: totalCost={}, avgPrice={}",
                        symbol, totalCost, avgPrice);
                }
            }

        } catch (Exception e) {
            log.warn("Error calculating cost basis from transactions: {}", e.getMessage());
        }

        return costBasisMap;
    }

    /**
     * Update holdings with real-time quote data.
     */
    private void updateHoldingsWithQuotes(List<ProviderDataEntity.Holding> holdings, Map<String, Object> quotes) {
        if (quotes == null || quotes.isEmpty()) {
            return;
        }

        for (ProviderDataEntity.Holding holding : holdings) {
            String symbol = holding.getAsset();
            if (symbol == null || !quotes.containsKey(symbol)) {
                continue;
            }

            try {
                Map<String, Object> quoteData = (Map<String, Object>) quotes.get(symbol);
                if (quoteData == null) continue;

                // Quote data might be nested under "quote" key
                Map<String, Object> quote = quoteData.containsKey("quote") ?
                    (Map<String, Object>) quoteData.get("quote") : quoteData;

                // Update current price
                BigDecimal lastPrice = extractBigDecimal(quote, "lastPrice");
                if (lastPrice == null) {
                    lastPrice = extractBigDecimal(quote, "regularMarketLastPrice");
                }

                if (lastPrice != null && holding.getQuantity() != null) {
                    holding.setCurrentPrice(lastPrice);
                    BigDecimal newValue = lastPrice.multiply(holding.getQuantity())
                        .setScale(2, RoundingMode.HALF_UP);
                    holding.setCurrentValue(newValue);

                    // Recalculate P&L with new price
                    if (holding.getCostBasis() != null) {
                        BigDecimal pl = newValue.subtract(holding.getCostBasis());
                        holding.setProfitLoss(pl);
                        if (holding.getCostBasis().compareTo(BigDecimal.ZERO) > 0) {
                            BigDecimal plPercent = pl.divide(holding.getCostBasis(), 4, RoundingMode.HALF_UP)
                                .multiply(new BigDecimal("100"));
                            holding.setProfitLossPercent(plPercent);
                        }
                    }
                }

                // Update 24h change if available
                BigDecimal netChange = extractBigDecimal(quote, "netChange");
                if (netChange != null) {
                    holding.setPriceChange24h(netChange);
                }

            } catch (Exception e) {
                log.warn("Error updating quote for {}: {}", symbol, e.getMessage());
            }
        }
    }

    /**
     * Recalculate total value from holdings.
     */
    private BigDecimal recalculateTotalValue(List<ProviderDataEntity.Holding> holdings, BigDecimal cashBalance) {
        BigDecimal total = cashBalance != null ? cashBalance : BigDecimal.ZERO;
        for (ProviderDataEntity.Holding holding : holdings) {
            if (holding.getCurrentValue() != null) {
                total = total.add(holding.getCurrentValue());
            }
        }
        return total;
    }

    /**
     * Extract BigDecimal from map, handling various number types.
     */
    private BigDecimal extractBigDecimal(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return new BigDecimal(value.toString());
        }
        if (value instanceof String) {
            try {
                return new BigDecimal((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private void storeTokensInVault(String userId, String accessToken, String refreshToken, Instant expiresAt) {
        try {
            // Use dot notation for secretManager - it will convert to:
            // secret/data/strategiz/users/{userId}/providers/schwab
            String secretKey = "users." + userId + ".providers.schwab";

            Map<String, Object> secretData = new HashMap<>();
            secretData.put("accessToken", accessToken);
            secretData.put("refreshToken", refreshToken);
            secretData.put("expiresAt", expiresAt.toString());
            secretData.put("provider", PROVIDER_ID);
            secretData.put("storedAt", Instant.now().toString());

            secretManager.createSecret(secretKey, secretData);
            log.debug("Stored Charles Schwab OAuth tokens in Vault for user: {}", userId);

        } catch (Exception e) {
            log.error("Failed to store Charles Schwab tokens for user: {}", userId, e);
            throw new StrategizException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                "Failed to store Charles Schwab tokens in Vault: " + e.getMessage());
        }
    }

    private void storeProviderData(String userId, ProviderDataEntity entity) {
        try {
            log.info("=== SCHWAB PROVIDER DATA STORAGE ===");
            log.info("User ID: {}", userId);
            log.info("Provider ID: {}", PROVIDER_ID);
            log.info("Holdings count: {}", entity.getHoldings() != null ? entity.getHoldings().size() : 0);
            log.info("Total value: {}", entity.getTotalValue());
            log.info("Cash balance: {}", entity.getCashBalance());

            if (entity.getHoldings() != null && !entity.getHoldings().isEmpty()) {
                log.info("First holding: asset={}, quantity={}, value={}",
                    entity.getHoldings().get(0).getAsset(),
                    entity.getHoldings().get(0).getQuantity(),
                    entity.getHoldings().get(0).getCurrentValue());
            }

            // Use createOrReplace to handle both create and update
            createProviderDataRepository.createOrReplaceProviderData(userId, PROVIDER_ID, entity);
            log.info("Successfully stored Schwab provider data at path: users/{}/provider_data/{}", userId, PROVIDER_ID);

        } catch (Exception e) {
            log.error("Failed to store Schwab provider data for user: {}", userId, e);
            throw e;
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
            throw new StrategizException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                "Failed to create Charles Schwab integration: " + e.getMessage());
        }
    }

    @Override
    public String getProviderId() {
        return PROVIDER_ID;
    }

    /**
     * Helper class to hold cost basis calculation results.
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
