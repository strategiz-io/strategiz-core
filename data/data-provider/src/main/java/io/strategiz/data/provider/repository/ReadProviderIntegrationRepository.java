package io.strategiz.data.provider.repository;

import io.strategiz.data.provider.entity.ProviderIntegrationEntity;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for reading provider integration entities
 * Following Single Responsibility Principle - focused only on read operations
 */
public interface ReadProviderIntegrationRepository {
    
    /**
     * Find a provider integration by ID
     * 
     * @param id The integration ID
     * @return Optional containing the integration if found
     */
    Optional<ProviderIntegrationEntity> findById(String id);
    
    /**
     * Find all provider integrations for a user
     * 
     * @param userId The user ID
     * @return List of provider integrations
     */
    List<ProviderIntegrationEntity> findByUserId(String userId);
    
    /**
     * Find a provider integration by user ID and provider ID
     * 
     * @param userId The user ID
     * @param providerId The provider ID
     * @return Optional containing the integration if found
     */
    Optional<ProviderIntegrationEntity> findByUserIdAndProviderId(String userId, String providerId);
    
    /**
     * Find all enabled provider integrations for a user
     * 
     * @param userId The user ID
     * @return List of enabled provider integrations
     */
    List<ProviderIntegrationEntity> findByUserIdAndEnabledTrue(String userId);
    
    /**
     * Find provider integrations by user ID and status
     * 
     * @param userId The user ID
     * @param status The status
     * @return List of provider integrations
     */
    List<ProviderIntegrationEntity> findByUserIdAndStatus(String userId, String status);
    
    /**
     * Find provider integrations by user ID and provider type
     * 
     * @param userId The user ID
     * @param providerType The provider type
     * @return List of provider integrations
     */
    List<ProviderIntegrationEntity> findByUserIdAndProviderType(String userId, String providerType);
    
    /**
     * Check if a provider integration exists for a user and provider
     * 
     * @param userId The user ID
     * @param providerId The provider ID
     * @return true if exists, false otherwise
     */
    boolean existsByUserIdAndProviderId(String userId, String providerId);
}