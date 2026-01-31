package io.strategiz.data.social.repository;

import io.strategiz.data.social.entity.OwnerSubscriptionSettings;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Repository interface for owner subscription settings operations.
 *
 * Manages the settings that control whether an owner has subscriptions enabled, their
 * pricing, and Stripe Connect integration.
 */
public interface OwnerSubscriptionSettingsRepository {

	// === Create/Update Operations ===

	/**
	 * Save or update owner subscription settings.
	 * @param settings The settings to save
	 * @param userId Who is performing this action
	 * @return The saved settings
	 */
	OwnerSubscriptionSettings save(OwnerSubscriptionSettings settings, String userId);

	/**
	 * Enable subscriptions for an owner.
	 * @param userId The owner's user ID
	 * @param monthlyPrice The monthly subscription price
	 * @param profilePitch The marketing pitch (max 500 chars)
	 * @return The updated settings
	 */
	OwnerSubscriptionSettings enableSubscriptions(String userId, BigDecimal monthlyPrice, String profilePitch);

	/**
	 * Disable subscriptions for an owner. Note: Existing subscribers will remain active
	 * until they cancel.
	 * @param userId The owner's user ID
	 * @return The updated settings
	 */
	OwnerSubscriptionSettings disableSubscriptions(String userId);

	// === Read Operations ===

	/**
	 * Get settings by user ID.
	 * @param userId The owner's user ID
	 * @return Optional containing the settings if they exist
	 */
	Optional<OwnerSubscriptionSettings> findByUserId(String userId);

	/**
	 * Check if subscriptions are enabled for a user.
	 * @param userId The owner's user ID
	 * @return True if subscriptions are enabled
	 */
	boolean isEnabled(String userId);

	/**
	 * Get the current monthly price for new subscribers.
	 * @param userId The owner's user ID
	 * @return The monthly price, or null if not found
	 */
	BigDecimal getMonthlyPrice(String userId);

	// === Update Operations ===

	/**
	 * Update the monthly price. Note: Existing subscribers keep their grandfathered rate.
	 * @param userId The owner's user ID
	 * @param newPrice The new monthly price for new subscribers
	 * @return The updated settings
	 */
	OwnerSubscriptionSettings updateMonthlyPrice(String userId, BigDecimal newPrice);

	/**
	 * Update the profile pitch.
	 * @param userId The owner's user ID
	 * @param pitch The new profile pitch (max 500 chars)
	 * @return The updated settings
	 */
	OwnerSubscriptionSettings updateProfilePitch(String userId, String pitch);

	/**
	 * Update Stripe Connect status.
	 * @param userId The owner's user ID
	 * @param accountId The Stripe Connect account ID
	 * @param status The connection status (not_started, pending, active, restricted)
	 * @return The updated settings
	 */
	OwnerSubscriptionSettings updateStripeConnectStatus(String userId, String accountId, String status);

	/**
	 * Increment subscriber count.
	 * @param userId The owner's user ID
	 * @return The updated settings
	 */
	OwnerSubscriptionSettings incrementSubscriberCount(String userId);

	/**
	 * Decrement subscriber count.
	 * @param userId The owner's user ID
	 * @return The updated settings
	 */
	OwnerSubscriptionSettings decrementSubscriberCount(String userId);

	/**
	 * Update public strategy count (denormalized).
	 * @param userId The owner's user ID
	 * @param count The new count
	 * @return The updated settings
	 */
	OwnerSubscriptionSettings updatePublicStrategyCount(String userId, int count);

}
