package io.strategiz.data.auth.repository.session;

import io.strategiz.data.auth.entity.session.PasetoTokenEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data repository for managing PASETO authentication tokens
 * Provides CRUD operations for PASETO tokens in sessions collection
 */
@Repository
public interface PasetoTokenRepository extends CrudRepository<PasetoTokenEntity, String> {
    
    // ===============================
    // Spring Data Query Methods  
    // ===============================
    
    /**
     * Find a token by its value
     */
    Optional<PasetoTokenEntity> findByTokenValue(String tokenValue);
    
    /**
     * Find all tokens for a specific user
     */
    List<PasetoTokenEntity> findByUserId(String userId);
    
    /**
     * Find all active (non-revoked) tokens for a specific user
     */
    List<PasetoTokenEntity> findByUserIdAndRevokedFalse(String userId);
    
    /**
     * Find active tokens by user and type
     */
    List<PasetoTokenEntity> findByUserIdAndTokenTypeAndRevokedFalse(String userId, String tokenType);
    
    /**
     * Find tokens by device ID
     */
    List<PasetoTokenEntity> findByDeviceId(String deviceId);
    
    /**
     * Find revoked tokens
     */
    List<PasetoTokenEntity> findByRevokedTrue();
    
    /**
     * Find tokens that expire before given time
     */
    List<PasetoTokenEntity> findByExpiresAtBefore(Instant expiresBefore);
    
    /**
     * Find expired tokens for cleanup
     */
    List<PasetoTokenEntity> findByExpiresAtBeforeOrRevokedTrue(Instant expiresBefore);
    
    /**
     * Check if token exists by value
     */
    boolean existsByTokenValue(String tokenValue);
    
    /**
     * Count active tokens by user
     */
    long countByUserIdAndRevokedFalse(String userId);
    
    /**
     * Count tokens by type and user
     */
    long countByUserIdAndTokenTypeAndRevokedFalse(String userId, String tokenType);
    
    /**
     * Delete expired tokens
     */
    void deleteByExpiresAtBefore(Instant expiresBefore);
    
    /**
     * Delete revoked tokens
     */
    void deleteByRevokedTrue();
    
    /**
     * Delete tokens by user ID
     */
    void deleteByUserId(String userId);
}
