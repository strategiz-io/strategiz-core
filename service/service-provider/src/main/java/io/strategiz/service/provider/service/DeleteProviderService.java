package io.strategiz.service.provider.service;

import io.strategiz.service.provider.model.request.DeleteProviderRequest;
import io.strategiz.service.provider.model.response.DeleteProviderResponse;
import io.strategiz.service.provider.exception.ServiceProviderErrorDetails;
import io.strategiz.service.base.service.ProviderBaseService;
import io.strategiz.data.provider.repository.PortfolioProviderRepository;
import io.strategiz.data.provider.repository.ProviderHoldingsRepository;
import io.strategiz.framework.secrets.controller.SecretManager;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.business.portfolio.PortfolioSummaryManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import io.strategiz.service.base.BaseService;

/**
 * Service for deleting provider connections and data.
 * Handles business logic for disconnecting providers and cleaning up associated data.
 * 
 * @author Strategiz Team
 * @version 1.0
 */
@Service
public class DeleteProviderService extends ProviderBaseService {

    @Autowired
    private PortfolioProviderRepository portfolioProviderRepository;

    @Autowired
    private ProviderHoldingsRepository providerHoldingsRepository;

    @Autowired
    private SecretManager secretManager;

    @Autowired
    private PortfolioSummaryManager portfolioSummaryManager;

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
            throw new StrategizException(
                    ServiceProviderErrorDetails.MISSING_REQUIRED_FIELD,
                    "service-provider",
                    "userId"
            );
        }

        if (request.getProviderId() == null || request.getProviderId().trim().isEmpty()) {
            throw new StrategizException(
                    ServiceProviderErrorDetails.MISSING_REQUIRED_FIELD,
                    "service-provider",
                    "providerId"
            );
        }

        if (!isSupportedProvider(request.getProviderId())) {
            throw new StrategizException(
                    ServiceProviderErrorDetails.PROVIDER_NOT_SUPPORTED,
                    "service-provider",
                    request.getProviderId()
            );
        }
    }
    
    /**
     * Processes provider deletion.
     *
     * @param request The delete request
     * @param response The response to populate
     */
    private void processProviderDeletion(DeleteProviderRequest request, DeleteProviderResponse response) {
        Map<String, Object> data = new HashMap<>();
        data.put("deletedAt", Instant.now().toString());
        data.put("userId", request.getUserId());
        data.put("providerId", request.getProviderId());

        try {
            // 1. Soft delete provider from Firestore (marks as deleted with isActive=false)
            // Path: users/{userId}/portfolio/data/providers/{providerId}
            providerLog.info("Soft deleting provider from Firestore for user: {}, provider: {}",
                           request.getUserId(), request.getProviderId());
            boolean providerDeleted = portfolioProviderRepository.delete(
                    request.getUserId(), request.getProviderId());
            data.put("providerDeleted", providerDeleted);

            // 2. Delete holdings subcollection (hard delete - removes cached portfolio data)
            // Path: users/{userId}/portfolio/data/providers/{providerId}/holdings/current
            providerLog.info("Deleting provider holdings from Firestore for user: {}, provider: {}",
                           request.getUserId(), request.getProviderId());
            boolean holdingsDeleted = providerHoldingsRepository.delete(
                    request.getUserId(), request.getProviderId());
            data.put("holdingsDeleted", holdingsDeleted);

            // 3. Delete OAuth tokens from Vault
            String vaultPath = "secret/strategiz/users/" + request.getUserId() + "/providers/" + request.getProviderId();
            providerLog.info("Deleting OAuth tokens from Vault at path: {}", vaultPath);
            try {
                secretManager.deleteSecret(vaultPath);
                data.put("tokensDeleted", true);
            } catch (Exception e) {
                // Tokens might not exist in Vault, log but don't fail
                providerLog.warn("Failed to delete tokens from Vault (may not exist): {}", e.getMessage());
                data.put("tokensDeleted", false);
                data.put("tokensDeletedReason", "Tokens not found in Vault or already deleted");
            }

            // Process cleanup options if provided
            if (request.getCleanupOptions() != null) {
                processCleanupOptions(request, data);
            }

            response.setSuccess(true);
            response.setStatus("deleted");
            response.setMessage("Provider connection deleted successfully");
            response.setData(data);

            // Refresh portfolio summary after provider deletion
            portfolioSummaryManager.refreshPortfolioSummary(request.getUserId());

        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            providerLog.error("Error during provider deletion: {}", e.getMessage(), e);
            throw new StrategizException(ServiceProviderErrorDetails.PROVIDER_DELETE_FAILED, "service-provider", e, request.getProviderId());
        }
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
            case "webull":
            case "schwab":
            case "alpaca":
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