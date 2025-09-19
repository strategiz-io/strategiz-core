package io.strategiz.business.provider.base;

import io.strategiz.business.base.provider.model.CreateProviderIntegrationRequest;
import io.strategiz.business.base.provider.ProviderIntegrationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Base implementation for API key-based provider integrations.
 * This class provides common functionality for providers that use API keys
 * (like Kraken, Binance US) as opposed to OAuth providers.
 * 
 * The key pattern is:
 * 1. Store credentials in Vault first (createIntegration)
 * 2. Test connection using stored credentials from Vault (testConnection)
 * 
 * This ensures proper separation of concerns between storing and testing.
 * 
 * @author Strategiz Team
 * @version 1.0
 */
public abstract class BaseApiKeyProviderHandler implements ProviderIntegrationHandler {
    
    private static final Logger log = LoggerFactory.getLogger(BaseApiKeyProviderHandler.class);
    
    /**
     * Test connection using credentials stored in Vault.
     * This method should always retrieve credentials from Vault to ensure
     * it's testing with the same credentials that were stored.
     * 
     * @param request The integration request (credentials ignored, uses Vault)
     * @param userId The user ID
     * @return true if connection successful, false otherwise
     */
    @Override
    public boolean testConnection(CreateProviderIntegrationRequest request, String userId) {
        log.info("Testing {} connection for user: {}", getProviderId(), userId);
        
        try {
            // Always retrieve credentials from Vault for API key providers
            // This ensures the test uses the same credentials that were stored
            Map<String, String> storedCredentials = getStoredCredentials(userId);
            
            if (storedCredentials == null || storedCredentials.isEmpty()) {
                log.warn("No stored credentials found in Vault for user: {}", userId);
                return false;
            }
            
            // Validate and test with stored credentials
            return testConnectionWithStoredCredentials(storedCredentials, userId);
            
        } catch (Exception e) {
            log.error("Error testing {} connection for user: {} - Error: {}", 
                     getProviderId(), userId, e.getMessage());
            return false;
        }
    }
    
    /**
     * Get stored credentials from Vault for a user.
     * Must be implemented by concrete providers.
     * 
     * @param userId The user ID
     * @return Map of stored credentials
     */
    protected abstract Map<String, String> getStoredCredentials(String userId);
    
    /**
     * Test connection using credentials retrieved from Vault.
     * Must be implemented by concrete providers.
     * 
     * @param storedCredentials Credentials retrieved from Vault
     * @param userId The user ID
     * @return true if connection successful, false otherwise
     */
    protected abstract boolean testConnectionWithStoredCredentials(
            Map<String, String> storedCredentials, 
            String userId);
}