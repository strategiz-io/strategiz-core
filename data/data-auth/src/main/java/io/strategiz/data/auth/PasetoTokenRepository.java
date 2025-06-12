package io.strategiz.data.auth;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing PASETO tokens
 */
public interface PasetoTokenRepository {
    
    /**
     * Save or update a PASETO token
     *
     * @param token PASETO token to save or update
     * @return The saved PASETO token
     */
    PasetoToken save(PasetoToken token);
    
    /**
     * Find a PASETO token by its ID
     *
     * @param id PASETO token ID
     * @return Optional containing the PASETO token if found
     */
    Optional<PasetoToken> findById(String id);
    
    /**
     * Find a PASETO token by its token value
     *
     * @param tokenValue PASETO token value
     * @return Optional containing the PASETO token if found
     */
    Optional<PasetoToken> findByTokenValue(String tokenValue);
    
    /**
     * Find all PASETO tokens for a user
     *
     * @param userId User ID
     * @return List of PASETO tokens
     */
    List<PasetoToken> findAllByUserId(String userId);
    
    /**
     * Find all active (non-expired, non-revoked) PASETO tokens for a user
     *
     * @param userId User ID
     * @return List of active PASETO tokens
     */
    List<PasetoToken> findActiveTokensByUserId(String userId);
    
    /**
     * Delete a PASETO token by its ID
     *
     * @param id PASETO token ID
     * @return true if deleted, false otherwise
     */
    boolean deleteById(String id);
    
    /**
     * Delete all PASETO tokens for a user
     *
     * @param userId User ID
     * @return Number of deleted tokens
     */
    int deleteAllByUserId(String userId);
    
    /**
     * Delete all expired tokens
     *
     * @param currentTime Current timestamp in seconds
     * @return Number of deleted tokens
     */
    int deleteExpiredTokens(long currentTime);
}
