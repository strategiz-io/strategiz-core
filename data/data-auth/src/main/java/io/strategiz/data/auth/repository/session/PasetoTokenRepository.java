package io.strategiz.data.auth.repository.session;

import io.strategiz.data.auth.model.session.PasetoToken;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing PASETO authentication tokens.
 */
@Repository
public interface PasetoTokenRepository {
    /**
     * Save a token to the repository.
     * 
     * @param token The token to save
     * @return The saved token
     */
    PasetoToken save(PasetoToken token);
    
    /**
     * Delete a token from the repository.
     * 
     * @param token The token to delete
     */
    void delete(PasetoToken token);
    
    /**
     * Find a token by its value.
     * 
     * @param tokenValue The token value to search for
     * @return Optional containing the token if found, empty otherwise
     */
    Optional<PasetoToken> findByTokenValue(String tokenValue);
    
    /**
     * Find all tokens for a specific user.
     * 
     * @param userId The user ID to find tokens for
     * @return List of tokens belonging to the user
     */
    List<PasetoToken> findAllByUserId(String userId);
    
    /**
     * Find all active (non-revoked) tokens for a specific user.
     * 
     * @param userId The user ID to search for
     * @return List of active tokens belonging to the user
     */
    List<PasetoToken> findActiveTokensByUserId(String userId);
    
    /**
     * Delete all tokens that have expired before the specified timestamp.
     * 
     * @param timestamp Unix timestamp (seconds) representing current time
     * @return Number of deleted tokens
     */
    int deleteExpiredTokens(long timestamp);
}
