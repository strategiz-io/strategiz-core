package io.strategiz.data.auth.repository;

import io.strategiz.data.auth.entity.AuthenticationMethodEntity;
import io.strategiz.data.auth.entity.AuthenticationMethodType;
import io.strategiz.data.base.repository.BaseRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing authentication methods in users/{userId}/authentication_methods subcollection
 * Provides CRUD operations for all authentication method types (TOTP, OAuth, Passkey, SMS OTP, etc.)
 * 
 * This is an interface that defines subcollection-specific operations.
 * Implementation should extend SubcollectionRepository.
 */
public interface AuthenticationMethodRepository {
    
    /**
     * Find all authentication methods for a specific user
     */
    List<AuthenticationMethodEntity> findByUserId(String userId);
    
    /**
     * Find authentication methods by user ID and type
     */
    List<AuthenticationMethodEntity> findByUserIdAndType(String userId, AuthenticationMethodType type);
    
    /**
     * Find a specific authentication method by user ID and method ID
     */
    Optional<AuthenticationMethodEntity> findByUserIdAndId(String userId, String methodId);
    
    /**
     * Find enabled authentication methods for a user
     */
    List<AuthenticationMethodEntity> findByUserIdAndIsEnabled(String userId, boolean isEnabled);
    
    /**
     * Find enabled authentication methods by user ID and type
     */
    List<AuthenticationMethodEntity> findByUserIdAndTypeAndIsEnabled(String userId, AuthenticationMethodType type, boolean isEnabled);
    
    /**
     * Check if user has authentication method of given type
     */
    boolean existsByUserIdAndType(String userId, AuthenticationMethodType type);
    
    /**
     * Check if user has enabled authentication method of given type
     */
    boolean existsByUserIdAndTypeAndIsEnabled(String userId, AuthenticationMethodType type, boolean isEnabled);
    
    /**
     * Count authentication methods by user ID and type
     */
    long countByUserIdAndType(String userId, AuthenticationMethodType type);
    
    /**
     * Delete authentication methods by user ID and type
     */
    void deleteByUserIdAndType(String userId, AuthenticationMethodType type);
    
    /**
     * Save authentication method for a specific user (subcollection)
     */
    AuthenticationMethodEntity saveForUser(String userId, AuthenticationMethodEntity entity);
    
    /**
     * Delete authentication method for a specific user
     */
    void deleteForUser(String userId, String methodId);
} 