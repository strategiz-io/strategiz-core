package io.strategiz.business.provider.binanceus;

import io.strategiz.business.provider.binanceus.exception.BinanceUSProviderErrorDetails;
import io.strategiz.client.binanceus.auth.BinanceUSApiAuthClient;
import io.strategiz.client.binanceus.auth.manager.BinanceUSCredentialManager;
// import io.strategiz.data.auth.entity.ProviderIntegrationEntity;
// import io.strategiz.data.auth.repository.ProviderIntegrationRepository;
import io.strategiz.business.base.provider.model.CreateProviderIntegrationRequest;
import io.strategiz.business.base.provider.model.ProviderIntegrationResult;
import io.strategiz.business.provider.base.BaseApiKeyProviderHandler;
import io.strategiz.framework.exception.StrategizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;

/**
 * Business logic for Binance US provider integration
 * Implements dual storage: credentials in Vault, metadata in Firestore
 */
@Service
public class BinanceUSProviderBusiness extends BaseApiKeyProviderHandler {

    private static final Logger log = LoggerFactory.getLogger(BinanceUSProviderBusiness.class);
    
    private static final String PROVIDER_ID = "binanceus";
    private static final String PROVIDER_NAME = "Binance US";
    private static final String PROVIDER_TYPE = "exchange";
    private final BinanceUSApiAuthClient apiAuthClient;
    private final BinanceUSCredentialManager credentialManager;
    // private final ProviderIntegrationRepository providerIntegrationRepository;

    public BinanceUSProviderBusiness(
            BinanceUSApiAuthClient apiAuthClient,
            BinanceUSCredentialManager credentialManager) {
            // ProviderIntegrationRepository providerIntegrationRepository) {
        this.apiAuthClient = apiAuthClient;
        this.credentialManager = credentialManager;
        // this.providerIntegrationRepository = providerIntegrationRepository;
    }

    @Override
    protected Map<String, String> getStoredCredentials(String userId) {
        try {
            return credentialManager.getCredentialsAsMap(userId);
        } catch (Exception e) {
            log.error("Error retrieving Binance US credentials for user: {}", userId, e);
            return null;
        }
    }
    
    @Override
    protected boolean testConnectionWithStoredCredentials(Map<String, String> storedCredentials, String userId) {
        String apiKey = storedCredentials.get("apiKey");
        String apiSecret = storedCredentials.get("apiSecret");
        
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
            // Use the API auth client to test connection
            boolean isConnected = apiAuthClient.testConnection(apiKey, apiSecret);
            
            if (isConnected) {
                log.info("Binance US connection test successful for user: {}", userId);
            } else {
                log.warn("Binance US connection test failed for user: {}", userId);
            }
            
            return isConnected;
            
        } catch (Exception e) {
            log.error("Error during Binance US API call for user: {} - Error: {}", userId, e.getMessage());
            return false;
        }
    }

    @Override
    public ProviderIntegrationResult createIntegration(CreateProviderIntegrationRequest request, String userId) {
        log.info("Creating Binance US integration for user: {}", userId);
        
        try {
            // 1. Store credentials in Vault using credential service
            String apiKey = request.getApiKey();
            String apiSecret = request.getApiSecret();
            
            credentialManager.storeCredentials(userId, apiKey, apiSecret);
            log.info("Stored Binance US credentials in Vault for user: {}", userId);
            
            // 2. Create provider integration entity for Firestore
            // ProviderIntegrationEntity entity = new ProviderIntegrationEntity(
            //     PROVIDER_ID, PROVIDER_NAME, PROVIDER_TYPE);
            // 
            // entity.setStatus("connected");
            // entity.setEnabled(true);
            // entity.setSupportsTrading(true);
            // entity.setPermissions(Arrays.asList("read", "trade"));
            // entity.setConnectedAt(Instant.now());
            // entity.setLastTestedAt(Instant.now());
            // 
            // // Add metadata
            // entity.putMetadata("connectionMethod", "api_key");
            // entity.putMetadata("apiVersion", "v3");
            // entity.putMetadata("region", "us");
            // 
            // // 3. Save to Firestore user subcollection
            // ProviderIntegrationEntity savedEntity = providerIntegrationRepository.saveForUser(userId, entity);
            // log.info("Saved Binance US provider integration to Firestore for user: {}", userId);
            
            // 4. Build result
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("connectionMethod", "api_key");
            metadata.put("apiVersion", "v3");
            metadata.put("region", "us");
            
            ProviderIntegrationResult result = new ProviderIntegrationResult();
            result.setSuccess(true);
            result.setMessage("Binance US integration created successfully");
            result.setMetadata(metadata);
            return result;
                
        } catch (Exception e) {
            log.error("Error creating Binance US integration for user: {}", userId, e);
            throw new StrategizException(BinanceUSProviderErrorDetails.INTEGRATION_CREATION_FAILED, "business-provider-binanceus", e, userId);
        }
    }

    @Override
    public String getProviderId() {
        return PROVIDER_ID;
    }






    /**
     * Test existing integration for a user
     */
    public boolean testExistingIntegration(String userId) {
        log.info("Testing existing Binance US integration for user: {}", userId);
        
        try {
            // Get stored credentials
            Map<String, String> credentials = getStoredCredentials(userId);
            if (credentials == null || credentials.isEmpty()) {
                log.warn("No credentials found for user: {}", userId);
                return false;
            }
            String apiKey = credentials.get("apiKey");
            String apiSecret = credentials.get("apiSecret");
            
            // Test connection using API auth client
            boolean isConnected = apiAuthClient.testConnection(apiKey, apiSecret);
                                  
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
            log.error("Error testing existing Binance US integration for user: {}", userId, e);
            return false;
        }
    }

    /**
     * Disconnect Binance US integration for a user
     */
    public boolean disconnectIntegration(String userId) {
        log.info("Disconnecting Binance US integration for user: {}", userId);
        
        try {
            // Remove credentials from Vault using credential service
            credentialManager.deleteCredentials(userId);
            
            // Remove from Firestore
            // boolean deleted = providerIntegrationRepository.deleteByUserIdAndProviderId(userId, PROVIDER_ID);
            boolean deleted = true; // Placeholder - repository access removed
            
            log.info("Disconnected Binance US integration for user: {}, deleted: {}", userId, deleted);
            return deleted;
            
        } catch (Exception e) {
            log.error("Error disconnecting Binance US integration for user: {}", userId, e);
            return false;
        }
    }
}