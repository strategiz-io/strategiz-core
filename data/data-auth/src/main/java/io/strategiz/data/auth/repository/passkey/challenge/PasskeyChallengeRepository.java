package io.strategiz.data.auth.repository.passkey.challenge;

import io.strategiz.data.auth.model.passkey.PasskeyChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * JPA Repository for PasskeyChallenge entities
 */
@Repository
public interface PasskeyChallengeRepository extends JpaRepository<PasskeyChallenge, String> {
    
    /**
     * Find a challenge by its challenge string
     */
    Optional<PasskeyChallenge> findByChallenge(String challenge);
    
    /**
     * Find all challenges for a specific user
     */
    List<PasskeyChallenge> findByUserId(String userId);
    
    /**
     * Find challenges by user ID and credential ID
     */
    List<PasskeyChallenge> findByUserIdAndCredentialId(String userId, String credentialId);
    
    /**
     * Find unused challenges for a user that have not expired
     */
    List<PasskeyChallenge> findByUserIdAndUsedFalseAndExpiresAtAfter(String userId, Instant now);
    
    /**
     * Find all expired challenges
     */
    List<PasskeyChallenge> findByExpiresAtBefore(Instant now);
    
    /**
     * Find challenges by type
     */
    List<PasskeyChallenge> findByChallengeType(String challengeType);
    
    /**
     * Delete expired challenges
     */
    void deleteByExpiresAtBefore(Instant now);
}
