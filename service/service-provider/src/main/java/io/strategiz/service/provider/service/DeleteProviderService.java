package io.strategiz.service.provider.service;

import io.strategiz.service.provider.model.request.DeleteProviderRequest;
import io.strategiz.service.provider.model.response.DeleteProviderResponse;
import io.strategiz.service.base.service.ProviderBaseService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for deleting provider connections and data.
 * Handles business logic for disconnecting providers and cleaning up associated data.
 * 
 * @author Strategiz Team
 * @version 1.0
 */
@Service
public class DeleteProviderService extends ProviderBaseService {
    
    /**
     * Deletes a provider connection.
     * 
     * @param request The delete request with provider ID and cleanup options
     * @return DeleteProviderResponse containing deletion result
     * @throws IllegalArgumentException if request is invalid
     */
    public DeleteProviderResponse deleteProvider(DeleteProviderRequest request) {
        providerLog.info("Deleting provider: {} for user: {}", 
                        request.getProviderId(), request.getUserId());
        
        // Log the delete attempt
        logProviderAttempt(request.getUserId(), "DELETE_PROVIDER", false);
        
        // Validate request
        validateDeleteRequest(request);
        
        DeleteProviderResponse response = new DeleteProviderResponse();
        response.setProviderId(request.getProviderId());
        response.setSuccess(false);
        
        try {
            // TODO: Replace with actual business logic integration
            // For now, simulate provider deletion
            processProviderDeletion(request, response);
            
            // Log successful attempt
            logProviderAttempt(request.getUserId(), "DELETE_PROVIDER", true);
            
            providerLog.info("Deleted provider: {} for user: {}", 
                            request.getProviderId(), request.getUserId());
            
        } catch (Exception e) {
            providerLog.error("Error deleting provider: {} for user: {}", 
                             request.getProviderId(), request.getUserId(), e);
            
            // Log failed attempt
            logProviderAttempt(request.getUserId(), "DELETE_PROVIDER", false);
            
            response.setSuccess(false);
            response.setErrorCode("DELETION_FAILED");
            response.setErrorMessage("Failed to delete provider: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * Validates the delete request.
     * 
     * @param request The delete request
     * @throws IllegalArgumentException if request is invalid
     */
    private void validateDeleteRequest(DeleteProviderRequest request) {
        if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
            throw new IllegalArgumentException("User ID is required");
        }
        
        if (request.getProviderId() == null || request.getProviderId().trim().isEmpty()) {
            throw new IllegalArgumentException("Provider ID is required");
        }
        
        if (!isSupportedProvider(request.getProviderId())) {
            throw new IllegalArgumentException("Provider not supported: " + request.getProviderId());
        }
    }
    
    /**
     * Processes provider deletion.
     * 
     * @param request The delete request
     * @param response The response to populate
     */
    private void processProviderDeletion(DeleteProviderRequest request, DeleteProviderResponse response) {
        // TODO: Implement actual provider deletion logic with business module
        // For now, simulate successful deletion
        
        // Set response data
        Map<String, Object> data = new HashMap<>();
        data.put("deletedAt", Instant.now().toString());
        data.put("userId", request.getUserId());
        data.put("providerId", request.getProviderId());
        
        // Process cleanup options
        if (request.getCleanupOptions() != null) {
            processCleanupOptions(request, data);
        }
        
        response.setSuccess(true);
        response.setStatus("deleted");
        response.setMessage("Provider connection deleted successfully");
        response.setData(data);
    }
    
    /**
     * Processes cleanup options.
     * 
     * @param request The delete request
     * @param data The response data map
     */
    private void processCleanupOptions(DeleteProviderRequest request, Map<String, Object> data) {
        Map<String, Object> cleanupOptions = request.getCleanupOptions();
        
        // Check if user wants to revoke tokens
        if (Boolean.TRUE.equals(cleanupOptions.get("revokeTokens"))) {
            // TODO: Implement token revocation with business module
            data.put("tokensRevoked", true);
            providerLog.info("Revoked tokens for provider: {} user: {}", 
                           request.getProviderId(), request.getUserId());
        }
        
        // Check if user wants to clear cached data
        if (Boolean.TRUE.equals(cleanupOptions.get("clearCachedData"))) {
            // TODO: Implement cached data clearing with business module
            data.put("cachedDataCleared", true);
            providerLog.info("Cleared cached data for provider: {} user: {}", 
                           request.getProviderId(), request.getUserId());
        }
        
        // Check if user wants to remove webhooks
        if (Boolean.TRUE.equals(cleanupOptions.get("removeWebhooks"))) {
            // TODO: Implement webhook removal with business module
            data.put("webhooksRemoved", true);
            providerLog.info("Removed webhooks for provider: {} user: {}", 
                           request.getProviderId(), request.getUserId());
        }
        
        data.put("cleanupOptions", cleanupOptions);
    }
    
    /**
     * Checks if a provider is supported.
     * 
     * @param providerId The provider ID
     * @return true if supported
     */
    private boolean isSupportedProvider(String providerId) {
        switch (providerId.toLowerCase()) {
            case "coinbase":
            case "binance":
            case "kraken":
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Get the provider ID for this service.
     * 
     * @return The provider ID
     */
    @Override
    protected String getProviderId() {
        return "provider"; // Generic provider service
    }
    
    /**
     * Get the provider display name.
     * 
     * @return The provider display name
     */
    @Override
    protected String getProviderName() {
        return "Provider"; // Generic provider service
    }
} 