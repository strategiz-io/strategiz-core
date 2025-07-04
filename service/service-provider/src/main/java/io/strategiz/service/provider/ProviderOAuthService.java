package io.strategiz.service.provider;

import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.provider.api.ProviderApiFactory;
import io.strategiz.service.provider.api.ProviderApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Service for handling OAuth flows with various exchanges and brokerages.
 * Acts as a facade for the specific provider implementations.
 */
@Service
public class ProviderOAuthService {

    private static final Logger log = LoggerFactory.getLogger(ProviderOAuthService.class);

    private final UserRepository userRepository;
    private final ProviderApiFactory providerApiFactory;
    
    // Redirect URI for OAuth callbacks
    @Value("${provider.oauth.redirect-uri}")
    private String redirectUri;

    @Autowired
    public ProviderOAuthService(
            UserRepository userRepository,
            ProviderApiFactory providerApiFactory) {
        this.userRepository = userRepository;
        this.providerApiFactory = providerApiFactory;
    }

    /**
     * Generate the OAuth authorization URL for a specific provider.
     *
     * @param providerId The provider ID (kraken, binanceus)
     * @param userId The user ID
     * @param accountType The account type (paper or real)
     * @param state A state parameter for security
     * @return The authorization URL to redirect the user to
     */
    public String generateAuthorizationUrl(String providerId, String userId, String accountType, String state) {
        ProviderApiService providerApi = providerApiFactory.getProviderApi(providerId);
        return providerApi.generateAuthorizationUrl(userId, accountType, state);
    }

    /**
     * Handle the OAuth callback and exchange the code for tokens.
     *
     * @param providerId The provider ID
     * @param userId The user ID
     * @param code The authorization code
     * @param accountType The account type (paper or real)
     * @return True if successful
     */
    public boolean handleOAuthCallback(String providerId, String userId, String code, String accountType) {
        log.info("Handling OAuth callback for provider: {}", providerId);
        ProviderApiService providerApi = providerApiFactory.getProviderApi(providerId);
        return providerApi.handleOAuthCallback(userId, code, accountType);
    }
    
    /**
     * Refresh the OAuth tokens for a provider.
     *
     * @param userId The user ID
     * @param providerId The provider ID
     * @return True if successful
     */
    public boolean refreshTokens(String userId, String providerId) {
        log.info("Refreshing tokens for provider: {}", providerId);
        ProviderApiService providerApi = providerApiFactory.getProviderApi(providerId);
        return providerApi.refreshTokens(userId);
    }
    
    /**
     * Initiate OAuth flow for a provider (method expected by controller).
     *
     * @param providerId The provider ID
     * @param userId The user ID
     * @return Map containing OAuth initialization data
     */
    public Map<String, String> initiateOAuth(String providerId, String userId) {
        String accountType = "paper"; // Default to paper trading
        String state = "state-" + System.currentTimeMillis(); // Generate a simple state
        
        String authUrl = generateAuthorizationUrl(providerId, userId, accountType, state);
        
        Map<String, String> response = new HashMap<>();
        response.put("authorizationUrl", authUrl);
        response.put("providerId", providerId);
        response.put("state", state);
        
        return response;
    }

    /**
     * Disconnect a provider by removing its configuration and credentials (modified to return Map).
     *
     * @param providerId The provider ID
     * @param userId The user ID
     * @return Map containing disconnection result
     */
    public Map<String, String> disconnectProvider(String providerId, String userId) {
        log.info("Disconnecting provider: {} for user: {}", providerId, userId);
        ProviderApiService providerApi = providerApiFactory.getProviderApi(providerId);
        boolean success = providerApi.disconnectProvider(userId);
        
        Map<String, String> response = new HashMap<>();
        response.put("providerId", providerId);
        response.put("status", success ? "disconnected" : "failed");
        response.put("message", success ? "Provider successfully disconnected" : "Failed to disconnect provider");
        
        return response;
    }
    
    /**
     * Get account balance from a provider.
     *
     * @param userId The user ID
     * @param providerId The provider ID
     * @return Map containing the account balance data
     */
    public Map<String, Object> getAccountBalance(String userId, String providerId) {
        log.info("Getting account balance for provider: {}", providerId);
        ProviderApiService providerApi = providerApiFactory.getProviderApi(providerId);
        return providerApi.getAccountBalance(userId);
    }
    
    /**
     * Get open orders from a provider.
     *
     * @param userId The user ID
     * @param providerId The provider ID
     * @return Map containing the open orders data
     */
    public Map<String, Object> getOpenOrders(String userId, String providerId) {
        log.info("Getting open orders for provider: {}", providerId);
        ProviderApiService providerApi = providerApiFactory.getProviderApi(providerId);
        return providerApi.getOpenOrders(userId);
    }
    
    /**
     * Place a new order on a provider.
     *
     * @param userId The user ID
     * @param providerId The provider ID
     * @param orderParams Map containing order parameters
     * @return Map containing the order result
     */
    public Map<String, Object> placeOrder(String userId, String providerId, Map<String, String> orderParams) {
        log.info("Placing order for provider: {}", providerId);
        ProviderApiService providerApi = providerApiFactory.getProviderApi(providerId);
        return providerApi.placeOrder(userId, orderParams);
    }
    
    /**
     * Get provider status for all supported providers.
     * 
     * @param userId The user ID
     * @return Map containing status for each provider
     */
    public Map<String, Object> getProviderStatus(String userId) {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> providers = new HashMap<>();
        
        Set<String> supportedProviders = providerApiFactory.getSupportedProviders();
        
        for (String providerId : supportedProviders) {
            try {
                ProviderApiService providerApi = providerApiFactory.getProviderApi(providerId);
                Map<String, Object> metadata = providerApi.getProviderMetadata(userId);
                providers.put(providerId, metadata);
            } catch (Exception e) {
                log.warn("Error getting provider status for {}: {}", providerId, e.getMessage());
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("connected", false);
                metadata.put("accountType", "paper");
                metadata.put("error", e.getMessage());
                providers.put(providerId, metadata);
            }
        }
        
        response.put("providers", providers);
        response.put("accountMode", "paper"); // Default, should come from user preferences
        
        return response;
    }
}
