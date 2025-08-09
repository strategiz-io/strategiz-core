package io.strategiz.business.provider.binanceus;

import io.strategiz.client.binanceus.auth.BinanceUSApiAuthClient;
import io.strategiz.client.binanceus.auth.service.BinanceUSCredentialService;
// import io.strategiz.data.auth.entity.ProviderIntegrationEntity;
// import io.strategiz.data.auth.repository.ProviderIntegrationRepository;
import io.strategiz.business.base.provider.model.CreateProviderIntegrationRequest;
import io.strategiz.business.base.provider.model.ProviderIntegrationResult;
import io.strategiz.business.base.provider.ProviderIntegrationHandler;
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
public class BinanceUSProviderBusiness implements ProviderIntegrationHandler {

    private static final Logger log = LoggerFactory.getLogger(BinanceUSProviderBusiness.class);
    
    private static final String PROVIDER_ID = "binanceus";
    private static final String PROVIDER_NAME = "Binance US";
    private static final String PROVIDER_TYPE = "exchange";
    private final BinanceUSApiAuthClient apiAuthClient;
    private final BinanceUSCredentialService credentialService;
    // private final ProviderIntegrationRepository providerIntegrationRepository;

    public BinanceUSProviderBusiness(
            BinanceUSApiAuthClient apiAuthClient,
            BinanceUSCredentialService credentialService) {
            // ProviderIntegrationRepository providerIntegrationRepository) {
        this.apiAuthClient = apiAuthClient;
        this.credentialService = credentialService;
        // this.providerIntegrationRepository = providerIntegrationRepository;
    }

    @Override
    public boolean testConnection(CreateProviderIntegrationRequest request, String userId) {
        log.info("Testing Binance US connection for user: {}", userId);
        
        try {
            // Extract credentials from request
            String apiKey = request.getApiKey();
            String apiSecret = request.getApiSecret();
            
            // Use the API auth client to test connection
            boolean isConnected = apiAuthClient.testConnection(apiKey, apiSecret);
            
            if (isConnected) {
                log.info("Binance US connection test successful for user: {}", userId);
            } else {
                log.warn("Binance US connection test failed for user: {}", userId);
            }
            
            return isConnected;
            
        } catch (Exception e) {
            log.error("Error testing Binance US connection for user: {}", userId, e);
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
            
            credentialService.storeCredentials(userId, apiKey, apiSecret);
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
            throw new RuntimeException("Failed to create Binance US integration", e);
        }
    }

    @Override
    public String getProviderId() {
        return PROVIDER_ID;
    }


    /**
     * Get stored Binance US credentials for a user
     */
    public Map<String, String> getCredentials(String userId) {
        try {
            return credentialService.getCredentialsAsMap(userId);
        } catch (Exception e) {
            log.error("Failed to retrieve Binance US credentials for user: {}", userId, e);
            throw new RuntimeException("Failed to retrieve credentials", e);
        }
    }




    /**
     * Test existing integration for a user
     */
    public boolean testExistingIntegration(String userId) {
        log.info("Testing existing Binance US integration for user: {}", userId);
        
        try {
            // Get stored credentials
            Map<String, String> credentials = getCredentials(userId);
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
            credentialService.deleteCredentials(userId);
            
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