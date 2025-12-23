package io.strategiz.business.provider.plaid;

import io.strategiz.business.provider.plaid.constants.PlaidConstants;
import io.strategiz.business.provider.plaid.exception.PlaidProviderErrorDetails;
import io.strategiz.business.provider.plaid.model.PlaidAccount;
import io.strategiz.business.provider.plaid.model.PlaidConnectionResult;
import io.strategiz.client.plaid.PlaidClient;
import io.strategiz.client.plaid.model.PlaidAccessToken;
import io.strategiz.client.plaid.model.PlaidInvestmentHoldings;
import io.strategiz.client.plaid.model.PlaidLinkToken;
import io.strategiz.data.featureflags.service.FeatureFlagService;
import io.strategiz.data.provider.entity.PortfolioProviderEntity;
import io.strategiz.data.provider.entity.ProviderHoldingsEntity;
import io.strategiz.data.provider.repository.PortfolioProviderRepository;
import io.strategiz.data.provider.repository.ProviderHoldingsRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.framework.secrets.controller.SecretManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Business logic for Plaid provider integration.
 *
 * Plaid provides aggregated access to brokerage accounts via Plaid Link.
 * Flow:
 * 1. Frontend calls createLinkToken() to get a Link token
 * 2. User goes through Plaid Link UI to connect their brokerage
 * 3. Plaid returns a public_token
 * 4. Frontend calls exchangePublicToken() to convert to access_token
 * 5. Access token is stored in Vault for ongoing access
 * 6. syncHoldings() fetches and stores portfolio data
 */
@Service
public class PlaidProviderBusiness {

    private static final Logger log = LoggerFactory.getLogger(PlaidProviderBusiness.class);
    private static final String MODULE_NAME = "business-provider-plaid";

    private final PlaidClient plaidClient;
    private final FeatureFlagService featureFlagService;
    private final SecretManager secretManager;
    private final PortfolioProviderRepository portfolioProviderRepository;
    private final ProviderHoldingsRepository providerHoldingsRepository;

    @Value("${plaid.redirect-uri:}")
    private String redirectUri;

    @Autowired
    public PlaidProviderBusiness(
            PlaidClient plaidClient,
            FeatureFlagService featureFlagService,
            @Qualifier("vaultSecretService") SecretManager secretManager,
            PortfolioProviderRepository portfolioProviderRepository,
            ProviderHoldingsRepository providerHoldingsRepository) {
        this.plaidClient = plaidClient;
        this.featureFlagService = featureFlagService;
        this.secretManager = secretManager;
        this.portfolioProviderRepository = portfolioProviderRepository;
        this.providerHoldingsRepository = providerHoldingsRepository;
    }

    /**
     * Check if Plaid integration is enabled via feature flag.
     */
    public boolean isEnabled() {
        return featureFlagService.isEnabled(PlaidConstants.FEATURE_FLAG_KEY);
    }

    /**
     * Ensure Plaid is enabled, throwing an exception if not.
     */
    private void ensureEnabled() {
        if (!isEnabled()) {
            throw new StrategizException(
                PlaidProviderErrorDetails.PLAID_DISABLED,
                MODULE_NAME
            );
        }
    }

    /**
     * Create a Plaid Link token for the user.
     * This token is used by the frontend to initialize Plaid Link.
     *
     * @param userId User ID
     * @return Link token response
     */
    public PlaidLinkToken createLinkToken(String userId) {
        ensureEnabled();
        log.info("Creating Plaid Link token for user: {}", userId);

        try {
            // Check if user has existing Plaid connection that needs update
            String existingAccessToken = getAccessToken(userId);

            if (existingAccessToken != null) {
                // Create update mode Link token
                log.info("User has existing Plaid connection, creating update mode Link token");
                return plaidClient.createUpdateLinkToken(userId, existingAccessToken, redirectUri);
            }

            // Create new connection Link token
            List<String> products = Arrays.asList(
                PlaidConstants.PRODUCT_INVESTMENTS,
                PlaidConstants.PRODUCT_TRANSACTIONS
            );
            List<String> countryCodes = Collections.singletonList(PlaidConstants.COUNTRY_US);

            return plaidClient.createLinkToken(userId, products, countryCodes, redirectUri);

        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create Plaid Link token for user: {}", userId, e);
            throw new StrategizException(
                PlaidProviderErrorDetails.LINK_TOKEN_CREATION_FAILED,
                MODULE_NAME,
                e.getMessage()
            );
        }
    }

