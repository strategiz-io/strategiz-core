package io.strategiz.business.provider.kraken.business;

import io.strategiz.business.provider.kraken.constants.KrakenConstants;
import io.strategiz.business.provider.kraken.exception.KrakenProviderErrorDetails;
import io.strategiz.business.provider.kraken.helper.KrakenDataInitializer;
import io.strategiz.business.provider.kraken.helper.KrakenDataTransformer;
import io.strategiz.client.kraken.auth.KrakenApiAuthClient;
import io.strategiz.client.kraken.auth.model.KrakenApiCredentials;
import io.strategiz.client.kraken.auth.manager.KrakenCredentialManager;
import io.strategiz.data.provider.entity.PortfolioProviderEntity;
import io.strategiz.data.provider.entity.ProviderHoldingsEntity;
import io.strategiz.data.provider.repository.PortfolioProviderRepository;
import io.strategiz.business.base.provider.model.CreateProviderIntegrationRequest;
import io.strategiz.business.base.provider.model.ProviderIntegrationResult;
import io.strategiz.business.provider.base.BaseApiKeyProviderHandler;
import io.strategiz.framework.exception.StrategizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Business logic for Kraken provider integration
 * Implements dual storage: credentials in Vault, metadata in Firestore
 */
@Service
public class KrakenProviderBusiness extends BaseApiKeyProviderHandler {

    private static final Logger log = LoggerFactory.getLogger(KrakenProviderBusiness.class);
    
    private static final String MODULE_NAME = "business-provider-kraken";

    private final KrakenApiAuthClient krakenApiAuthClient;
    private final KrakenCredentialManager krakenCredentialManager;
    private final KrakenDataInitializer dataInitializer;
    private final PortfolioProviderRepository portfolioProviderRepository;

    @Autowired
    public KrakenProviderBusiness(
            KrakenApiAuthClient krakenApiAuthClient,
            KrakenCredentialManager krakenCredentialManager,
            KrakenDataInitializer dataInitializer,
            PortfolioProviderRepository portfolioProviderRepository) {
        this.krakenApiAuthClient = krakenApiAuthClient;
        this.krakenCredentialManager = krakenCredentialManager;
        this.dataInitializer = dataInitializer;
        this.portfolioProviderRepository = portfolioProviderRepository;
    }

    @Override
    protected Map<String, String> getStoredCredentials(String userId) {
        try {
            return krakenCredentialManager.getCredentialsAsMap(userId);
        } catch (Exception e) {
            log.error("Error retrieving Kraken credentials for user: {}", userId, e);
            return null;
        }
    }
    
    @Override
    protected boolean testConnectionWithStoredCredentials(Map<String, String> storedCredentials, String userId) {
        String apiKey = storedCredentials.get("apiKey");
        String apiSecret = storedCredentials.get("apiSecret");
        String otp = storedCredentials.get("otp");
        
        // Validate credentials
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("API key is missing in stored credentials for user: {}", userId);
            return false;
        }
        
        if (apiSecret == null || apiSecret.trim().isEmpty()) {
            log.warn("API secret is missing in stored credentials for user: {}", userId);
            return false;
        }
        
        log.debug("Testing connection with stored credentials from Vault");
        
