package io.strategiz.data.provider.repository;

/**
 * Repository interface for deleting provider integration entities
 * Following Single Responsibility Principle - focused only on delete operations
 */
public interface DeleteProviderIntegrationRepository {
    
    /**
     * Delete a provider integration by ID
     * 
     * @param id The integration ID
     * @return true if deleted, false otherwise
     */
    boolean deleteById(String id);
    
    /**
     * Delete a provider integration by user ID and provider ID
     * 
     * @param userId The user ID
     * @param providerId The provider ID
     * @return true if deleted, false otherwise
     */
    boolean deleteByUserIdAndProviderId(String userId, String providerId);
    
    /**
     * Soft delete a provider integration
     * 
     * @param id The integration ID
     * @param userId The user performing the delete
     * @return true if deleted, false otherwise
     */
    boolean softDelete(String id, String userId);
    
    /**
     * Soft delete a provider integration by user ID and provider ID
     * 
     * @param userId The user ID
     * @param providerId The provider ID
     * @param deleteUserId The user performing the delete
     * @return true if deleted, false otherwise
     */
    boolean softDeleteByUserIdAndProviderId(String userId, String providerId, String deleteUserId);
}