package io.strategiz.data.auth.repository;

import io.strategiz.data.auth.entity.AuthenticationMethodEntity;
import io.strategiz.data.auth.entity.totp.TotpAuthenticationMethodEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data repository for managing authentication methods
 * Provides CRUD operations for all authentication method types in users/{userId}/authentication_methods subcollection
 */
@Repository
public interface AuthenticationMethodRepository extends CrudRepository<AuthenticationMethodEntity, String> {
    
    // ===============================
    // Spring Data Query Methods  
    // ===============================
    
    /**
     * Find authentication methods by type
     */
    List<AuthenticationMethodEntity> findByType(String type);
    
    /**
     * Find authentication methods by name
     */
    List<AuthenticationMethodEntity> findByName(String name);
    
    /**
     * Find authentication methods by name (case insensitive)
     */
    List<AuthenticationMethodEntity> findByNameIgnoreCase(String name);
    
    /**
     * Check if method exists by type
     */
    boolean existsByType(String type);
    
    /**
     * Check if method exists by name
     */
    boolean existsByName(String name);
    
    /**
     * Count authentication methods by type
     */
    long countByType(String type);
    
    /**
     * Delete authentication methods by type
     */
    void deleteByType(String type);
    
    /**
     * Find methods ordered by last verified date
     */
    List<AuthenticationMethodEntity> findAllByOrderByLastVerifiedAtDesc();
    
    /**
     * Find authentication methods by user ID
     */
    List<AuthenticationMethodEntity> findByUserId(String userId);
    
    /**
     * Find authentication method by user ID and provider and provider ID (for OAuth)
     */
    Optional<AuthenticationMethodEntity> findByUserIdAndProviderAndProviderId(String userId, String provider, String providerId);
    
    /**
     * Find authentication methods by user ID and type
     */
    List<AuthenticationMethodEntity> findByUserIdAndType(String userId, String type);
    
    /**
     * Check if user has authentication method of given type
     */
    boolean existsByUserIdAndType(String userId, String type);
    
    /**
     * Delete authentication methods by user ID and type
     */
    void deleteByUserIdAndType(String userId, String type);
    
    /**
     * Find TOTP authentication methods by user ID
     * Spring Data query derivation method
     */
    List<TotpAuthenticationMethodEntity> findTotpByUserId(String userId);
} 