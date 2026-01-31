package io.strategiz.data.session.repository;

import io.strategiz.data.session.entity.SessionEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data repository for managing authentication sessions Provides CRUD operations
 * for sessions in the 'sessions' collection Following SOLID principles with clean Spring
 * Data patterns
 */
@Repository
public interface SessionRepository extends CrudRepository<SessionEntity, String> {

	// ===============================
	// Token Lookup Methods
	// ===============================

	/**
	 * Find a session by its token value Used for token validation
	 */
	Optional<SessionEntity> findByTokenValue(String tokenValue);

	// ===============================
	// User Session Methods
	// ===============================

	/**
	 * Find all sessions for a specific user
	 */
	List<SessionEntity> findByUserId(String userId);

	/**
	 * Find all active (non-revoked) sessions for a user
	 */
	List<SessionEntity> findByUserIdAndRevokedFalse(String userId);

	/**
	 * Find sessions by user and token type
	 */
	List<SessionEntity> findByUserIdAndTokenType(String userId, String tokenType);

	/**
	 * Find active sessions by user and token type
	 */
	List<SessionEntity> findByUserIdAndTokenTypeAndRevokedFalse(String userId, String tokenType);

	// ===============================
	// Device Session Methods
	// ===============================

	/**
	 * Find all sessions for a specific device
	 */
	List<SessionEntity> findByDeviceId(String deviceId);

	/**
	 * Find active sessions for a device
	 */
	List<SessionEntity> findByDeviceIdAndRevokedFalse(String deviceId);

	// ===============================
	// Maintenance Methods
	// ===============================

	/**
	 * Find sessions that expire before given time Used for cleanup operations
	 */
	List<SessionEntity> findByExpiresAtBefore(Instant expiresBefore);

	/**
	 * Find revoked sessions for cleanup
	 */
	List<SessionEntity> findByRevokedTrue();

	/**
	 * Find sessions by revocation or expiry Used for batch cleanup operations
	 */
	List<SessionEntity> findByExpiresAtBeforeOrRevokedTrue(Instant expiresBefore);

	// ===============================
	// Validation Methods
	// ===============================

	/**
	 * Check if a token exists
	 */
	boolean existsByTokenValue(String tokenValue);

	/**
	 * Count active sessions for a user
	 */
	long countByUserIdAndRevokedFalse(String userId);

	/**
	 * Count sessions by type for a user
	 */
	long countByUserIdAndTokenTypeAndRevokedFalse(String userId, String tokenType);

	// ===============================
	// Cleanup Methods
	// ===============================

	/**
	 * Delete expired sessions
	 */
	void deleteByExpiresAtBefore(Instant expiresBefore);

	/**
	 * Delete revoked sessions
	 */
	void deleteByRevokedTrue();

	/**
	 * Delete all sessions for a user
	 */
	void deleteByUserId(String userId);

	/**
	 * Delete all sessions for a device
	 */
	void deleteByDeviceId(String deviceId);

}