package io.strategiz.business.provider.webull.business;

import io.strategiz.business.provider.webull.constants.WebullConstants;
import io.strategiz.business.provider.webull.exception.WebullProviderErrorDetails;
import io.strategiz.business.provider.webull.helper.WebullDataInitializer;
import io.strategiz.client.webull.auth.WebullApiAuthClient;
import io.strategiz.client.webull.auth.manager.WebullCredentialManager;
import io.strategiz.data.provider.entity.ProviderIntegrationEntity;
import io.strategiz.data.provider.entity.ProviderDataEntity;
import io.strategiz.data.provider.repository.CreateProviderIntegrationRepository;
import io.strategiz.business.base.provider.model.CreateProviderIntegrationRequest;
import io.strategiz.business.base.provider.model.ProviderIntegrationResult;
import io.strategiz.business.provider.base.BaseApiKeyProviderHandler;
import io.strategiz.framework.exception.StrategizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Business logic for Webull provider integration
 * Implements dual storage: credentials in Vault, metadata in Firestore
 */
@Service
public class WebullProviderBusiness extends BaseApiKeyProviderHandler {

    private static final Logger log = LoggerFactory.getLogger(WebullProviderBusiness.class);

    private static final String MODULE_NAME = "business-provider-webull";

    private final WebullApiAuthClient webullApiAuthClient;
    private final WebullCredentialManager webullCredentialManager;
    private final WebullDataInitializer dataInitializer;

    @Autowired(required = false)
    private CreateProviderIntegrationRepository providerIntegrationRepository;

    @Autowired
    public WebullProviderBusiness(
            WebullApiAuthClient webullApiAuthClient,
            WebullCredentialManager webullCredentialManager,
            WebullDataInitializer dataInitializer) {
        this.webullApiAuthClient = webullApiAuthClient;
        this.webullCredentialManager = webullCredentialManager;
        this.dataInitializer = dataInitializer;
    }

    @Override
    protected Map<String, String> getStoredCredentials(String userId) {
        try {
            return webullCredentialManager.getCredentialsAsMap(userId);
        } catch (Exception e) {
            log.error("Error retrieving Webull credentials for user: {}", userId, e);
            return null;
        }
    }

    @Override
    protected boolean testConnectionWithStoredCredentials(Map<String, String> storedCredentials, String userId) {
        String appKey = storedCredentials.get("appKey");
        String appSecret = storedCredentials.get("appSecret");

        // Validate credentials
        if (appKey == null || appKey.trim().isEmpty()) {
            log.warn("App key is missing in stored credentials for user: {}", userId);
            return false;
        }

        if (appSecret == null || appSecret.trim().isEmpty()) {
            log.warn("App secret is missing in stored credentials for user: {}", userId);
            return false;
        }

        log.debug("Testing connection with stored credentials from Vault");

        try {
            // Test connection by getting account list
            Boolean isConnected = webullApiAuthClient.testConnection(appKey, appSecret).block();

            if (isConnected == null || !isConnected) {
                log.warn("Connection test failed for user: {}", userId);
                return false;
            }

            log.info("Webull connection test successful for user: {}", userId);
            return true;

        } catch (Exception e) {
            log.error("Error during Webull API call for user: {} - Error: {}", userId, e.getMessage());
            return false;
        }
    }

