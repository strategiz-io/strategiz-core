package io.strategiz.data.auth.repository.passkey.challenge;

import io.strategiz.data.auth.model.passkey.PasskeyChallenge;
import io.strategiz.data.base.repository.BaseRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing passkey challenges
 * Provides CRUD operations for WebAuthn challenges
 */
@Repository
public interface PasskeyChallengeRepository {
    
    /**
     * Find challenge by challenge string
     */
    Optional<PasskeyChallenge> findByChallenge(String challenge);
    
    /**
     * Find challenges by user ID
     */
    List<PasskeyChallenge> findByUserId(String userId);
    
    /**
     * Find challenges by session ID
     */
    List<PasskeyChallenge> findBySessionId(String sessionId);
    
    /**
     * Find challenges by type
     */
    List<PasskeyChallenge> findByChallengeType(String type);
    
    /**
     * Find challenges by type (using the type field)
     */
    List<PasskeyChallenge> findByType(String type);
    
    /**
     * Find challenges created before a certain time (for cleanup)
     */
    List<PasskeyChallenge> findByCreatedAtBefore(Instant before);
    
    /**
     * Find expired challenges
     */
    List<PasskeyChallenge> findByExpiresAtBefore(Instant now);
    
    /**
     * Find used challenges
     */
    List<PasskeyChallenge> findByUsedTrue();
    
    /**
     * Check if challenge exists by challenge string
     */
    boolean existsByChallenge(String challenge);
    
    /**
     * Delete expired challenges
     */
    void deleteByExpiresAtBefore(Instant now);
    
    /**
     * Delete used challenges
     */
    void deleteByUsedTrue();
    
    /**
     * Delete challenges by user ID
     */
    void deleteByUserId(String userId);
    
    /**
     * Save and return the saved entity
     */
    PasskeyChallenge save(PasskeyChallenge challenge);
    
    /**
     * Save and flush to database immediately
     */
    PasskeyChallenge saveAndFlush(PasskeyChallenge challenge);
    
    /**
     * Delete an entity
     */
    void delete(PasskeyChallenge challenge);
    
    /**
     * Delete multiple entities
     */
    void deleteAll(Iterable<? extends PasskeyChallenge> challenges);
}

