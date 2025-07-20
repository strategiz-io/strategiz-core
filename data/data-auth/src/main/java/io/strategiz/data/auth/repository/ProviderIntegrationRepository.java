package io.strategiz.data.auth.repository;

import io.strategiz.data.auth.entity.ProviderIntegrationEntity;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for provider integrations stored in users/{userId}/provider_integrations subcollection
 * Implementation should extend SubcollectionRepository
 */
public interface ProviderIntegrationRepository {

    /**
     * Find all provider integrations for a user
     * 
     * @param userId User ID
     * @return List of provider integrations
     */
    List<ProviderIntegrationEntity> findByUserId(String userId);

    /**
     * Find a specific provider integration for a user
     * 
     * @param userId User ID
     * @param providerId Provider ID (e.g., "kraken", "binanceus")
     * @return Optional provider integration
     */
    Optional<ProviderIntegrationEntity> findByUserIdAndProviderId(String userId, String providerId);

    /**
     * Find all enabled provider integrations for a user
     * 
     * @param userId User ID
     * @return List of enabled provider integrations
     */
    List<ProviderIntegrationEntity> findByUserIdAndIsEnabledTrue(String userId);

    /**
     * Find all provider integrations by status for a user
     * 
     * @param userId User ID
     * @param status Status (e.g., "connected", "error")
     * @return List of provider integrations
     */
    List<ProviderIntegrationEntity> findByUserIdAndStatus(String userId, String status);

    /**
     * Find all provider integrations by type for a user
     * 
     * @param userId User ID
     * @param providerType Provider type (e.g., "exchange", "brokerage")
     * @return List of provider integrations
     */
    List<ProviderIntegrationEntity> findByUserIdAndProviderType(String userId, String providerType);

    /**
     * Check if a user has a specific provider integrated
     * 
     * @param userId User ID
     * @param providerId Provider ID
     * @return true if provider exists, false otherwise
     */
    boolean existsByUserIdAndProviderId(String userId, String providerId);

    /**
     * Save a provider integration for a user
     * 
     * @param userId User ID
     * @param entity Provider integration entity
     * @return Saved entity
     */
    ProviderIntegrationEntity saveForUser(String userId, ProviderIntegrationEntity entity);

    /**
     * Delete a provider integration for a user
     * 
     * @param userId User ID
     * @param providerId Provider ID
     * @return true if deleted, false if not found
     */
    boolean deleteByUserIdAndProviderId(String userId, String providerId);

    /**
     * Update provider status for a user
     * 
     * @param userId User ID
     * @param providerId Provider ID
     * @param status New status
     * @return true if updated, false if not found
     */
    boolean updateStatusByUserIdAndProviderId(String userId, String providerId, String status);
}