    /**
     * Exchange a Plaid public token for an access token.
     * Called after user completes Plaid Link.
     *
     * @param userId User ID
     * @param publicToken Public token from Plaid Link
     * @return Connection result with account info
     */
    public PlaidConnectionResult exchangePublicToken(String userId, String publicToken) {
        ensureEnabled();
        log.info("Exchanging Plaid public token for user: {}", userId);

        try {
            // Exchange for access token
            PlaidAccessToken accessToken = plaidClient.exchangePublicToken(publicToken);

            // Store access token in Vault
            storeAccessToken(userId, accessToken.getAccessToken(), accessToken.getItemId());
            log.info("Stored Plaid access token in Vault for user: {}", userId);

            // Get item info to retrieve institution
            var item = plaidClient.getItem(accessToken.getAccessToken());
            String institutionId = item != null ? item.getInstitutionId() : null;
            String institutionName = "Unknown Institution";

            if (institutionId != null) {
                try {
                    var institution = plaidClient.getInstitution(institutionId);
                    institutionName = institution != null ? institution.getName() : institutionName;
                } catch (Exception e) {
                    log.warn("Failed to get institution name: {}", e.getMessage());
                }
            }

            // Get initial holdings
            PlaidInvestmentHoldings holdings = plaidClient.getInvestmentHoldings(accessToken.getAccessToken());

            // Convert accounts
            List<PlaidAccount> accounts = holdings.getAccounts().stream()
                .map(this::convertAccount)
                .collect(Collectors.toList());

            // Save provider integration to Firestore
            saveProviderIntegration(userId, accessToken.getItemId(), institutionId, institutionName, accounts);

            // Sync and save holdings
            syncAndSaveHoldings(userId, accessToken.getAccessToken());

            return PlaidConnectionResult.success(accessToken.getItemId(), institutionName, accounts);

        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to exchange public token for user: {}", userId, e);
            throw new StrategizException(
                PlaidProviderErrorDetails.TOKEN_EXCHANGE_FAILED,
                MODULE_NAME,
                e.getMessage()
            );
        }
    }

    /**
     * Sync holdings for a user's Plaid connection.
     *
     * @param userId User ID
     * @return Updated holdings
     */
    public PlaidInvestmentHoldings syncHoldings(String userId) {
        ensureEnabled();
        log.info("Syncing Plaid holdings for user: {}", userId);

        String accessToken = getAccessToken(userId);
        if (accessToken == null) {
            throw new StrategizException(
                PlaidProviderErrorDetails.ITEM_NOT_FOUND,
                MODULE_NAME,
                userId
            );
        }

        try {
            PlaidInvestmentHoldings holdings = plaidClient.getInvestmentHoldings(accessToken);
            syncAndSaveHoldings(userId, accessToken);
            return holdings;

        } catch (Exception e) {
            log.error("Failed to sync Plaid holdings for user: {}", userId, e);
            throw new StrategizException(
                PlaidProviderErrorDetails.SYNC_FAILED,
                MODULE_NAME,
                userId
            );
        }
    }

    /**
     * Disconnect Plaid integration for a user.
     *
     * @param userId User ID
     * @return true if successfully disconnected
     */
    public boolean disconnect(String userId) {
        log.info("Disconnecting Plaid for user: {}", userId);

        try {
            String accessToken = getAccessToken(userId);
            if (accessToken != null) {
                // Remove item from Plaid
                plaidClient.removeItem(accessToken);
            }

            // Delete credentials from Vault
            deleteAccessToken(userId);

            // Delete provider integration from Firestore
            // TODO: Implement delete in repository

            log.info("Successfully disconnected Plaid for user: {}", userId);
            return true;

        } catch (Exception e) {
            log.error("Failed to disconnect Plaid for user: {}", userId, e);
            throw new StrategizException(
                PlaidProviderErrorDetails.DISCONNECT_FAILED,
                MODULE_NAME,
                e.getMessage()
            );
        }
    }

    /**
     * Check if user has an active Plaid connection.
     */
    public boolean hasConnection(String userId) {
        return getAccessToken(userId) != null;
    }

    // ========== Private Helper Methods ==========

    private void storeAccessToken(String userId, String accessToken, String itemId) {
        String vaultPath = PlaidConstants.VAULT_PATH_PREFIX + userId;
        Map<String, String> secrets = new HashMap<>();
        secrets.put("access_token", accessToken);
        secrets.put("item_id", itemId);
        secrets.put("created_at", Instant.now().toString());
        secretManager.storeSecret(vaultPath, secrets);
    }

    private String getAccessToken(String userId) {
        try {
            String vaultPath = PlaidConstants.VAULT_PATH_PREFIX + userId;
            Map<String, String> secrets = secretManager.getSecret(vaultPath);
            return secrets != null ? secrets.get("access_token") : null;
        } catch (Exception e) {
            log.debug("No Plaid access token found for user: {}", userId);
            return null;
        }
    }

