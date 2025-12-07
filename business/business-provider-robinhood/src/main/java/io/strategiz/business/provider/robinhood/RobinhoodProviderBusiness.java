package io.strategiz.business.provider.robinhood;

import io.strategiz.business.provider.robinhood.model.RobinhoodConnectionResult;
import io.strategiz.client.robinhood.RobinhoodClient;
import io.strategiz.client.robinhood.error.RobinhoodErrors;
import io.strategiz.client.robinhood.model.RobinhoodLoginResult;
import io.strategiz.data.provider.entity.ProviderIntegrationEntity;
import io.strategiz.data.provider.entity.ProviderDataEntity;
import io.strategiz.data.provider.repository.CreateProviderIntegrationRepository;
import io.strategiz.data.provider.repository.ReadProviderIntegrationRepository;
import io.strategiz.data.provider.repository.UpdateProviderIntegrationRepository;
import io.strategiz.data.provider.repository.CreateProviderDataRepository;
import io.strategiz.data.provider.repository.ReadProviderDataRepository;
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
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Business logic for Robinhood provider integration.
 * Handles credential-based OAuth flows (password grant) with MFA support.
 *
 * Key differences from standard OAuth providers (Schwab, Coinbase):
 * - Uses password grant instead of authorization code flow
 * - Requires user credentials to be sent to backend
 * - Multi-step MFA flow (login -> MFA code -> complete)
 *
 * Note: This is an unofficial API integration.
 */
@Component
public class RobinhoodProviderBusiness implements ProviderIntegrationHandler {

    private static final Logger log = LoggerFactory.getLogger(RobinhoodProviderBusiness.class);

    private static final String PROVIDER_ID = "robinhood";
    private static final String PROVIDER_NAME = "Robinhood";
    private static final String PROVIDER_TYPE = "equity";
    private static final String PROVIDER_CATEGORY = "brokerage";

    private final RobinhoodClient robinhoodClient;
    private final CreateProviderIntegrationRepository createProviderIntegrationRepository;
    private final ReadProviderIntegrationRepository readProviderIntegrationRepository;
    private final UpdateProviderIntegrationRepository updateProviderIntegrationRepository;
    private final CreateProviderDataRepository createProviderDataRepository;
    private final ReadProviderDataRepository readProviderDataRepository;
    private final SecretManager secretManager;

    @Autowired(required = false)
    private PortfolioSummaryManager portfolioSummaryManager;

    @Autowired
    public RobinhoodProviderBusiness(
            RobinhoodClient robinhoodClient,
            CreateProviderIntegrationRepository createProviderIntegrationRepository,
            ReadProviderIntegrationRepository readProviderIntegrationRepository,
            UpdateProviderIntegrationRepository updateProviderIntegrationRepository,
            @Autowired(required = false) CreateProviderDataRepository createProviderDataRepository,
            @Autowired(required = false) ReadProviderDataRepository readProviderDataRepository,
            @Qualifier("vaultSecretService") SecretManager secretManager) {
        this.robinhoodClient = robinhoodClient;
        this.createProviderIntegrationRepository = createProviderIntegrationRepository;
        this.readProviderIntegrationRepository = readProviderIntegrationRepository;
        this.updateProviderIntegrationRepository = updateProviderIntegrationRepository;
        this.createProviderDataRepository = createProviderDataRepository;
        this.readProviderDataRepository = readProviderDataRepository;
        this.secretManager = secretManager;
    }

    /**
     * Initiate Robinhood login with credentials.
     * This may trigger MFA if enabled on the account.
     *
     * @param userId User ID
     * @param username Robinhood username (email)
     * @param password Robinhood password
     * @param mfaType Preferred MFA type - "sms" or "email"
     * @return Connection result (may require MFA completion)
     */
    public RobinhoodConnectionResult initiateLogin(String userId, String username,
                                                    String password, String mfaType) {
        validateRequired("userId", userId);
        validateRequired("username", username);
        validateRequired("password", password);

        log.info("Initiating Robinhood login for user: {}", userId);

        try {
            // Store credentials temporarily for MFA completion
            storeTemporaryCredentials(userId, username, password);

            // Attempt login
            RobinhoodLoginResult loginResult = robinhoodClient.login(
                username, password, mfaType != null ? mfaType : "sms"
            );

            return processLoginResult(userId, loginResult);

        } catch (Exception e) {
            log.error("Failed to initiate Robinhood login for user: {}", userId, e);

            RobinhoodConnectionResult result = new RobinhoodConnectionResult();
            result.setUserId(userId);
            result.setProviderId(PROVIDER_ID);
            result.setConnectionStatus(RobinhoodConnectionResult.ConnectionStatus.ERROR);
            result.setErrorMessage(e.getMessage());
            return result;
        }
    }

