package io.strategiz.data.auth.repository;

import io.strategiz.data.auth.entity.PushAuthRequestEntity;
import io.strategiz.data.auth.entity.PushAuthStatus;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing push authentication requests.
 */
public interface PushAuthRequestRepository {

	/**
	 * Save a push auth request.
	 */
	PushAuthRequestEntity save(PushAuthRequestEntity entity, String userId);

	/**
	 * Find a request by ID.
	 */
	Optional<PushAuthRequestEntity> findById(String id);

	/**
	 * Find a request by challenge token.
	 */
	Optional<PushAuthRequestEntity> findByChallenge(String challenge);

	/**
	 * Find pending requests for a user.
	 */
	List<PushAuthRequestEntity> findPendingByUserId(String userId);

	/**
	 * Find requests by status.
	 */
	List<PushAuthRequestEntity> findByStatus(PushAuthStatus status);

	/**
	 * Update a request.
	 */
	PushAuthRequestEntity update(PushAuthRequestEntity entity, String userId);

	/**
	 * Cancel all pending requests for a user. Called when user signs in through another
	 * method.
	 */
	int cancelPendingForUser(String userId, String systemUserId);

	/**
	 * Mark expired requests.
	 */
	int markExpired();

	/**
	 * Delete old completed/expired requests.
	 */
	int deleteOldRequests(int olderThanHours);

}
