package io.strategiz.service.exchange.kraken;

import io.strategiz.client.kraken.KrakenClient;
import io.strategiz.client.kraken.model.KrakenAccount;
import io.strategiz.data.exchange.ExchangeCredentialsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for interacting with the Kraken exchange API
 */
@Service
public class KrakenService {

    private static final Logger log = LoggerFactory.getLogger(KrakenService.class);

    private final KrakenClient krakenClient;
    private final ExchangeCredentialsRepository credentialsRepository;

    @Autowired
    public KrakenService(KrakenClient krakenClient, ExchangeCredentialsRepository credentialsRepository) {
        this.krakenClient = krakenClient;
        this.credentialsRepository = credentialsRepository;
        log.info("KrakenService initialized");
    }

    /**
     * Configure the Kraken API credentials
     * @param apiKey API key
     * @param secretKey Secret key
     * @return Configuration status
     */
    public Map<String, String> configure(String apiKey, String secretKey) {
        Map<String, String> response = new HashMap<>();
        if (apiKey == null || apiKey.isEmpty() || secretKey == null || secretKey.isEmpty()) {
            response.put("status", "error");
            response.put("message", "API Key and Secret Key are required");
            return response;
        }

        try {
            // Test connection with the provided credentials
            KrakenAccount account = krakenClient.getAccount(apiKey, secretKey);
            if (account != null && account.getError() != null && account.getError().length > 0) {
                response.put("status", "error");
                response.put("message", String.join(", ", account.getError()));
                return response;
            }
            response.put("status", "success");
            response.put("message", "Kraken API credentials configured successfully");
            return response;
        } catch (Exception e) {
            log.error("Error configuring Kraken API credentials", e);
            response.put("status", "error");
            response.put("message", e.getMessage());
            return response;
        }
    }

    /**
     * Test connection to Kraken API
     * 
     * @param apiKey API key
     * @param secretKey Secret key
     * @return true if successful, false otherwise
     */
    public boolean testConnection(String apiKey, String secretKey) {
        try {
            return krakenClient.testConnection(apiKey, secretKey);
        } catch (Exception e) {
            log.error("Error testing Kraken API connection", e);
            return false;
        }
    }

    /**
     * Get account information from Kraken API
     * 
     * @param apiKey API key
     * @param secretKey Secret key
     * @return Account information
     */
    public KrakenAccount getAccount(String apiKey, String secretKey) {
        log.info("Getting account information from Kraken API");
        return krakenClient.getAccount(apiKey, secretKey);
    }
    
    /**
     * Get account information from Kraken API for a specific user
     * 
     * @param userId User ID
     * @return Account information or error if credentials not found
     */
    public KrakenAccount getAccountForUser(String userId) {
        log.info("Getting Kraken account information for user: {}", userId);
        
        try {
            // Get credentials from repository
            Map<String, String> credentials = credentialsRepository.getExchangeCredentials(userId, "kraken");
            
            if (credentials == null || !credentials.containsKey("apiKey") || !credentials.containsKey("secretKey")) {
                log.warn("Kraken credentials not found for user: {}", userId);
                KrakenAccount errorAccount = new KrakenAccount();
                errorAccount.setError(new String[]{"Kraken API credentials not configured"});
                return errorAccount;
            }
            
            String apiKey = credentials.get("apiKey");
            String secretKey = credentials.get("secretKey");
            
            return getAccount(apiKey, secretKey);
        } catch (Exception e) {
            log.error("Error getting Kraken account for user: {}", userId, e);
            KrakenAccount errorAccount = new KrakenAccount();
            errorAccount.setError(new String[]{"Error retrieving Kraken account: " + e.getMessage()});
            return errorAccount;
        }
    }
    
    /**
     * Save Kraken API credentials for a user
     * 
     * @param userId User ID
     * @param apiKey API key
     * @param secretKey Secret key
     * @return Status of the operation
     */
    public Map<String, String> saveCredentials(String userId, String apiKey, String secretKey) {
        log.info("Saving Kraken API credentials for user: {}", userId);
        Map<String, String> response = new HashMap<>();
        
        try {
            // Test the credentials first
            KrakenAccount account = getAccount(apiKey, secretKey);
            if (account != null && account.getError() != null && account.getError().length > 0) {
                response.put("status", "error");
                response.put("message", String.join(", ", account.getError()));
                return response;
            }
            
            // Save the credentials
            credentialsRepository.saveExchangeCredentials(userId, "kraken", apiKey, secretKey);
            
            response.put("status", "success");
            response.put("message", "Kraken API credentials saved successfully");
            return response;
        } catch (Exception e) {
            log.error("Error saving Kraken API credentials", e);
            response.put("status", "error");
            response.put("message", e.getMessage());
            return response;
        }
    }
    
    /**
     * Check if the Kraken API is available without authentication
     * Used for health monitoring
     * 
     * @return true if the API is available, false otherwise
     */
    public boolean isApiAvailable() {
        try {
            log.debug("Checking Kraken API availability");
            // Use the client to make a simple request to check API availability
            // For Kraken, we can use the public Time endpoint
            boolean available = krakenClient.checkPublicApiAvailability();
            log.debug("Kraken API available: {}", available);
            return available;
        } catch (Exception e) {
            log.debug("Kraken API unavailable: {}", e.getMessage());
            return false;
        }
    }
}
