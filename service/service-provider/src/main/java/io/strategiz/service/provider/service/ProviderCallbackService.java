package io.strategiz.service.provider.service;

import io.strategiz.service.provider.model.response.ProviderCallbackResponse;
import io.strategiz.service.provider.exception.ServiceProviderErrorDetails;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.business.provider.coinbase.CoinbaseProviderBusiness;
import io.strategiz.business.provider.alpaca.AlpacaProviderBusiness;
import io.strategiz.business.provider.schwab.SchwabProviderBusiness;
import io.strategiz.business.provider.kraken.business.KrakenProviderBusiness;
import io.strategiz.business.portfolio.PortfolioSummaryManager;
import io.strategiz.service.profile.service.ProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for processing OAuth callbacks from provider integrations.
 * Handles OAuth code exchange and provider connection completion.
 * 
 * @author Strategiz Team
 * @version 1.0
 */
@Service
public class ProviderCallbackService {
    
    private static final Logger log = LoggerFactory.getLogger(ProviderCallbackService.class);
    
    private final CoinbaseProviderBusiness coinbaseProviderBusiness;
    private final AlpacaProviderBusiness alpacaProviderBusiness;
    private final SchwabProviderBusiness schwabProviderBusiness;
    private final KrakenProviderBusiness krakenProviderBusiness;
    private final PortfolioSummaryManager portfolioSummaryManager;

