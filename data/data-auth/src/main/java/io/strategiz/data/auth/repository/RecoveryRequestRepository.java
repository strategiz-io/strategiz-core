package io.strategiz.data.auth.repository;

import io.strategiz.data.auth.entity.RecoveryRequestEntity;
import io.strategiz.data.auth.entity.RecoveryStatus;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing account recovery requests. Recovery requests are stored in the
 * recovery_requests top-level collection.
 *
 * <p>
 * Key operations:
 * </p>
 * <ul>
 * <li>Create and track recovery requests</li>
 * <li>Find active (non-expired) recovery requests</li>
 * <li>Rate limiting checks by email/IP</li>
 * <li>Cleanup of expired requests</li>
 * </ul>
 */
public interface RecoveryRequestRepository {

	/**
	 * Save a recovery request.
	 * @param entity the recovery request entity
	 * @param userId system user ID for audit (typically "system" for recovery)
	 * @return the saved entity with generated ID
	 */
	RecoveryRequestEntity save(RecoveryRequestEntity entity, String userId);

	/**
	 * Find a recovery request by ID.
	 * @param id the recovery request ID
	 * @return the recovery request if found
	 */
	Optional<RecoveryRequestEntity> findById(String id);

	/**
	 * Find active recovery requests for a user. Active = not expired, not completed, not
	 * cancelled.
	 * @param userId the user ID
	 * @return list of active recovery requests
	 */
	List<RecoveryRequestEntity> findActiveByUserId(String userId);

	/**
	 * Find active recovery requests by email.
	 * @param email the email address
	 * @return list of active recovery requests
	 */
	List<RecoveryRequestEntity> findActiveByEmail(String email);

	/**
	 * Find recovery requests by status.
	 * @param status the recovery status
	 * @return list of recovery requests with the given status
	 */
	List<RecoveryRequestEntity> findByStatus(RecoveryStatus status);

	/**
	 * Count recovery requests for an email in the last N hours. Used for rate limiting.
	 * @param email the email address
	 * @param hours the time window in hours
	 * @return number of requests
	 */
	long countByEmailInLastHours(String email, int hours);

	/**
	 * Count recovery requests from an IP in the last N hours. Used for rate limiting.
	 * @param ipAddress the IP address
	 * @param hours the time window in hours
	 * @return number of requests
	 */
	long countByIpInLastHours(String ipAddress, int hours);

	/**
	 * Update a recovery request.
	 * @param entity the recovery request entity
	 * @param userId system user ID for audit
	 * @return the updated entity
	 */
	RecoveryRequestEntity update(RecoveryRequestEntity entity, String userId);

	/**
	 * Delete expired recovery requests. Called periodically to clean up old requests.
	 * @return number of deleted requests
	 */
	int deleteExpired();

	/**
	 * Cancel all active recovery requests for a user. Called when user successfully logs
	 * in or completes recovery.
	 * @param userId the user ID
	 * @param systemUserId system user ID for audit
	 * @return number of cancelled requests
	 */
	int cancelAllActiveForUser(String userId, String systemUserId);

}