    /**
     * Complete MFA challenge with the code received via SMS/email.
     *
     * @param userId User ID
     * @param challengeId Challenge ID from initial login
     * @param mfaCode MFA code received
     * @param deviceToken Device token from initial login
     * @return Connection result
     */
    public RobinhoodConnectionResult completeMfaChallenge(String userId, String challengeId,
                                                          String mfaCode, String deviceToken) {
        validateRequired("userId", userId);
        validateRequired("challengeId", challengeId);
        validateRequired("mfaCode", mfaCode);
        validateRequired("deviceToken", deviceToken);

        log.info("Completing MFA challenge for user: {}", userId);

        try {
            // Respond to challenge
            boolean validated = robinhoodClient.respondToChallenge(challengeId, mfaCode);

            if (!validated) {
                RobinhoodConnectionResult result = new RobinhoodConnectionResult();
                result.setUserId(userId);
                result.setProviderId(PROVIDER_ID);
                result.setConnectionStatus(RobinhoodConnectionResult.ConnectionStatus.ERROR);
                result.setErrorMessage("Invalid MFA code");
                result.setErrorCode("INVALID_MFA_CODE");
                return result;
            }

            // Retrieve stored credentials
            Map<String, Object> credentials = retrieveTemporaryCredentials(userId);
            String username = (String) credentials.get("username");
            String password = (String) credentials.get("password");

            // Complete login with validated challenge
            RobinhoodLoginResult loginResult = robinhoodClient.completeLoginAfterMfa(
                username, password, deviceToken, challengeId
            );

            // Clear temporary credentials
            clearTemporaryCredentials(userId);

            return processLoginResult(userId, loginResult);

        } catch (Exception e) {
            log.error("Failed to complete MFA for user: {}", userId, e);

            RobinhoodConnectionResult result = new RobinhoodConnectionResult();
            result.setUserId(userId);
            result.setProviderId(PROVIDER_ID);
            result.setConnectionStatus(RobinhoodConnectionResult.ConnectionStatus.ERROR);
            result.setErrorMessage(e.getMessage());
            return result;
        }
    }

    /**
     * Process login result and create connection result.
     */
    private RobinhoodConnectionResult processLoginResult(String userId, RobinhoodLoginResult loginResult) {
        RobinhoodConnectionResult result = new RobinhoodConnectionResult();
        result.setUserId(userId);
        result.setProviderId(PROVIDER_ID);
        result.setProviderName(PROVIDER_NAME);

        if (loginResult.isSuccess()) {
            result.setConnectionStatus(RobinhoodConnectionResult.ConnectionStatus.SUCCESS);
            result.setAccessToken(loginResult.getAccessToken());
            result.setRefreshToken(loginResult.getRefreshToken());
            result.setExpiresAt(Instant.now().plusSeconds(
                loginResult.getExpiresIn() != null ? loginResult.getExpiresIn() : 86400
            ));
            result.setConnectedAt(Instant.now());

            // Store tokens and complete integration
            completeIntegration(userId, result);

        } else if (loginResult.isMfaRequired()) {
            result.setConnectionStatus(RobinhoodConnectionResult.ConnectionStatus.MFA_REQUIRED);
            result.setChallenge(loginResult.getChallenge());
            result.setChallengeType(loginResult.getChallengeType());
            result.setDeviceToken(loginResult.getDeviceToken());

        } else if (loginResult.isDeviceApprovalRequired()) {
            result.setConnectionStatus(RobinhoodConnectionResult.ConnectionStatus.DEVICE_APPROVAL);
            result.setDeviceToken(loginResult.getDeviceToken());
            result.setErrorMessage("Please approve this device in your Robinhood app");

        } else {
            result.setConnectionStatus(RobinhoodConnectionResult.ConnectionStatus.ERROR);
            result.setErrorMessage(loginResult.getErrorMessage());
            result.setErrorCode(loginResult.getErrorCode());
        }

        return result;
    }