    @Value("${frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Autowired
    public ProviderCallbackService(CoinbaseProviderBusiness coinbaseProviderBusiness,
                                   AlpacaProviderBusiness alpacaProviderBusiness,
                                   SchwabProviderBusiness schwabProviderBusiness,
                                   KrakenProviderBusiness krakenProviderBusiness,
                                   PortfolioSummaryManager portfolioSummaryManager) {
        this.coinbaseProviderBusiness = coinbaseProviderBusiness;
        this.alpacaProviderBusiness = alpacaProviderBusiness;
        this.schwabProviderBusiness = schwabProviderBusiness;
        this.krakenProviderBusiness = krakenProviderBusiness;
        this.portfolioSummaryManager = portfolioSummaryManager;
    }
    
    /**
     * Process OAuth callback from a provider.
     * 
     * @param provider The provider name
     * @param code The authorization code
     * @param state The state parameter
     * @return ProviderCallbackResponse with connection status
     */
    public ProviderCallbackResponse processOAuthCallback(String provider, String code, String state) {
        log.info("Processing OAuth callback for provider: {}, state: {}", provider, state);
        
        // Validate provider
        if (!isValidProvider(provider)) {
            throw new StrategizException(ServiceProviderErrorDetails.INVALID_PROVIDER_TYPE, "service-provider", provider);
        }
        
        // Extract user ID from state parameter
        String userId = extractUserIdFromState(state);
        if (userId == null || userId.isEmpty()) {
            throw new StrategizException(ServiceProviderErrorDetails.INVALID_OAUTH_STATE, "service-provider", state);
        }
        
        try {
            ProviderCallbackResponse response = new ProviderCallbackResponse();
            response.setProviderId(provider);
            response.setProviderName(getProviderDisplayName(provider));
            
            // Process based on provider
            switch (provider.toLowerCase()) {
                case "coinbase":
                    processCoinbaseCallback(userId, code, state, response);
                    break;
                    
                case "alpaca":
                    processAlpacaCallback(userId, code, state, response);
                    break;
                    
                case "schwab":
                    processSchwabCallback(userId, code, state, response);
                    break;
                    
                case "kraken":
                    processKrakenCallback(userId, code, state, response);
                    break;
                    
                case "binance":
                    // TODO: Implement Binance OAuth callback processing
                    throw new StrategizException(ServiceProviderErrorDetails.PROVIDER_NOT_SUPPORTED, "service-provider", provider);
                    
                default:
                    throw new StrategizException(ServiceProviderErrorDetails.INVALID_PROVIDER_TYPE, "service-provider", provider);
            }
            
            // Set success redirect URL
            response.setRedirectUrl(getSuccessRedirectUrl(provider));
            response.setOperationSuccess(true);

            // Refresh portfolio summary now that new provider is connected
            portfolioSummaryManager.refreshPortfolioSummary(userId);

            log.info("Successfully processed OAuth callback for provider: {}, user: {}", provider, userId);
            return response;
            
        } catch (StrategizException e) {
            // Re-throw business exceptions
            throw e;
        } catch (Exception e) {
            log.error("Error processing OAuth callback for provider: {}, user: {}", provider, userId, e);
            throw new StrategizException(ServiceProviderErrorDetails.OAUTH_TOKEN_EXCHANGE_FAILED, "service-provider", 
                userId, provider, e.getMessage());
        }
    }
    
    /**
     * Process Coinbase OAuth callback.
     * 
     * @param userId The user ID
     * @param code The authorization code
     * @param state The state parameter
     * @param response The response to populate
     */
    private void processCoinbaseCallback(String userId, String code, String state, ProviderCallbackResponse response) {
        log.info("Processing Coinbase OAuth callback for user: {}", userId);
        
        try {
            // Complete OAuth flow using Coinbase business module
            coinbaseProviderBusiness.completeOAuthFlow(userId, code, state);
            
            response.setStatus("connected");
            response.setMessage("Successfully connected Coinbase account");
            response.setConnectedAt(Instant.now());
            
            // Add connection metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("provider", "coinbase");
            metadata.put("connectionType", "oauth");
            metadata.put("userId", userId);
            response.setConnectionData(metadata);
            
        } catch (Exception e) {
            log.error("Failed to complete Coinbase OAuth flow for user: {}", userId, e);
            throw new StrategizException(ServiceProviderErrorDetails.OAUTH_TOKEN_EXCHANGE_FAILED, "service-provider", 
                userId, "coinbase", e.getMessage());
        }
    }
    
    /**
     * Process Alpaca OAuth callback.
     * 
     * @param userId The user ID
     * @param code The authorization code
     * @param state The state parameter
     * @param response The response to populate
     */
    private void processAlpacaCallback(String userId, String code, String state, ProviderCallbackResponse response) {
        log.info("Processing Alpaca OAuth callback for user: {}", userId);
        
        try {
            // Complete OAuth flow using Alpaca business module
            alpacaProviderBusiness.completeOAuthFlow(userId, code, state);
            
            response.setStatus("connected");
            response.setMessage("Successfully connected Alpaca account");
            response.setConnectedAt(Instant.now());
            
            // Add connection metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("provider", "alpaca");
            metadata.put("connectionType", "oauth");
            metadata.put("userId", userId);
            response.setConnectionData(metadata);
            
        } catch (Exception e) {
            log.error("Failed to complete Alpaca OAuth flow for user: {}", userId, e);
            throw new StrategizException(ServiceProviderErrorDetails.OAUTH_TOKEN_EXCHANGE_FAILED, "service-provider", 
                userId, "alpaca", e.getMessage());
        }
    }
    
    /**
     * Process Charles Schwab OAuth callback.
     * 
     * @param userId The user ID
     * @param code The authorization code
     * @param state The state parameter
     * @param response The response to populate
     */
    private void processSchwabCallback(String userId, String code, String state, ProviderCallbackResponse response) {
        log.info("Processing Charles Schwab OAuth callback for user: {}", userId);
        
        try {
            // Complete OAuth flow using Schwab business module
            schwabProviderBusiness.completeOAuthFlow(userId, code, state);
            
            response.setStatus("connected");
            response.setMessage("Successfully connected Charles Schwab account");
            response.setConnectedAt(Instant.now());
            
            // Add connection metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("provider", "schwab");
            metadata.put("connectionType", "oauth");
            metadata.put("userId", userId);
            response.setConnectionData(metadata);
            
        } catch (Exception e) {
            log.error("Failed to complete Charles Schwab OAuth flow for user: {}", userId, e);
            throw new StrategizException(ServiceProviderErrorDetails.OAUTH_TOKEN_EXCHANGE_FAILED, "service-provider", 
                userId, "schwab", e.getMessage());
        }
    }

    /**
     * Process Kraken OAuth callback.
     * Since Kraken doesn't currently support OAuth, this is a simplified implementation
     * that marks the connection as successful.
     *
     * @param userId The user ID
     * @param code The authorization code (not used for Kraken currently)
     * @param state The state parameter
     * @param response The response to populate
     */
    private void processKrakenCallback(String userId, String code, String state, ProviderCallbackResponse response) {
        log.info("Processing Kraken OAuth callback for user: {}", userId);

        try {
            // For now, simply mark as connected since Kraken doesn't have full OAuth support
            // In a real implementation, this would exchange the code for tokens and store them

            response.setStatus("connected");
            response.setMessage("Successfully connected Kraken account");
            response.setConnectedAt(Instant.now());

            // Add connection metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("provider", "kraken");
            metadata.put("connectionType", "oauth");
            metadata.put("userId", userId);
            metadata.put("note", "Kraken OAuth integration simplified - full OAuth flow to be implemented");
            response.setConnectionData(metadata);

            log.info("Successfully processed Kraken OAuth callback for user: {}", userId);

        } catch (Exception e) {
            log.error("Failed to complete Kraken OAuth flow for user: {}", userId, e);
            throw new StrategizException(ServiceProviderErrorDetails.OAUTH_TOKEN_EXCHANGE_FAILED, "service-provider",
                userId, "kraken", e.getMessage());
        }
    }

    /**
     * Get success redirect URL for frontend.
     * 
     * @param provider The provider name
     * @return The redirect URL
     */
    public String getSuccessRedirectUrl(String provider) {
        return String.format("%s/providers/callback/%s/success", frontendUrl, provider);
    }
    
    /**
     * Get error redirect URL for frontend.
     * 
     * @param provider The provider name
     * @param error The error code
     * @param errorDescription The error description
     * @return The redirect URL
     */
    public String getErrorRedirectUrl(String provider, String error, String errorDescription) {
        return String.format("%s/providers/callback/%s/error?error=%s&description=%s", 
            frontendUrl, provider, error, errorDescription != null ? errorDescription : "");
    }
    
    /**
     * Extract user ID from state parameter.
     * Supports multiple formats:
     * 1. userId-sessionUUID (generated by backend) - e.g. "65370699-cc00-4bb2-88bd-ae005b80c9d8-08665d89-c9b6-419a-9ac9-090a85b8a518"
     * 2. coinbase_userId_timestamp (manual script format)
     *
     * @param state The state parameter
     * @return The user ID or null if invalid
     */
    private String extractUserIdFromState(String state) {
        if (state == null || state.isEmpty()) {
            return null;
        }

        // Handle format: coinbase_userId_timestamp
        if (state.contains("_")) {
            String[] parts = state.split("_");
            if (parts.length >= 3) {
                // Return the middle part which is the user ID
                return parts[1];
            }
        }

        // Handle format: userId-sessionUUID (standard backend format)
        // State format: "userId-sessionUUID" where userId is a full UUID (5 parts) and sessionUUID is also a UUID (5 parts)
        // Example: "65370699-cc00-4bb2-88bd-ae005b80c9d8-08665d89-c9b6-419a-9ac9-090a85b8a518"
        if (state.contains("-")) {
            String[] parts = state.split("-");
            // A UUID has 5 parts separated by dashes, so if we have 10+ parts, it's userId-sessionUUID
            if (parts.length >= 10) {
                // Extract first 5 parts (the user UUID)
                return String.join("-", parts[0], parts[1], parts[2], parts[3], parts[4]);
            } else if (parts.length >= 2) {
                // Fallback: take first part for backward compatibility
                return parts[0];
            }
        }

        // If no delimiter found, assume the entire string is the user ID
        return state;
    }
    
    /**
     * Check if provider is valid.
     * 
     * @param provider The provider name
     * @return true if valid, false otherwise
     */
    private boolean isValidProvider(String provider) {
        if (provider == null || provider.isEmpty()) {
            return false;
        }
        
        String p = provider.toLowerCase();
        return "coinbase".equals(p) || "kraken".equals(p) || "binance".equals(p) || "alpaca".equals(p) || "schwab".equals(p);
    }
    
    /**
     * Get provider display name.
     * 
     * @param provider The provider ID
     * @return The display name
     */
    private String getProviderDisplayName(String provider) {
        if (provider == null) {
            return "Unknown";
        }
        
        switch (provider.toLowerCase()) {
            case "coinbase":
                return "Coinbase";
            case "kraken":
                return "Kraken";
            case "binance":
                return "Binance";
            case "alpaca":
                return "Alpaca";
            case "schwab":
                return "Charles Schwab";
            default:
                return provider;
        }
    }
}