    @Override
    public ProviderIntegrationResult createIntegration(CreateProviderIntegrationRequest request, String userId) {
        log.info("Creating Webull integration for user: {}", userId);

        try {
            // 1. Extract credentials from request
            // For Webull, we use apiKey -> appKey, apiSecret -> appSecret
            String appKey = request.getApiKey();
            String appSecret = request.getApiSecret();
            String accountId = null; // Will be fetched from API

            // 2. Store credentials in Vault
            webullCredentialManager.storeCredentials(userId, appKey, appSecret, accountId);
            log.info("Stored Webull credentials in Vault for user: {}", userId);

            // 3. Fetch account list to get account ID
            try {
                Map<String, Object> accountListResponse = webullApiAuthClient.getAccountList(appKey, appSecret).block();
                if (accountListResponse != null && accountListResponse.containsKey("data")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> accounts = (List<Map<String, Object>>) accountListResponse.get("data");
                    if (accounts != null && !accounts.isEmpty()) {
                        // Use the first account
                        Map<String, Object> firstAccount = accounts.get(0);
                        accountId = String.valueOf(firstAccount.get("account_id"));
                        // Update credentials with account ID
                        webullCredentialManager.updateAccountId(userId, accountId);
                        log.info("Retrieved and stored account ID for user: {}", userId);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to fetch account list, continuing without account ID: {}", e.getMessage());
            }

            // 4. Create provider integration entity for Firestore
            if (providerIntegrationRepository != null) {
                try {
                    ProviderIntegrationEntity entity = new ProviderIntegrationEntity(
                            WebullConstants.PROVIDER_ID, "api_key", userId);
                    entity.setStatus("connected");

                    ProviderIntegrationEntity savedEntity = providerIntegrationRepository.createForUser(entity, userId);
                    log.info("Saved Webull provider integration to Firestore for user: {}", userId);
                } catch (Exception e) {
                    log.error("Failed to save provider integration to Firestore, but continuing: {}", e.getMessage());
                }
            } else {
                log.warn("ProviderIntegrationRepository not available, skipping Firestore storage");
            }

            // 5. Initialize and store portfolio data
            ProviderDataEntity portfolioData = null;
            if (accountId != null) {
                try {
                    portfolioData = dataInitializer.initializeAndStoreData(userId, appKey, appSecret, accountId);
                    log.info("Successfully initialized portfolio data for user: {}, total value: {}",
                            userId, portfolioData.getTotalValue());
                } catch (Exception e) {
                    log.error("Failed to initialize portfolio data, but connection saved: {}", e.getMessage());
                }
            }

            // 6. Build result
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("hasAccountId", accountId != null && !accountId.trim().isEmpty());
            metadata.put("connectionMethod", "api_key");
            metadata.put("apiVersion", "1.0");

            if (portfolioData != null) {
                metadata.put("initialSync", "success");
                metadata.put("totalValue", portfolioData.getTotalValue());
                metadata.put("holdingsCount", portfolioData.getHoldings() != null ?
                        portfolioData.getHoldings().size() : 0);
                metadata.put("cashBalance", portfolioData.getCashBalance());
            } else {
                metadata.put("initialSync", accountId != null ? "failed" : "pending");
            }

            ProviderIntegrationResult result = new ProviderIntegrationResult();
            result.setSuccess(true);
            result.setMessage("Webull integration created successfully");
            result.setMetadata(metadata);
            return result;

        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating Webull integration for user: {}", userId, e);
            throw new StrategizException(
                    WebullProviderErrorDetails.CONNECTION_FAILED,
                    MODULE_NAME,
                    userId,
                    e.getMessage()
            );
        }
    }

    @Override
    public String getProviderId() {
        return WebullConstants.PROVIDER_ID;
    }

    /**
     * Test existing integration for a user
     */
    public boolean testExistingIntegration(String userId) {
        log.info("Testing existing Webull integration for user: {}", userId);

        try {
            Map<String, String> credentials = getStoredCredentials(userId);
            if (credentials == null || credentials.isEmpty()) {
                log.warn("No credentials found for user: {}", userId);
                return false;
            }

            String appKey = credentials.get("appKey");
            String appSecret = credentials.get("appSecret");

            Boolean isConnected = webullApiAuthClient.testConnection(appKey, appSecret).block();
            return isConnected != null && isConnected;

        } catch (Exception e) {
            log.error("Error testing existing Webull integration for user: {}", userId, e);
            return false;
        }
    }

    /**
     * Disconnect Webull integration for a user
     */
    public boolean disconnectIntegration(String userId) {
        log.info("Disconnecting Webull integration for user: {}", userId);

        try {
            webullCredentialManager.deleteCredentials(userId);
            log.info("Disconnected Webull integration for user: {}", userId);
            return true;

        } catch (Exception e) {
            log.error("Error disconnecting Webull integration for user: {}", userId, e);
            return false;
        }
    }

    /**
     * Sync provider data for a user
     *
     * @param userId User ID
     * @return Synced ProviderDataEntity with latest portfolio data
     */
    public ProviderDataEntity syncProviderData(String userId) {
        log.info("Syncing Webull provider data for user: {}", userId);

        try {
            Map<String, String> credentials = getStoredCredentials(userId);
            if (credentials == null || credentials.isEmpty()) {
                throw new StrategizException(
                        WebullProviderErrorDetails.INVALID_CREDENTIALS,
                        MODULE_NAME,
                        userId,
                        "No credentials found in Vault"
                );
            }

            String appKey = credentials.get("appKey");
            String appSecret = credentials.get("appSecret");
            String accountId = credentials.get("accountId");

            if (accountId == null || accountId.isEmpty()) {
                throw new StrategizException(
                        WebullProviderErrorDetails.ACCOUNT_ID_REQUIRED,
                        MODULE_NAME,
                        userId,
                        "Account ID not found in stored credentials"
                );
            }

            ProviderDataEntity syncedData = dataInitializer.initializeAndStoreData(userId, appKey, appSecret, accountId);

            log.info("Successfully synced Webull data for user: {}, total value: {}, holdings: {}",
                    userId, syncedData.getTotalValue(),
                    syncedData.getHoldings() != null ? syncedData.getHoldings().size() : 0);

            return syncedData;

        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to sync Webull provider data for user: {}", userId, e);
            throw new StrategizException(
                    WebullProviderErrorDetails.DATA_INITIALIZATION_FAILED,
                    MODULE_NAME,
                    userId,
                    e.getMessage()
            );
        }
    }
}
