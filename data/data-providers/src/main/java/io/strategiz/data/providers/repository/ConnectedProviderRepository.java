package io.strategiz.data.providers.repository;

import io.strategiz.data.providers.entity.ConnectedProviderEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data repository for users/{userId}/providers subcollection
 */
@Repository
public interface ConnectedProviderRepository extends CrudRepository<ConnectedProviderEntity, String> {
    
    // ===============================
    // Spring Data Query Methods
    // ===============================
    
    /**
     * Find providers by name
     */
    List<ConnectedProviderEntity> findByProviderName(String providerName);
    
    /**
     * Find providers by type
     */
    List<ConnectedProviderEntity> findByProviderType(String providerType);
    
    /**
     * Find providers by status
     */
    List<ConnectedProviderEntity> findByStatus(String status);
    
    /**
     * Find providers by account type
     */
    List<ConnectedProviderEntity> findByAccountType(String accountType);
    
    /**
     * Find providers by name and type
     */
    List<ConnectedProviderEntity> findByProviderNameAndProviderType(String providerName, String providerType);
    
    /**
     * Find providers by status and type
     */
    List<ConnectedProviderEntity> findByStatusAndProviderType(String status, String providerType);
    
    /**
     * Find active providers (alias for findByStatus)
     */
    default List<ConnectedProviderEntity> findActiveProviders() {
        return findByStatus("ACTIVE");
    }
    
    /**
     * Check if provider exists by name
     */
    boolean existsByProviderName(String providerName);
    
    /**
     * Count providers by type
     */
    long countByProviderType(String providerType);
    
    /**
     * Count providers by status
     */
    long countByStatus(String status);
    
    /**
     * Find providers ordered by connected date
     */
    List<ConnectedProviderEntity> findAllByOrderByConnectedAtDesc();
}