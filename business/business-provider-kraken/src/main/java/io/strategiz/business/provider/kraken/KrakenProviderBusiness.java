package io.strategiz.business.provider.kraken;

import io.strategiz.client.kraken.auth.KrakenApiAuthClient;
import io.strategiz.client.kraken.auth.model.KrakenApiCredentials;
import io.strategiz.client.kraken.auth.service.KrakenCredentialService;
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
import java.util.HashMap;
import java.util.Map;

/**
 * Business logic for Kraken provider integration
 * Implements dual storage: credentials in Vault, metadata in Firestore
 */
@Service
public class KrakenProviderBusiness implements ProviderIntegrationHandler {

    private static final Logger log = LoggerFactory.getLogger(KrakenProviderBusiness.class);
    
    private static final String PROVIDER_ID = "kraken";
    private static final String PROVIDER_NAME = "Kraken";
    private static final String PROVIDER_TYPE = "exchange";

    private final KrakenApiAuthClient krakenApiAuthClient;
    private final KrakenCredentialService krakenCredentialService;
    // private final ProviderIntegrationRepository providerIntegrationRepository;

    public KrakenProviderBusiness(
            KrakenApiAuthClient krakenApiAuthClient,
            KrakenCredentialService krakenCredentialService) {
            // ProviderIntegrationRepository providerIntegrationRepository) {
        this.krakenApiAuthClient = krakenApiAuthClient;
        this.krakenCredentialService = krakenCredentialService;
        // this.providerIntegrationRepository = providerIntegrationRepository;
    }

    @Override
    public boolean testConnection(CreateProviderIntegrationRequest request, String userId) {
        log.info("Testing Kraken connection for user: {}", userId);
        
        try {
            // Extract credentials from request
            String apiKey = request.getApiKey();
            String apiSecret = request.getApiSecret();
            String otp = null; // OTP not supported in simplified request
            
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
            log.error("Error testing Kraken connection for user: {}", userId, e);
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
            
            krakenCredentialService.storeCredentials(userId, apiKey, apiSecret, otp);
            log.info("Stored Kraken credentials in Vault for user: {}", userId);
            
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
            // entity.putMetadata("hasOtp", otp != null && !otp.trim().isEmpty());
            // entity.putMetadata("connectionMethod", "api_key");
            // entity.putMetadata("apiVersion", "v0");
            // 
            // // 3. Save to Firestore user subcollection
            // ProviderIntegrationEntity savedEntity = providerIntegrationRepository.saveForUser(userId, entity);
            // log.info("Saved Kraken provider integration to Firestore for user: {}", userId);
            
            // 4. Build result
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("hasOtp", otp != null && !otp.trim().isEmpty());
            metadata.put("connectionMethod", "api_key");
            metadata.put("apiVersion", "v0");
            
            ProviderIntegrationResult result = new ProviderIntegrationResult();
            result.setSuccess(true);
            result.setMessage("Kraken integration created successfully");
            result.setMetadata(metadata);
            return result;
                
        } catch (Exception e) {
            log.error("Error creating Kraken integration for user: {}", userId, e);
            throw new RuntimeException("Failed to create Kraken integration", e);
        }
    }

    @Override
    public String getProviderId() {
        return PROVIDER_ID;
    }

    /**
     * Get stored Kraken credentials for a user
     */
    public Map<String, String> getCredentials(String userId) {
        try {
            return krakenCredentialService.getCredentialsAsMap(userId);
        } catch (Exception e) {
            log.error("Error retrieving Kraken credentials for user: {}", userId, e);
            throw new RuntimeException("Failed to retrieve Kraken credentials", e);
        }
    }

    /**
     * Test existing integration for a user
     */
    public boolean testExistingIntegration(String userId) {
        log.info("Testing existing Kraken integration for user: {}", userId);
        
        try {
            // Get stored credentials
            Map<String, String> credentials = getCredentials(userId);
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
            krakenCredentialService.deleteCredentials(userId);
            
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
}