package io.strategiz.data.auth.repository.passkey.credential;

import io.strategiz.data.auth.model.passkey.PasskeyCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA Repository for PasskeyCredential entities
 */
@Repository
public interface PasskeyCredentialRepository extends JpaRepository<PasskeyCredential, String> {
    
    /**
     * Find a credential by its credential ID
     */
    Optional<PasskeyCredential> findByCredentialId(String credentialId);
    
    /**
     * Find a credential by credential ID and user ID
     */
    Optional<PasskeyCredential> findByCredentialIdAndUserId(String credentialId, String userId);
    
    /**
     * Find all credentials for a specific user
     */
    List<PasskeyCredential> findByUserId(String userId);
    
    /**
     * Alias for findByUserId to maintain compatibility with service layer
     */
    List<PasskeyCredential> findAllByUserId(String userId);
    
    /**
     * Delete credentials by credential ID
     */
    void deleteByCredentialId(String credentialId);
    
    /**
     * Delete all credentials for a specific user
     */
    void deleteByUserId(String userId);
    
    /**
     * Alias for deleteByUserId to maintain compatibility with service layer
     */
    void deleteAllByUserId(String userId);
    
    /**
     * Check if a credential exists by credential ID
     */
    boolean existsByCredentialId(String credentialId);
}
