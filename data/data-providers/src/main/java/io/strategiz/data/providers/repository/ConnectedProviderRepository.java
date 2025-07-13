package io.strategiz.data.providers.repository;

import io.strategiz.data.providers.entity.ConnectedProvider;

import java.util.List;
import java.util.Optional;

/**
 * Repository for users/{userId}/providers subcollection
 */
public interface ConnectedProviderRepository {
    
    /**
     * Connect provider to user
     */
    ConnectedProvider connectProvider(String userId, ConnectedProvider provider);
    
    /**
     * Get all connected providers for user
     */
    List<ConnectedProvider> findByUserId(String userId);
    
    /**
     * Get providers by type
     */
    List<ConnectedProvider> findByUserIdAndType(String userId, String providerType);
    
    /**
     * Get specific connected provider
     */
    Optional<ConnectedProvider> findByUserIdAndProviderId(String userId, String providerId);
    
    /**
     * Find provider by name
     */
    Optional<ConnectedProvider> findByUserIdAndProviderName(String userId, String providerName);
    
    /**
     * Update provider
     */
    ConnectedProvider updateProvider(String userId, String providerId, ConnectedProvider provider);
    
    /**
     * Update provider status
     */
    void updateStatus(String userId, String providerId, String status);
    
    /**
     * Mark provider sync
     */
    void markLastSync(String userId, String providerId);
    
    /**
     * Mark provider error
     */
    void markError(String userId, String providerId, String errorMessage);
    
    /**
     * Disconnect provider
     */
    void disconnectProvider(String userId, String providerId);
    
    /**
     * Check if user has provider connected
     */
    boolean hasProviderConnected(String userId, String providerName);
    
    /**
     * Count connected providers
     */
    long countByUserId(String userId);
    
    /**
     * Get providers by status
     */
    List<ConnectedProvider> findByUserIdAndStatus(String userId, String status);
    
    /**
     * Get trading providers
     */
    List<ConnectedProvider> findTradingProviders(String userId);
}