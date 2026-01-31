package io.strategiz.data.auth.repository;

import io.strategiz.data.auth.entity.PushSubscriptionEntity;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing push notification subscriptions.
 */
public interface PushSubscriptionRepository {

	/**
	 * Save a push subscription.
	 */
	PushSubscriptionEntity save(PushSubscriptionEntity entity, String userId);

	/**
	 * Find a subscription by ID.
	 */
	Optional<PushSubscriptionEntity> findById(String id);

	/**
	 * Find all subscriptions for a user.
	 */
	List<PushSubscriptionEntity> findByUserId(String userId);

	/**
	 * Find active subscriptions for a user (push auth enabled, valid).
	 */
	List<PushSubscriptionEntity> findActiveByUserId(String userId);

	/**
	 * Find a subscription by endpoint.
	 */
	Optional<PushSubscriptionEntity> findByEndpoint(String endpoint);

	/**
	 * Find a subscription by user and endpoint.
	 */
	Optional<PushSubscriptionEntity> findByUserIdAndEndpoint(String userId, String endpoint);

	/**
	 * Update a subscription.
	 */
	PushSubscriptionEntity update(PushSubscriptionEntity entity, String userId);

	/**
	 * Delete a subscription.
	 */
	void delete(String id);

	/**
	 * Delete all subscriptions for a user.
	 */
	int deleteAllForUser(String userId);

	/**
	 * Count active subscriptions for a user.
	 */
	long countActiveByUserId(String userId);

	/**
	 * Check if a user has any active push subscriptions.
	 */
	boolean hasActiveSubscriptions(String userId);

}