        try {
            // Test connection by fetching account balance
            Map<String, Object> response = krakenApiAuthClient.getAccountBalance(apiKey, apiSecret, otp).block();
            
            if (response == null) {
                log.warn("Null response from Kraken API for user: {}", userId);
                return false;
            }
            
            // Check for Kraken API errors
            if (response.containsKey("error")) {
                Object errors = response.get("error");
                if (errors instanceof java.util.List && !((java.util.List<?>) errors).isEmpty()) {
                    log.warn("Kraken API errors for user {}: {}", userId, errors);
                    return false;
                }
            }
            
            // Check for result data
            if (!response.containsKey("result")) {
                log.warn("No result in Kraken API response for user: {}", userId);
                return false;
            }
            
            log.info("Kraken connection test successful for user: {}", userId);
            return true;
            
        } catch (Exception e) {
            log.error("Error during Kraken API call for user: {} - Error: {}", userId, e.getMessage());
            return false;
        }
    }

    @Override
    public ProviderIntegrationResult createIntegration(CreateProviderIntegrationRequest request, String userId) {
        log.info("Creating Kraken integration for user: {}", userId);

        try {
            // 1. Store credentials in Vault
            String apiKey = request.getApiKey();
            String apiSecret = request.getApiSecret();
            String otp = null; // OTP not supported in simplified request

            krakenCredentialManager.storeCredentials(userId, apiKey, apiSecret, otp);
            log.info("Stored Kraken credentials in Vault for user: {}", userId);

            // 2. Create provider entity in new portfolio structure
            // Path: users/{userId}/portfolio/providers/{providerId}
            try {
                PortfolioProviderEntity entity = new PortfolioProviderEntity(
                    KrakenConstants.PROVIDER_ID, "api_key", userId);
                entity.setProviderName("Kraken");
                entity.setProviderType("crypto");
                entity.setProviderCategory("exchange");
                entity.setStatus("connected");

                portfolioProviderRepository.save(entity, userId);
                log.info("Saved Kraken provider to Firestore for user: {}", userId);
            } catch (Exception e) {
                log.error("Failed to save provider to Firestore, but continuing: {}", e.getMessage());
                // Continue even if Firestore save fails - credentials are in Vault
            }

            // 3. Initialize and store portfolio data
            ProviderHoldingsEntity portfolioData = null;
            try {
                portfolioData = dataInitializer.initializeAndStoreData(userId, apiKey, apiSecret, otp);
                log.info("Successfully initialized portfolio data for user: {}, total value: {}",
                        userId, portfolioData.getTotalValue());
            } catch (Exception e) {
                log.error("Failed to initialize portfolio data, but connection saved: {}", e.getMessage());
                // Don't fail the connection if initial data fetch fails
            }

            // 4. Build result
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("hasOtp", otp != null && !otp.trim().isEmpty());
            metadata.put("connectionMethod", "api_key");
            metadata.put("apiVersion", "v0");

            // Add portfolio data to metadata if available
            if (portfolioData != null) {
                metadata.put("initialSync", "success");
                metadata.put("totalValue", portfolioData.getTotalValue());
                metadata.put("holdingsCount", portfolioData.getHoldings() != null ?
                    portfolioData.getHoldings().size() : 0);
                metadata.put("cashBalance", portfolioData.getCashBalance());
            } else {
                metadata.put("initialSync", "failed");
            }

            ProviderIntegrationResult result = new ProviderIntegrationResult();
            result.setSuccess(true);
            result.setMessage("Kraken integration created successfully");
            result.setMetadata(metadata);
            return result;

        } catch (StrategizException e) {
            // Re-throw business exceptions
            throw e;
        } catch (Exception e) {
            log.error("Error creating Kraken integration for user: {}", userId, e);
            throw new StrategizException(
                KrakenProviderErrorDetails.CONNECTION_FAILED,
                MODULE_NAME,
                userId,
                e.getMessage()
            );
        }
    }

    @Override
    public String getProviderId() {
        return KrakenConstants.PROVIDER_ID;
    }


    /**
     * Test existing integration for a user
     */
    public boolean testExistingIntegration(String userId) {
        log.info("Testing existing Kraken integration for user: {}", userId);
        
        try {
            // Get stored credentials
            Map<String, String> credentials = getStoredCredentials(userId);
            if (credentials == null || credentials.isEmpty()) {
                log.warn("No credentials found for user: {}", userId);
                return false;
            }
            
            String apiKey = credentials.get("apiKey");
            String apiSecret = credentials.get("apiSecret");
            String otp = credentials.get("otp");
            
            // Test connection
            Map<String, Object> response = krakenApiAuthClient.getAccountBalance(apiKey, apiSecret, otp).block();
            
            boolean isConnected = response != null && 
                                  !response.containsKey("error") && 
                                  response.containsKey("result");
                                  
            // Update last tested timestamp
            // if (providerIntegrationRepository.existsByUserIdAndProviderId(userId, PROVIDER_ID)) {
            //     var entity = providerIntegrationRepository.findByUserIdAndProviderId(userId, PROVIDER_ID);
            //     if (entity.isPresent()) {
            //         entity.get().markAsTested();
            //         if (isConnected) {
            //             entity.get().setStatus("connected");
            //         } else {
            //             entity.get().markAsError("Connection test failed");
            //         }
            //         providerIntegrationRepository.saveForUser(userId, entity.get());
            //     }
            // }
            
            return isConnected;
            
        } catch (Exception e) {
            log.error("Error testing existing Kraken integration for user: {}", userId, e);
            return false;
        }
    }

    /**
     * Disconnect Kraken integration for a user
     */
    public boolean disconnectIntegration(String userId) {
        log.info("Disconnecting Kraken integration for user: {}", userId);

        try {
            // Remove credentials from Vault
            krakenCredentialManager.deleteCredentials(userId);

            // Remove from Firestore
            // boolean deleted = providerIntegrationRepository.deleteByUserIdAndProviderId(userId, PROVIDER_ID);
            boolean deleted = true; // Placeholder - repository access removed

            log.info("Disconnected Kraken integration for user: {}, deleted: {}", userId, deleted);
            return deleted;

        } catch (Exception e) {
            log.error("Error disconnecting Kraken integration for user: {}", userId, e);
            return false;
        }
    }

    /**
     * Sync provider data for a user - fetches latest portfolio data from Kraken
     * and stores it in Firestore with enriched information including cost basis.
     *
     * @param userId User ID
     * @return Synced ProviderHoldingsEntity with latest portfolio data
     */
    public ProviderHoldingsEntity syncProviderData(String userId) {
        log.info("Syncing Kraken provider data for user: {}", userId);

        try {
            // 1. Get credentials from Vault
            Map<String, String> credentials = getStoredCredentials(userId);
            if (credentials == null || credentials.isEmpty()) {
                throw new StrategizException(
                    KrakenProviderErrorDetails.INVALID_CREDENTIALS,
                    MODULE_NAME,
                    userId,
                    "No credentials found in Vault"
                );
            }

            String apiKey = credentials.get("apiKey");
            String apiSecret = credentials.get("apiSecret");
            String otp = credentials.get("otp");

            // 2. Fetch and store fresh portfolio data using the data initializer
            // This will fetch balances, trade history, prices, and calculate cost basis
            ProviderHoldingsEntity syncedData = dataInitializer.initializeAndStoreData(userId, apiKey, apiSecret, otp);

            log.info("Successfully synced Kraken data for user: {}, total value: {}, holdings: {}",
                    userId, syncedData.getTotalValue(),
                    syncedData.getHoldings() != null ? syncedData.getHoldings().size() : 0);

            return syncedData;

        } catch (StrategizException e) {
            // Re-throw business exceptions
            throw e;
        } catch (Exception e) {
            log.error("Failed to sync Kraken provider data for user: {}", userId, e);
            throw new StrategizException(
                KrakenProviderErrorDetails.DATA_INITIALIZATION_FAILED,
                MODULE_NAME,
                userId,
                e.getMessage()
            );
        }
    }
}