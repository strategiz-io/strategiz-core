package io.strategiz.business.preferences.service;

import io.strategiz.data.preferences.entity.SubscriptionTier;
import io.strategiz.data.preferences.entity.UserSubscription;
import io.strategiz.data.preferences.repository.SubscriptionRepository;
import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.data.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing user subscriptions and usage limits.
 */
@Service
public class SubscriptionService {

	private static final Logger logger = LoggerFactory.getLogger(SubscriptionService.class);

	private static final String ADMIN_ROLE = "ADMIN";

	private final SubscriptionRepository repository;

	private final UserRepository userRepository;

	public SubscriptionService(SubscriptionRepository repository, UserRepository userRepository) {
		this.repository = repository;
		this.userRepository = userRepository;
	}

	/**
	 * Get subscription for a user.
	 * @param userId The user ID
	 * @return The user subscription
	 */
	public UserSubscription getSubscription(String userId) {
		logger.debug("Getting subscription for user {}", userId);
		return repository.getByUserId(userId);
	}

	/**
	 * Get the subscription tier for a user.
	 * @param userId The user ID
	 * @return The subscription tier
	 */
	public SubscriptionTier getTier(String userId) {
		return repository.getTier(userId);
	}

	/**
	 * Check if a model is allowed for the user's subscription tier.
	 * ADMIN users have access to all models for testing purposes.
	 * @param userId The user ID
	 * @param modelId The model ID
	 * @return true if the model is allowed or user is admin
	 */
	public boolean isModelAllowed(String userId, String modelId) {
		// Admins can use all models for testing
		if (isAdmin(userId)) {
			logger.debug("Admin user {} granted access to model {}", userId, modelId);
			return true;
		}

		SubscriptionTier tier = getTier(userId);
		boolean allowed = tier.isModelAllowed(modelId);
		if (!allowed) {
			logger.info("Model {} not allowed for user {} on tier {}", modelId, userId, tier.getId());
		}
		return allowed;
	}

	/**
	 * Get allowed models for a user's subscription tier.
	 * @param userId The user ID
	 * @return List of allowed model IDs
	 */
	public List<String> getAllowedModels(String userId) {
		return getTier(userId).getAllowedModels();
	}

	/**
	 * Check if user can use Alpha Mode (historical data analysis for strategy generation).
	 * Alpha Mode is available to TRADER and STRATEGIST tiers.
	 * ADMIN users have access for testing purposes.
	 * @param userId The user ID
	 * @return true if user can use Alpha Mode
	 */
	public boolean canUseAlphaMode(String userId) {
		// Admins can use Alpha Mode for testing
		if (isAdmin(userId)) {
			logger.debug("Admin user {} granted access to Alpha Mode", userId);
			return true;
		}

		SubscriptionTier tier = getTier(userId);
		boolean canUse = tier == SubscriptionTier.TRADER || tier == SubscriptionTier.STRATEGIST;

		if (!canUse) {
			logger.info("Alpha Mode not available for user {} on tier {}", userId, tier.getId());
		}

		return canUse;
	}

	/**
	 * Check if user can send a message (within daily limit).
	 * Covers both Learn AI Chat and Labs Strategy Generation.
	 * ADMIN users bypass all limits for testing purposes.
	 * @param userId The user ID
	 * @return true if within limit or user is admin
	 */
	public boolean canSendMessage(String userId) {
		// Admins bypass all subscription limits for testing
		if (isAdmin(userId)) {
			logger.debug("Admin user {} bypassing subscription limits", userId);
			return true;
		}

		UserSubscription sub = getSubscription(userId);
		SubscriptionTier tier = sub.getTierEnum();

		if (tier.hasUnlimitedMessages()) {
			return true;
		}

		return sub.getDailyMessagesUsed() < tier.getDailyMessageLimit();
	}

	/**
	 * Record a message sent by the user (Learn chat or Labs strategy generation).
	 * @param userId The user ID
	 * @return The updated subscription
	 */
	public UserSubscription recordMessageUsage(String userId) {
		logger.debug("Recording AI chat message usage for user {}", userId);
		return repository.incrementMessageUsage(userId);
	}

	/**
	 * Get remaining messages for the day.
	 * @param userId The user ID
	 * @return Remaining messages
	 */
	public int getRemainingMessages(String userId) {
		UserSubscription sub = getSubscription(userId);
		SubscriptionTier tier = sub.getTierEnum();

		if (tier.hasUnlimitedMessages()) {
			return -1;
		}

		return Math.max(0, tier.getDailyMessageLimit() - sub.getDailyMessagesUsed());
	}

	/**
	 * Update subscription (for Stripe webhook handling).
	 * @param userId The user ID
	 * @param subscription The updated subscription
	 * @return The saved subscription
	 */
	public UserSubscription updateSubscription(String userId, UserSubscription subscription) {
		logger.info("Updating subscription for user {} to tier {}", userId, subscription.getTier());
		return repository.save(userId, subscription);
	}

	/**
	 * Upgrade user to a new tier.
	 * @param userId The user ID
	 * @param newTier The new tier
	 * @return The updated subscription
	 */
	public UserSubscription upgradeTier(String userId, SubscriptionTier newTier) {
		logger.info("Upgrading user {} to tier {}", userId, newTier.getId());
		return repository.updateTier(userId, newTier);
	}

	/**
	 * Cancel subscription (set to cancel at period end).
	 * @param userId The user ID
	 * @return The updated subscription
	 */
	public UserSubscription cancelSubscription(String userId) {
		logger.info("Canceling subscription for user {}", userId);
		UserSubscription sub = getSubscription(userId);
		sub.setCancelAtPeriodEnd(true);
		return repository.save(userId, sub);
	}

	/**
	 * Get all available tiers with their details.
	 * @return List of all tiers
	 */
	public List<TierInfo> getAllTiers() {
		return Arrays.stream(SubscriptionTier.values())
				.map(tier -> new TierInfo(tier.getId(), tier.getDisplayName(), tier.getPriceInCents(),
						tier.getDescription(), tier.getAllowedModels(), tier.getDailyMessageLimit()))
				.collect(Collectors.toList());
	}

	/**
	 * DTO for tier information.
	 */
	public record TierInfo(String id, String name, int priceInCents, String description, List<String> allowedModels,
			int dailyMessageLimit) {
	}

	/**
	 * Check if user has ADMIN role.
	 * @param userId The user ID
	 * @return true if user is admin
	 */
	private boolean isAdmin(String userId) {
		Optional<UserEntity> userOpt = userRepository.findById(userId);
		if (userOpt.isEmpty()) {
			return false;
		}

		UserEntity user = userOpt.get();
		String role = user.getProfile() != null ? user.getProfile().getRole() : null;
		return ADMIN_ROLE.equals(role);
	}

}
