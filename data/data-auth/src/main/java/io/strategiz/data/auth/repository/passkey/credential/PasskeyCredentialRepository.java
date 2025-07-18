package io.strategiz.data.auth.repository.passkey.credential;

import io.strategiz.data.auth.entity.passkey.PasskeyCredentialEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing passkey credentials
 * Provides CRUD operations for passkey credentials
 */
@Repository
public interface PasskeyCredentialRepository extends CrudRepository<PasskeyCredentialEntity, String> {
    
    /**
     * Find credential by credential ID
     */
    Optional<PasskeyCredentialEntity> findByCredentialId(String credentialId);
    
    /**
     * Find all credentials for a user (this would typically be done through a user-credential relationship)
     */
    List<PasskeyCredentialEntity> findByUserId(String userId);
    
    /**
     * Find credentials by device
     */
    List<PasskeyCredentialEntity> findByDevice(String device);
    
    /**
     * Find verified credentials
     */
    List<PasskeyCredentialEntity> findByVerifiedTrue();
    
    /**
     * Find credentials created before a certain time
     */
    List<PasskeyCredentialEntity> findByCreatedAtBefore(Instant before);
    
    /**
     * Find credentials last used before a certain time
     */
    List<PasskeyCredentialEntity> findByLastUsedAtBefore(Instant before);
    
    /**
     * Count credentials by user
     */
    long countByUserId(String userId);
    
    /**
     * Check if credential exists by credential ID
     */
    boolean existsByCredentialId(String credentialId);
    
    /**
     * Delete credentials by user ID
     */
    void deleteByUserId(String userId);
}