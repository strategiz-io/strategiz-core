package io.strategiz.data.user.repository;

import io.strategiz.data.user.entity.AuthTokenEntity;

import java.util.Optional;

/**
 * Repository for managing one-time authentication tokens.
 * Used for cross-app SSO (token relay pattern).
 */
public interface AuthTokenRepository {

    /**
     * Find a token by its value
     */
    Optional<AuthTokenEntity> findByToken(String token);

    /**
     * Save a new auth token
     */
    AuthTokenEntity save(AuthTokenEntity token);

    /**
     * Delete a token (after use or expiration)
     */
    void delete(String token);

    /**
     * Mark token as used
     */
    void markAsUsed(String token);
}
