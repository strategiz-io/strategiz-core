package io.strategiz.data.provider.repository;

import io.strategiz.data.provider.entity.ProviderIntegrationEntity;

/**
 * Repository interface for updating provider integration entities
 * Following Single Responsibility Principle - focused only on update operations
 */
public interface UpdateProviderIntegrationRepository {
    
    /**
     * Update an existing provider integration
     * 
     * @param integration The provider integration to update
     * @return The updated provider integration
     */
    ProviderIntegrationEntity update(ProviderIntegrationEntity integration);
    
    /**
     * Update an existing provider integration with audit user
     * 
     * @param integration The provider integration to update
     * @param userId The user performing the update
     * @return The updated provider integration
     */
    ProviderIntegrationEntity updateWithUserId(ProviderIntegrationEntity integration, String userId);
    
    /**
     * Update the status of a provider integration
     * 
     * @param userId The user ID
     * @param providerId The provider ID
     * @param status The new status
     * @return true if updated, false otherwise
     */
    boolean updateStatus(String userId, String providerId, String status);
    
    /**
     * Update the enabled state of a provider integration
     * 
     * @param userId The user ID
     * @param providerId The provider ID
     * @param enabled The new enabled state
     * @return true if updated, false otherwise
     */
    boolean updateEnabled(String userId, String providerId, boolean enabled);
}