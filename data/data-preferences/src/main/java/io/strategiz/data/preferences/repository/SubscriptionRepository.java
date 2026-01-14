package io.strategiz.data.preferences.repository;

import com.google.cloud.firestore.Firestore;
import io.strategiz.data.base.repository.SubcollectionRepository;
import io.strategiz.data.preferences.entity.SubscriptionTier;
import io.strategiz.data.preferences.entity.UserSubscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Repository for UserSubscription stored at users/{userId}/subscription/current
 */
@Repository
public class SubscriptionRepository extends SubcollectionRepository<UserSubscription> {

	private static final Logger logger = LoggerFactory.getLogger(SubscriptionRepository.class);

	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

	public SubscriptionRepository(Firestore firestore) {
		super(firestore, UserSubscription.class);
	}

	@Override
	protected String getModuleName() {
		return "data-preferences";
	}

	@Override
	protected String getParentCollectionName() {
		return "users";
	}

	@Override
	protected String getSubcollectionName() {
		return "subscription";
	}

	/**
	 * Get subscription for a user. Creates default (Trial) subscription if none exists.
	 * @param userId The user ID
	 * @return The user subscription
	 */
	public UserSubscription getByUserId(String userId) {
		validateParentId(userId);

		Optional<UserSubscription> existing = findByIdInSubcollection(userId, UserSubscription.SUBSCRIPTION_ID);

		if (existing.isPresent()) {
			UserSubscription sub = existing.get();
			// Reset daily usage if it's a new day (legacy compatibility)
			resetDailyUsageIfNeeded(userId, sub);
			return sub;
		}

		// Return default subscription (Trial tier - not persisted until upgraded)
		UserSubscription defaults = new UserSubscription(SubscriptionTier.TRIAL);
		defaults.setSubscriptionId(UserSubscription.SUBSCRIPTION_ID);
		return defaults;
	}

	/**
	 * Save subscription for a user.
	 * @param userId The user ID
	 * @param subscription The subscription to save
	 * @return The saved subscription
	 */
	public UserSubscription save(String userId, UserSubscription subscription) {
		validateParentId(userId);

		// Ensure correct document ID
		subscription.setSubscriptionId(UserSubscription.SUBSCRIPTION_ID);

		// Set usage reset date if not set
		if (subscription.getUsageResetDate() == null) {
			subscription.setUsageResetDate(LocalDate.now().format(DATE_FORMAT));
		}

		// Check if this is a new subscription (doesn't exist in Firestore yet)
		// This is needed because getByUserId() returns in-memory defaults with ID already set
		boolean existsInFirestore = findByIdInSubcollection(userId, UserSubscription.SUBSCRIPTION_ID).isPresent();

		if (!existsInFirestore) {
			// Initialize audit fields for new subscription
			subscription._initAudit(userId);
		} else {
			// Update audit fields for existing subscription
			subscription._updateAudit(userId);
		}

		// Validate entity state
		subscription._validate();

		// Save to Firestore (skip audit updates in saveInSubcollection since we handled them here)
		try {
			getSubcollection(userId).document(subscription.getId()).set(subscription).get();
			logger.debug("Saved UserSubscription for user {}", userId);
			return subscription;
		} catch (Exception e) {
			throw new io.strategiz.data.base.exception.DataRepositoryException(
				io.strategiz.data.base.exception.DataRepositoryErrorDetails.SUBCOLLECTION_ACCESS_FAILED,
				e, "UserSubscription", userId);
		}
	}

	/**
	 * Update subscription tier.
	 * @param userId The user ID
	 * @param tier The new tier
	 * @return The updated subscription
	 */
	public UserSubscription updateTier(String userId, SubscriptionTier tier) {
		UserSubscription sub = getByUserId(userId);
		sub.setTier(tier.getId());
		return save(userId, sub);
	}

	/**
	 * Increment daily message usage.
	 * @param userId The user ID
	 * @return The updated subscription
	 */
	public UserSubscription incrementMessageUsage(String userId) {
		UserSubscription sub = getByUserId(userId);
		sub.setDailyMessagesUsed(sub.getDailyMessagesUsed() + 1);
		return save(userId, sub);
	}

	/**
	 * Increment daily strategy usage.
	 * @param userId The user ID
	 * @return The updated subscription
	 */
	public UserSubscription incrementStrategyUsage(String userId) {
		UserSubscription sub = getByUserId(userId);
		sub.setDailyMessagesUsed(sub.getDailyMessagesUsed() + 1);
		return save(userId, sub);
	}

	/**
	 * Check and reset daily usage if it's a new day.
	 */
	private void resetDailyUsageIfNeeded(String userId, UserSubscription sub) {
		String today = LocalDate.now().format(DATE_FORMAT);
		if (!today.equals(sub.getUsageResetDate())) {
			logger.info("Resetting daily usage for user {} (was {})", userId, sub.getUsageResetDate());
			sub.setDailyMessagesUsed(0);
			// Strategy usage now combined with message usage (dailyMessagesUsed)
			sub.setUsageResetDate(today);
			save(userId, sub);
		}
	}

	/**
	 * Get the current tier for a user.
	 * @param userId The user ID
	 * @return The subscription tier
	 */
	public SubscriptionTier getTier(String userId) {
		UserSubscription sub = getByUserId(userId);
		return sub.getTierEnum();
	}

}
