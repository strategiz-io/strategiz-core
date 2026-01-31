package io.strategiz.data.social.repository;

import io.strategiz.data.social.entity.OwnerSubscription;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for owner subscription operations.
 *
 * <p>
 * Manages subscriptions where users subscribe to strategy OWNERS (not individual
 * strategies). Subscribers can deploy all of the owner's PUBLIC strategies.
 * </p>
 *
 * <p>
 * Note: This interface replaces the deprecated UserSubscriptionRepository.
 * </p>
 *
 * @see io.strategiz.data.social.entity.OwnerSubscription
 * @see io.strategiz.data.social.entity.OwnerSubscriptionSettings
 */
public interface OwnerSubscriptionRepository {

	// === Create Operations ===

	/**
	 * Create a new subscription.
	 * @param subscription The subscription to create
	 * @param performingUserId The user performing the action
	 * @return The created subscription
	 */
	OwnerSubscription save(OwnerSubscription subscription, String performingUserId);

	// === Read Operations ===

	/**
	 * Find a subscription by ID.
	 * @param subscriptionId The subscription ID
	 * @return Optional containing the subscription if found
	 */
	Optional<OwnerSubscription> findById(String subscriptionId);

	/**
	 * Find all subscriptions for a subscriber (what the user is subscribed to).
	 * @param subscriberId The subscriber's user ID
	 * @return List of subscriptions
	 */
	List<OwnerSubscription> findBySubscriberId(String subscriberId);

	/**
	 * Find all active subscriptions for a subscriber.
	 * @param subscriberId The subscriber's user ID
	 * @return List of active subscriptions
	 */
	List<OwnerSubscription> findActiveBySubscriberId(String subscriberId);

	/**
	 * Find all subscribers for an owner (who is subscribed to this owner).
	 * @param ownerId The owner's user ID
	 * @return List of subscriptions
	 */
	List<OwnerSubscription> findByOwnerId(String ownerId);

	/**
	 * Find all active subscribers for an owner.
	 * @param ownerId The owner's user ID
	 * @return List of active subscriptions
	 */
	List<OwnerSubscription> findActiveByOwnerId(String ownerId);

	/**
	 * Find an existing subscription between a subscriber and owner.
	 * @param subscriberId The subscriber's user ID
	 * @param ownerId The owner's user ID
	 * @return Optional containing the subscription if it exists
	 */
	Optional<OwnerSubscription> findBySubscriberIdAndOwnerId(String subscriberId, String ownerId);

	/**
	 * Find an active subscription between a subscriber and owner.
	 * @param subscriberId The subscriber's user ID
	 * @param ownerId The owner's user ID
	 * @return Optional containing the active subscription if it exists
	 */
	Optional<OwnerSubscription> findActiveBySubscriberIdAndOwnerId(String subscriberId, String ownerId);

	/**
	 * Find a subscription by Stripe subscription ID.
	 * @param stripeSubscriptionId The Stripe subscription ID
	 * @return Optional containing the subscription if found
	 */
	Optional<OwnerSubscription> findByStripeSubscriptionId(String stripeSubscriptionId);

	// === Count Operations ===

	/**
	 * Count active subscribers for an owner.
	 * @param ownerId The owner's user ID
	 * @return The number of active subscribers
	 */
	int countActiveByOwnerId(String ownerId);

	/**
	 * Count active subscriptions for a subscriber.
	 * @param subscriberId The subscriber's user ID
	 * @return The number of active subscriptions
	 */
	int countActiveBySubscriberId(String subscriberId);

	// === Check Operations ===

	/**
	 * Check if a subscriber has an active subscription to an owner.
	 * @param subscriberId The subscriber's user ID
	 * @param ownerId The owner's user ID
	 * @return True if an active subscription exists
	 */
	boolean hasActiveSubscription(String subscriberId, String ownerId);

	// === Delete Operations ===

	/**
	 * Delete a subscription by ID.
	 * @param subscriptionId The subscription ID
	 */
	void deleteById(String subscriptionId);

}
