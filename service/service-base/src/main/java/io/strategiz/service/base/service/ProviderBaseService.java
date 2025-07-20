package io.strategiz.service.base.service;

import io.strategiz.service.base.BaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Base service for all provider-related services.
 * Extends BaseService and adds provider-specific common functionality.
 * 
 * Feature services like CoinbaseService, KrakenService, etc. should extend this.
 */
public abstract class ProviderBaseService extends BaseService {
    
    @Override
    protected String getModuleName() {
        return "service-provider";
    }
    
    protected final Logger providerLog = LoggerFactory.getLogger("PROVIDER." + getClass().getSimpleName());
    
    /**
     * Generate state parameter for OAuth flows
     */
    protected String generateState(String userId, String context) {
        String state = context + ":" + userId + ":" + System.currentTimeMillis();
        providerLog.debug("Generated state parameter for user: {}, provider: {}, context: {}", 
            userId, getProviderId(), context);
        return state;
    }
    
    /**
     * Validate state parameter for OAuth callbacks
     */
    protected boolean validateState(String state, String userId, String expectedContext) {
        if (state == null || !state.startsWith(expectedContext + ":")) {
            providerLog.warn("Invalid state parameter for provider {}: {}", getProviderId(), state);
            return false;
        }
        
        String[] parts = state.split(":");
        if (parts.length >= 2) {
            String stateUserId = parts[1];
            boolean valid = userId.equals(stateUserId);
            
            if (!valid) {
                providerLog.warn("State validation failed for provider {} - expected user: {}, got: {}", 
                    getProviderId(), userId, stateUserId);
            }
            
            return valid;
        }
        
        return false;
    }
    
    /**
     * Log provider connection attempt for audit purposes
     */
    protected void logProviderAttempt(String userId, String action, boolean success) {
        if (success) {
            providerLog.info("PROVIDER_SUCCESS - User: {}, Provider: {}, Action: {}", 
                userId, getProviderId(), action);
        } else {
            providerLog.warn("PROVIDER_FAILURE - User: {}, Provider: {}, Action: {}", 
                userId, getProviderId(), action);
        }
    }
    
    /**
     * Log OAuth flow steps for debugging
     */
    protected void logOAuthStep(String userId, String step, String details) {
        providerLog.info("OAUTH_STEP - User: {}, Provider: {}, Step: {}, Details: {}", 
            userId, getProviderId(), step, details);
    }
    
    /**
     * Log API calls for monitoring and debugging
     */
    protected void logApiCall(String userId, String endpoint, boolean success, long responseTimeMs) {
        if (success) {
            providerLog.info("API_CALL_SUCCESS - User: {}, Provider: {}, Endpoint: {}, Time: {}ms", 
                userId, getProviderId(), endpoint, responseTimeMs);
        } else {
            providerLog.warn("API_CALL_FAILURE - User: {}, Provider: {}, Endpoint: {}, Time: {}ms", 
                userId, getProviderId(), endpoint, responseTimeMs);
        }
    }
    
    /**
     * Create standardized success response for provider operations
     */
    protected Map<String, Object> createProviderSuccessResponse(Map<String, Object> data) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("providerId", getProviderId());
        response.put("providerName", getProviderName());
        response.put("data", data);
        response.put("timestamp", new Date());
        return response;
    }
    
    /**
     * Create standardized error response for provider operations
     */
    protected Map<String, Object> createProviderErrorResponse(String message, String errorCode) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", message);
        response.put("errorCode", errorCode);
        response.put("providerId", getProviderId());
        response.put("providerName", getProviderName());
        response.put("timestamp", new Date());
        return response;
    }
    
    /**
     * Create OAuth redirect response
     */
    protected Map<String, Object> createOAuthRedirectResponse(String authorizationUrl, String state) {
        Map<String, Object> data = new HashMap<>();
        data.put("authorizationUrl", authorizationUrl);
        data.put("state", state);
        data.put("flowType", "oauth");
        data.put("instructions", "Redirect user to " + getProviderName() + " for authentication");
        
        return createProviderSuccessResponse(data);
    }
    
    /**
     * Create API key flow response
     */
    protected Map<String, Object> createApiKeyResponse(String instructions, String[] requiredFields) {
        Map<String, Object> data = new HashMap<>();
        data.put("flowType", "api_key");
        data.put("instructions", instructions);
        data.put("requiredFields", requiredFields);
        data.put("provider", getProviderName());
        
        return createProviderSuccessResponse(data);
    }
    
    /**
     * Validate provider-specific configuration
     */
    protected void validateProviderConfiguration() {
        providerLog.debug("Validating configuration for provider: {}", getProviderId());
        // Override in subclasses for specific validation
        ensureRealApiData(getProviderId() + " API");
    }
    
    /**
     * Check if provider credentials are properly configured
     */
    protected boolean areCredentialsConfigured(String userId) {
        // Override in subclasses for specific credential checking
        providerLog.debug("Checking credentials configuration for user: {}, provider: {}", 
            userId, getProviderId());
        return false;
    }
    
    /**
     * Get rate limit information for the provider
     */
    protected Map<String, Object> getRateLimitInfo() {
        // Override in subclasses for provider-specific rate limits
        Map<String, Object> rateLimits = new HashMap<>();
        rateLimits.put("requestsPerMinute", 60);  // Default
        rateLimits.put("requestsPerHour", 1000);  // Default
        return rateLimits;
    }
    
    /**
     * Get the provider ID for this service
     * Each provider service must implement this
     */
    protected abstract String getProviderId();
    
    /**
     * Get the provider display name
     * Each provider service must implement this
     */
    protected abstract String getProviderName();
} 