    private void deleteAccessToken(String userId) {
        try {
            String vaultPath = PlaidConstants.VAULT_PATH_PREFIX + userId;
            secretManager.deleteSecret(vaultPath);
        } catch (Exception e) {
            log.warn("Failed to delete Plaid access token from Vault for user: {}", userId);
        }
    }

    private PlaidAccount convertAccount(com.plaid.client.model.AccountBase account) {
        PlaidAccount plaidAccount = new PlaidAccount();
        plaidAccount.setAccountId(account.getAccountId());
        plaidAccount.setName(account.getName());
        plaidAccount.setOfficialName(account.getOfficialName());
        plaidAccount.setType(account.getType() != null ? account.getType().toString() : null);
        plaidAccount.setSubtype(account.getSubtype() != null ? account.getSubtype().toString() : null);
        plaidAccount.setMask(account.getMask());

        if (account.getBalances() != null) {
            plaidAccount.setCurrentBalance(
                account.getBalances().getCurrent() != null
                    ? BigDecimal.valueOf(account.getBalances().getCurrent())
                    : null
            );
            plaidAccount.setAvailableBalance(
                account.getBalances().getAvailable() != null
                    ? BigDecimal.valueOf(account.getBalances().getAvailable())
                    : null
            );
            plaidAccount.setIsoCurrencyCode(account.getBalances().getIsoCurrencyCode());
        }

        return plaidAccount;
    }

    private void saveProviderIntegration(String userId, String itemId, String institutionId,
                                          String institutionName, List<PlaidAccount> accounts) {
        try {
            PortfolioProviderEntity entity = new PortfolioProviderEntity();
            entity.setProviderId(PlaidConstants.PROVIDER_ID);
            entity.setProviderName(institutionName);
            entity.setProviderType(PlaidConstants.PROVIDER_TYPE);
            entity.setStatus("connected");
            entity.setConnectionType("plaid_link");
            entity.setUserId(userId);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("itemId", itemId);
            metadata.put("institutionId", institutionId);
            metadata.put("accountCount", accounts.size());
            entity.setMetadata(metadata);

            portfolioProviderRepository.createForUser(entity, userId);
            log.info("Saved Plaid provider integration for user: {}", userId);

        } catch (Exception e) {
            log.warn("Failed to save provider integration, but continuing: {}", e.getMessage());
        }
    }

    private void syncAndSaveHoldings(String userId, String accessToken) {
        try {
            PlaidInvestmentHoldings holdings = plaidClient.getInvestmentHoldings(accessToken);

            // Convert to our holdings entity format
            List<ProviderHoldingsEntity.Holding> holdingsList = holdings.getHoldings().stream()
                .map(h -> {
                    ProviderHoldingsEntity.Holding holding = new ProviderHoldingsEntity.Holding();

                    // Find security for this holding
                    var security = holdings.findSecurity(h.getSecurityId());
                    if (security != null) {
                        holding.setSymbol(security.getTickerSymbol());
                        holding.setName(security.getName());
                        holding.setType(security.getType());
                    }

                    holding.setQuantity(h.getQuantity() != null ? BigDecimal.valueOf(h.getQuantity()) : BigDecimal.ZERO);
                    holding.setCostBasis(h.getCostBasis() != null ? BigDecimal.valueOf(h.getCostBasis()) : null);
                    holding.setCurrentValue(h.getInstitutionValue() != null ? BigDecimal.valueOf(h.getInstitutionValue()) : null);
                    holding.setCurrentPrice(h.getInstitutionPrice() != null ? BigDecimal.valueOf(h.getInstitutionPrice()) : null);

                    return holding;
                })
                .collect(Collectors.toList());

            ProviderHoldingsEntity holdingsEntity = new ProviderHoldingsEntity();
            holdingsEntity.setProviderId(PlaidConstants.PROVIDER_ID);
            holdingsEntity.setHoldings(holdingsList);
            holdingsEntity.setTotalValue(BigDecimal.valueOf(holdings.getTotalValue()));
            holdingsEntity.setLastSyncedAt(Instant.now().toString());
            holdingsEntity.setUserId(userId);

            providerHoldingsRepository.createForUser(holdingsEntity, userId);
            log.info("Saved Plaid holdings for user: {}, total value: {}", userId, holdings.getTotalValue());

        } catch (Exception e) {
            log.error("Failed to sync and save Plaid holdings for user: {}", userId, e);
        }
    }
}
