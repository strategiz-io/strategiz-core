package io.strategiz.client.kraken.auth.service;

import io.strategiz.client.kraken.auth.model.KrakenApiCredentials;
import io.strategiz.framework.secrets.SecretManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for managing Kraken API credentials in Vault
 */
@Service
public class KrakenCredentialService {
    
    private static final Logger log = LoggerFactory.getLogger(KrakenCredentialService.class);
    
    private static final String SECRET_PATH_PREFIX = "secret/strategiz/users/";
    private static final String PROVIDER_PATH = "/providers/kraken";
    
    private final SecretManager secretManager;
    
    public KrakenCredentialService(SecretManager secretManager) {
        this.secretManager = secretManager;
    }
    
    /**
     * Store Kraken API credentials in Vault
     * 
     * @param credentials The credentials to store
     * @return true if stored successfully
     */
    public boolean storeCredentials(KrakenApiCredentials credentials) {
        if (credentials == null || credentials.getUserId() == null) {
            log.error("Cannot store credentials: missing userId");
            return false;
        }
        
        try {
            String secretPath = buildSecretPath(credentials.getUserId());
            
            Map<String, Object> secretData = new HashMap<>();
            secretData.put("apiKey", credentials.getApiKey());
            secretData.put("apiSecret", credentials.getApiSecret());
            if (credentials.getOtp() != null) {
                secretData.put("otp", credentials.getOtp());
            }
            secretData.put("provider", "kraken");
            secretData.put("createdAt", Instant.now().toString());
            
            secretManager.createSecret(secretPath, secretData);
            
            log.info("Stored Kraken credentials for user: {}", credentials.getUserId());
            return true;
            
        } catch (Exception e) {
            log.error("Failed to store Kraken credentials for user: {}", credentials.getUserId(), e);
            return false;
        }
    }
    
    /**
     * Store credentials with separate parameters
     * 
     * @param userId User ID
     * @param apiKey API key
     * @param apiSecret API secret
     * @param otp Optional OTP
     * @return true if stored successfully
     */
    public boolean storeCredentials(String userId, String apiKey, String apiSecret, String otp) {
        KrakenApiCredentials credentials = new KrakenApiCredentials(apiKey, apiSecret, otp, userId);
        return storeCredentials(credentials);
    }
    
    /**
     * Retrieve Kraken API credentials from Vault
     * 
     * @param userId User ID
     * @return The credentials, or null if not found
     */
    public KrakenApiCredentials getCredentials(String userId) {
        try {
            String secretPath = buildSecretPath(userId);
            Map<String, Object> secretData = secretManager.readSecretAsMap(secretPath);
            
            if (secretData == null || secretData.isEmpty()) {
                log.debug("No Kraken credentials found for user: {}", userId);
                return null;
            }
            
            String apiKey = (String) secretData.get("apiKey");
            String apiSecret = (String) secretData.get("apiSecret");
            String otp = (String) secretData.get("otp");
            
            return new KrakenApiCredentials(apiKey, apiSecret, otp, userId);
            
        } catch (Exception e) {
            log.error("Failed to retrieve Kraken credentials for user: {}", userId, e);
            return null;
        }
    }
    
    /**
     * Get credentials as a map (for compatibility)
     * 
     * @param userId User ID
     * @return Map with apiKey, apiSecret, and otp
     */
    public Map<String, String> getCredentialsAsMap(String userId) {
        KrakenApiCredentials credentials = getCredentials(userId);
        if (credentials == null) {
            return null;
        }
        
        Map<String, String> result = new HashMap<>();
        result.put("apiKey", credentials.getApiKey());
        result.put("apiSecret", credentials.getApiSecret());
        if (credentials.getOtp() != null) {
            result.put("otp", credentials.getOtp());
        }
        return result;
    }
    
    /**
     * Delete Kraken API credentials from Vault
     * 
     * @param userId User ID
     * @return true if deleted successfully
     */
    public boolean deleteCredentials(String userId) {
        try {
            String secretPath = buildSecretPath(userId);
            secretManager.deleteSecret(secretPath);
            
            log.info("Deleted Kraken credentials for user: {}", userId);
            return true;
            
        } catch (Exception e) {
            log.error("Failed to delete Kraken credentials for user: {}", userId, e);
            return false;
        }
    }
    
    /**
     * Check if credentials exist for a user
     * 
     * @param userId User ID
     * @return true if credentials exist
     */
    public boolean hasCredentials(String userId) {
        try {
            String secretPath = buildSecretPath(userId);
            Map<String, Object> secretData = secretManager.readSecretAsMap(secretPath);
            return secretData != null && !secretData.isEmpty();
            
        } catch (Exception e) {
            log.debug("Error checking credentials for user: {}", userId, e);
            return false;
        }
    }
    
    /**
     * Update OTP for existing credentials
     * 
     * @param userId User ID
     * @param otp New OTP value
     * @return true if updated successfully
     */
    public boolean updateOtp(String userId, String otp) {
        try {
            KrakenApiCredentials existingCredentials = getCredentials(userId);
            if (existingCredentials == null) {
                log.error("Cannot update OTP: no credentials found for user: {}", userId);
                return false;
            }
            
            // Update the OTP and store credentials again
            existingCredentials.setOtp(otp);
            return storeCredentials(existingCredentials);
            
        } catch (Exception e) {
            log.error("Failed to update OTP for user: {}", userId, e);
            return false;
        }
    }
    
    /**
     * Build the Vault secret path for a user
     * 
     * @param userId User ID
     * @return The secret path
     */
    private String buildSecretPath(String userId) {
        return SECRET_PATH_PREFIX + userId + PROVIDER_PATH;
    }
}