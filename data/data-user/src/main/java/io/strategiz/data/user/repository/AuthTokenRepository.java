package io.strategiz.data.user.repository;

import io.strategiz.data.user.entity.SsoRelayToken;

import java.util.Optional;

/**
 * Repository for managing one-time SSO relay tokens. Used for cross-app SSO (token relay
 * pattern).
 */
public interface AuthTokenRepository {

	/**
	 * Find a token by its value
	 */
	Optional<SsoRelayToken> findByToken(String token);

	/**
	 * Save a new SSO relay token
	 */
	SsoRelayToken save(SsoRelayToken token);

	/**
	 * Delete a token (after use or expiration)
	 */
	void delete(String token);

	/**
	 * Mark token as used
	 */
	void markAsUsed(String token);

}
