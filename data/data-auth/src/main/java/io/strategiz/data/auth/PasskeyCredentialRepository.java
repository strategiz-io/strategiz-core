package io.strategiz.data.auth;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for PasskeyCredential data access operations
 */
public interface PasskeyCredentialRepository {

    /**
     * Find a passkey credential by ID
     *
     * @param id the passkey credential ID
     * @return optional passkey credential
     */
    Optional<PasskeyCredential> findById(String id);
    
    /**
     * Find a passkey credential by credential ID
     *
     * @param credentialId the credential ID
     * @return optional passkey credential
     */
    Optional<PasskeyCredential> findByCredentialId(String credentialId);
    
    /**
     * Find a passkey credential by credential ID and user ID
     *
     * @param credentialId the credential ID
     * @param userId the user ID
     * @return optional passkey credential
     */
    Optional<PasskeyCredential> findByCredentialIdAndUserId(String credentialId, String userId);
    
    /**
     * Find all passkey credentials for a user
     *
     * @param userId the user ID
     * @return list of passkey credentials
     */
    List<PasskeyCredential> findAllByUserId(String userId);
    
    /**
     * Save a passkey credential
     *
     * @param passkeyCredential the passkey credential to save
     * @return the saved passkey credential with ID
     */
    PasskeyCredential save(PasskeyCredential passkeyCredential);
    
    /**
     * Delete a passkey credential by ID
     *
     * @param id the passkey credential ID
     * @return true if deleted, false otherwise
     */
    boolean deleteById(String id);
    
    /**
     * Delete all passkey credentials for a user
     *
     * @param userId the user ID
     * @return number of deleted passkey credentials
     */
    int deleteAllByUserId(String userId);
}