    /**
     * Complete the integration after successful authentication.
     */
    private void completeIntegration(String userId, RobinhoodConnectionResult result) {
        try {
            // Store tokens in Vault
            storeTokensInVault(userId, result.getAccessToken(),
                result.getRefreshToken(), result.getExpiresAt());

            // Create or update provider integration
            Optional<ProviderIntegrationEntity> existing =
                readProviderIntegrationRepository.findByUserIdAndProviderId(userId, PROVIDER_ID);

            if (existing.isPresent()) {
                ProviderIntegrationEntity entity = existing.get();
                entity.setStatus("connected");
                updateProviderIntegrationRepository.updateWithUserId(entity, userId);
            } else {
                ProviderIntegrationEntity entity = new ProviderIntegrationEntity(
                    PROVIDER_ID, "credentials", userId
                );
                entity.setStatus("connected");
                createProviderIntegrationRepository.createForUser(entity, userId);
            }

            // Fetch account info
            try {
                List<Map<String, Object>> accounts = robinhoodClient.getAccounts(result.getAccessToken());
                if (!accounts.isEmpty()) {
                    result.setAccountInfo(accounts.get(0));
                }
            } catch (Exception e) {
                log.warn("Could not fetch Robinhood accounts: {}", e.getMessage());
            }

            // Fetch and store portfolio data
            try {
                fetchAndStorePortfolioData(userId, result.getAccessToken());
            } catch (Exception e) {
                log.warn("Could not fetch portfolio data during connection: {}", e.getMessage());
            }

            log.info("Successfully completed Robinhood integration for user: {}", userId);

        } catch (Exception e) {
            log.error("Error completing Robinhood integration for user: {}", userId, e);
            throw new StrategizException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                "Failed to complete Robinhood integration: " + e.getMessage());
        }
    }

    /**
     * Sync provider data for a user.
     */
    public ProviderDataEntity syncProviderData(String userId) {
        log.info("Syncing Robinhood provider data for user: {}", userId);

        try {
            String accessToken = getValidAccessToken(userId);
            fetchAndStorePortfolioData(userId, accessToken);

            return readProviderDataRepository.getProviderData(userId, PROVIDER_ID);

        } catch (Exception e) {
            log.error("Failed to sync Robinhood data for user: {}", userId, e);
            throw new StrategizException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                "Failed to sync Robinhood data: " + e.getMessage());
        }
    }

    /**
     * Fetch and store portfolio data.
     */
    private void fetchAndStorePortfolioData(String userId, String accessToken) {
        if (createProviderDataRepository == null) {
            log.warn("Provider data repository not available");
            return;
        }

        try {
            // Get stock positions
            List<Map<String, Object>> positions = robinhoodClient.getNonzeroPositions(accessToken);

            // Get crypto holdings
            List<Map<String, Object>> cryptoHoldings = robinhoodClient.getCryptoHoldings(accessToken);

            // Get portfolio summary
            List<Map<String, Object>> portfolios = robinhoodClient.getPortfolios(accessToken);

            // Transform to ProviderDataEntity
            ProviderDataEntity entity = transformToProviderDataEntity(
                positions, cryptoHoldings, portfolios, userId, accessToken
            );

            // Store
            createProviderDataRepository.createOrReplaceProviderData(userId, PROVIDER_ID, entity);

            log.info("Stored Robinhood portfolio data for user: {} with {} holdings",
                userId, entity.getHoldings() != null ? entity.getHoldings().size() : 0);

            // Refresh portfolio summary to include this provider's data
            if (portfolioSummaryManager != null) {
                try {
                    portfolioSummaryManager.refreshPortfolioSummary(userId);
                    log.info("Refreshed portfolio summary after storing Robinhood data for user: {}", userId);
                } catch (Exception e) {
                    log.warn("Failed to refresh portfolio summary for user {}: {}", userId, e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Failed to fetch/store Robinhood portfolio data: {}", e.getMessage());
        }
    }

    /**
     * Transform Robinhood data to ProviderDataEntity.
     */
    private ProviderDataEntity transformToProviderDataEntity(
            List<Map<String, Object>> positions,
            List<Map<String, Object>> cryptoHoldings,
            List<Map<String, Object>> portfolios,
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
        BigDecimal totalProfitLoss = BigDecimal.ZERO;

        // Collect symbols for batch quote
        List<String> symbols = new ArrayList<>();
        for (Map<String, Object> position : positions) {
            String symbol = (String) position.get("symbol");
            if (symbol != null) {
                symbols.add(symbol);
            }
        }

        // Get quotes for all symbols
        Map<String, Object> quotes = new HashMap<>();
        if (!symbols.isEmpty()) {
            try {
                quotes = robinhoodClient.getQuotes(accessToken, symbols);
            } catch (Exception e) {
                log.warn("Could not fetch quotes: {}", e.getMessage());
            }
        }

        // Process stock positions
        for (Map<String, Object> position : positions) {
            ProviderDataEntity.Holding holding = transformPosition(position, quotes);
            if (holding != null) {
                holdings.add(holding);
                if (holding.getCurrentValue() != null) {
                    totalValue = totalValue.add(holding.getCurrentValue());
                }
                if (holding.getProfitLoss() != null) {
                    totalProfitLoss = totalProfitLoss.add(holding.getProfitLoss());
                }
            }
        }

        // Process crypto holdings
        for (Map<String, Object> crypto : cryptoHoldings) {
            ProviderDataEntity.Holding holding = transformCryptoHolding(crypto);
            if (holding != null) {
                holdings.add(holding);
                if (holding.getCurrentValue() != null) {
                    totalValue = totalValue.add(holding.getCurrentValue());
                }
            }
        }

        // Get cash balance from portfolio
        BigDecimal cashBalance = BigDecimal.ZERO;
        if (!portfolios.isEmpty()) {
            Map<String, Object> portfolio = portfolios.get(0);
            cashBalance = extractBigDecimal(portfolio, "withdrawable_amount");
            if (cashBalance == null) {
                cashBalance = BigDecimal.ZERO;
            }
        }

        entity.setHoldings(holdings);
        entity.setTotalValue(totalValue.add(cashBalance));
        entity.setCashBalance(cashBalance);
        entity.setTotalProfitLoss(totalProfitLoss);

        return entity;
    }

    /**
     * Transform a stock position to Holding.
     */
    private ProviderDataEntity.Holding transformPosition(Map<String, Object> position,
                                                          Map<String, Object> quotes) {
        String symbol = (String) position.get("symbol");
        if (symbol == null) {
            return null;
        }

        BigDecimal quantity = extractBigDecimal(position, "quantity");
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        BigDecimal averageBuyPrice = extractBigDecimal(position, "average_buy_price");

        // Get current price from quotes
        BigDecimal currentPrice = BigDecimal.ZERO;
        if (quotes.containsKey(symbol)) {
            Map<String, Object> quote = (Map<String, Object>) quotes.get(symbol);
            currentPrice = extractBigDecimal(quote, "last_trade_price");
            if (currentPrice == null) {
                currentPrice = extractBigDecimal(quote, "last_extended_hours_trade_price");
            }
        }

        if (currentPrice == null) {
            currentPrice = BigDecimal.ZERO;
        }

        BigDecimal currentValue = quantity.multiply(currentPrice).setScale(2, RoundingMode.HALF_UP);
        BigDecimal costBasis = averageBuyPrice != null ?
            quantity.multiply(averageBuyPrice).setScale(2, RoundingMode.HALF_UP) : null;

        BigDecimal profitLoss = null;
        BigDecimal profitLossPercent = null;
        if (costBasis != null && costBasis.compareTo(BigDecimal.ZERO) > 0) {
            profitLoss = currentValue.subtract(costBasis);
            profitLossPercent = profitLoss.divide(costBasis, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
        }

        ProviderDataEntity.Holding holding = new ProviderDataEntity.Holding();
        holding.setAsset(symbol);
        holding.setName((String) position.get("name"));
        holding.setAssetType("equity");
        holding.setQuantity(quantity);
        holding.setCurrentPrice(currentPrice);
        holding.setCurrentValue(currentValue);
        holding.setAverageBuyPrice(averageBuyPrice);
        holding.setCostBasis(costBasis);
        holding.setProfitLoss(profitLoss);
        holding.setProfitLossPercent(profitLossPercent);
        holding.setOriginalSymbol(symbol);

        return holding;
    }

    /**
     * Transform a crypto holding to Holding.
     */
    private ProviderDataEntity.Holding transformCryptoHolding(Map<String, Object> crypto) {
        String currencyCode = (String) crypto.get("currency_code");
        if (currencyCode == null) {
            return null;
        }

        BigDecimal quantity = extractBigDecimal(crypto, "quantity");
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        BigDecimal costBasis = extractBigDecimal(crypto, "cost_bases");
        BigDecimal currentValue = extractBigDecimal(crypto, "equity");

        BigDecimal currentPrice = BigDecimal.ZERO;
        if (currentValue != null && quantity.compareTo(BigDecimal.ZERO) > 0) {
            currentPrice = currentValue.divide(quantity, 8, RoundingMode.HALF_UP);
        }

        BigDecimal profitLoss = null;
        BigDecimal profitLossPercent = null;
        if (costBasis != null && currentValue != null && costBasis.compareTo(BigDecimal.ZERO) > 0) {
            profitLoss = currentValue.subtract(costBasis);
            profitLossPercent = profitLoss.divide(costBasis, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
        }

        ProviderDataEntity.Holding holding = new ProviderDataEntity.Holding();
        holding.setAsset(currencyCode);
        holding.setName(currencyCode + " (Crypto)");
        holding.setAssetType("crypto");
        holding.setQuantity(quantity);
        holding.setCurrentPrice(currentPrice);
        holding.setCurrentValue(currentValue);
        holding.setCostBasis(costBasis);
        holding.setProfitLoss(profitLoss);
        holding.setProfitLossPercent(profitLossPercent);
        holding.setOriginalSymbol(currencyCode);

        return holding;
    }

    /**
     * Get valid access token, refreshing if needed.
     */
    private String getValidAccessToken(String userId) {
        String secretKey = "users." + userId + ".providers.robinhood";
        Map<String, Object> secretData = secretManager.readSecretAsMap(secretKey);

        if (secretData == null || secretData.isEmpty()) {
            throw new StrategizException(ErrorCode.RESOURCE_NOT_FOUND,
                "No Robinhood tokens found for user: " + userId);
        }

        String accessToken = (String) secretData.get("accessToken");
        String refreshToken = (String) secretData.get("refreshToken");
        String expiresAtStr = (String) secretData.get("expiresAt");

        // Check if token needs refresh
        if (expiresAtStr != null) {
            try {
                Instant expiresAt = Instant.parse(expiresAtStr);
                if (expiresAt.isBefore(Instant.now().plusSeconds(300))) {
                    log.info("Robinhood token expiring soon, refreshing for user: {}", userId);
                    return refreshAccessToken(userId, refreshToken);
                }
            } catch (Exception e) {
                log.warn("Could not parse token expiration: {}", e.getMessage());
            }
        }

        return accessToken;
    }

    /**
     * Refresh access token.
     */
    private String refreshAccessToken(String userId, String refreshToken) {
        try {
            Map<String, Object> tokenData = robinhoodClient.refreshAccessToken(refreshToken);

            String newAccessToken = (String) tokenData.get("access_token");
            String newRefreshToken = (String) tokenData.get("refresh_token");
            Integer expiresIn = (Integer) tokenData.get("expires_in");

            if (newRefreshToken == null) {
                newRefreshToken = refreshToken;
            }

            Instant expiresAt = Instant.now().plusSeconds(expiresIn != null ? expiresIn : 86400);
            storeTokensInVault(userId, newAccessToken, newRefreshToken, expiresAt);

            return newAccessToken;

        } catch (Exception e) {
            log.error("Failed to refresh Robinhood token for user: {}", userId, e);
            throw new StrategizException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                "Failed to refresh Robinhood token. Please reconnect your account.");
        }
    }

    /**
     * Store tokens in Vault.
     */
    private void storeTokensInVault(String userId, String accessToken,
                                     String refreshToken, Instant expiresAt) {
        String secretKey = "users." + userId + ".providers.robinhood";

        Map<String, Object> secretData = new HashMap<>();
        secretData.put("accessToken", accessToken);
        secretData.put("refreshToken", refreshToken);
        secretData.put("expiresAt", expiresAt.toString());
        secretData.put("provider", PROVIDER_ID);
        secretData.put("storedAt", Instant.now().toString());

        secretManager.createSecret(secretKey, secretData);
        log.debug("Stored Robinhood tokens in Vault for user: {}", userId);
    }

    /**
     * Store temporary credentials for MFA completion.
     */
    private void storeTemporaryCredentials(String userId, String username, String password) {
        String secretKey = "users." + userId + ".providers.robinhood.temp";

        Map<String, Object> secretData = new HashMap<>();
        secretData.put("username", username);
        secretData.put("password", password);
        secretData.put("storedAt", Instant.now().toString());
        secretData.put("expiresAt", Instant.now().plusSeconds(600).toString()); // 10 min expiry

        secretManager.createSecret(secretKey, secretData);
    }

    /**
     * Retrieve temporary credentials.
     */
    private Map<String, Object> retrieveTemporaryCredentials(String userId) {
        String secretKey = "users." + userId + ".providers.robinhood.temp";
        Map<String, Object> secretData = secretManager.readSecretAsMap(secretKey);

        if (secretData == null || secretData.isEmpty()) {
            throw new StrategizException(ErrorCode.RESOURCE_NOT_FOUND,
                "No temporary credentials found. Please restart the login process.");
        }

        return secretData;
    }

    /**
     * Clear temporary credentials.
     */
    private void clearTemporaryCredentials(String userId) {
        try {
            String secretKey = "users." + userId + ".providers.robinhood.temp";
            secretManager.deleteSecret(secretKey);
        } catch (Exception e) {
            log.warn("Could not clear temporary credentials: {}", e.getMessage());
        }
    }

    // Utility methods

    private BigDecimal extractBigDecimal(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return new BigDecimal(value.toString());
        if (value instanceof String) {
            try {
                return new BigDecimal((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private void validateRequired(String paramName, String paramValue) {
        if (paramValue == null || paramValue.trim().isEmpty()) {
            throw new StrategizException(ErrorCode.VALIDATION_ERROR,
                "Required parameter '" + paramName + "' is missing");
        }
    }

    // ProviderIntegrationHandler interface implementation

    @Override
    public boolean testConnection(CreateProviderIntegrationRequest request, String userId) {
        log.info("Testing Robinhood connection for user: {}", userId);
        // Robinhood uses credential flow, can't test without credentials
        // Return true to indicate the flow should proceed
        return true;
    }

    @Override
    public ProviderIntegrationResult createIntegration(CreateProviderIntegrationRequest request, String userId) {
        log.info("Creating Robinhood integration for user: {}", userId);

        try {
            // Create pending integration
            ProviderIntegrationEntity entity = new ProviderIntegrationEntity(
                PROVIDER_ID, "credentials", userId
            );
            entity.setStatus("pending");
            createProviderIntegrationRepository.createForUser(entity, userId);

            // Build result - Robinhood requires credential flow, not redirect
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("flowType", "credentials");
            metadata.put("requiresMfa", true);
            metadata.put("mfaTypes", new String[]{"sms", "email"});
            metadata.put("note", "Submit credentials via /v1/providers/robinhood/login endpoint");

            ProviderIntegrationResult result = new ProviderIntegrationResult();
            result.setSuccess(true);
            result.setMessage("Robinhood integration initiated. Submit credentials to complete.");
            result.setMetadata(metadata);
            return result;

        } catch (Exception e) {
            log.error("Error creating Robinhood integration: {}", e.getMessage());
            throw new StrategizException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                "Failed to create Robinhood integration: " + e.getMessage());
        }
    }

    @Override
    public String getProviderId() {
        return PROVIDER_ID;
    }
}
