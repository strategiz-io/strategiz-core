package io.strategiz.service.provider.api;

import java.util.Map;

/**
 * Interface defining operations that all provider API services must implement.
 */
public interface ProviderApiService {

    /**
     * Get the provider ID (e.g., "kraken", "binanceus").
     *
     * @return The provider ID
     */
    String getProviderId();
    
    /**
     * Generate the OAuth authorization URL.
     *
     * @param userId The user ID
     * @param accountType The account type (paper or real)
     * @param state A state parameter for security
     * @return The authorization URL to redirect the user to
     */
    String generateAuthorizationUrl(String userId, String accountType, String state);
    
    /**
     * Handle the OAuth callback and exchange the code for tokens.
     *
     * @param userId The user ID
     * @param code The authorization code
     * @param accountType The account type (paper or real)
     * @return True if successful
     */
    boolean handleOAuthCallback(String userId, String code, String accountType);
    
    /**
     * Refresh the OAuth tokens.
     *
     * @param userId The user ID
     * @return True if successful
     */
    boolean refreshTokens(String userId);
    
    /**
     * Get account balance from the provider.
     *
     * @param userId The user ID
     * @return Map containing the account balance data
     */
    Map<String, Object> getAccountBalance(String userId);
    
    /**
     * Get open orders from the provider.
     *
     * @param userId The user ID
     * @return Map containing the open orders data
     */
    Map<String, Object> getOpenOrders(String userId);
    
    /**
     * Place a new order on the provider.
     *
     * @param userId The user ID
     * @param orderParams Map containing order parameters
     * @return Map containing the order result
     */
    Map<String, Object> placeOrder(String userId, Map<String, String> orderParams);
    
    /**
     * Disconnect the provider by removing its configuration and credentials.
     *
     * @param userId The user ID
     * @return True if successful
     */
    boolean disconnectProvider(String userId);
    
    /**
     * Check if a user has connected this provider.
     *
     * @param userId The user ID
     * @return True if connected, false otherwise
     */
    boolean isProviderConnected(String userId);
    
    /**
     * Get provider metadata for a user, such as account type and connection status.
     *
     * @param userId The user ID
     * @return Map containing provider metadata
     */
    Map<String, Object> getProviderMetadata(String userId);
}
