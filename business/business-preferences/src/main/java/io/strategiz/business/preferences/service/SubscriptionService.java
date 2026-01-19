package io.strategiz.business.preferences.service;

import io.strategiz.data.preferences.entity.PlatformSubscription;
import io.strategiz.data.preferences.entity.SubscriptionTier;
import io.strategiz.data.preferences.repository.SubscriptionRepository;
import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.data.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

	/**
	 * Minimum tier level required for Historical Market Insights (Feeling Lucky).
	 * Level 1 = STRATEGIST tier and above (STRATEGIST, QUANT)
	 * Tier levels: EXPLORER=0, STRATEGIST=1, QUANT=2
	 */
	private static final int MIN_TIER_LEVEL_HISTORICAL_INSIGHTS = 1;

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
	public PlatformSubscription getSubscription(String userId) {
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
	 * Check if user can use Historical Market Insights (Feeling Lucky mode).
	 * Analyzes 7 years of historical data to generate optimized strategies.
	 * Historical Market Insights requires tier level 1 or higher (EXPLORER+).
	 * ADMIN users have access for testing purposes.
	 * @param userId The user ID
	 * @return true if user can use Historical Market Insights
	 */
	public boolean canUseHistoricalInsights(String userId) {
		// Admins can use Historical Market Insights for testing
		if (isAdmin(userId)) {
			logger.debug("Admin user {} granted access to Historical Market Insights", userId);
			return true;
		}

		SubscriptionTier tier = getTier(userId);
		boolean canUse = tier.meetsMinimumLevel(MIN_TIER_LEVEL_HISTORICAL_INSIGHTS);

		if (!canUse) {
			logger.info("Historical Market Insights not available for user {} on tier {} (level {}). Requires level {} or higher.",
					userId, tier.getId(), tier.getLevel(), MIN_TIER_LEVEL_HISTORICAL_INSIGHTS);
		}

		return canUse;
	}

	/**
	 * Check if user has credits available.
	 * ADMIN users bypass all limits for testing purposes.
	 * @param userId The user ID
	 * @return true if user has remaining credits or is admin
	 */
	public boolean hasCreditsAvailable(String userId) {
		// Admins bypass all subscription limits for testing
		if (isAdmin(userId)) {
			logger.debug("Admin user {} bypassing subscription limits", userId);
			return true;
		}

		PlatformSubscription sub = getSubscription(userId);

		// Check if trial expired
		if (sub.isTrialExpired()) {
			return false;
		}

		// Check if blocked due to usage
		if (sub.isBlocked()) {
			return false;
		}

		return sub.getRemainingCredits() > 0;
	}

	/**
	 * Get remaining credits for the current billing period.
	 * @param userId The user ID
	 * @return Remaining credits
	 */
	public int getRemainingCredits(String userId) {
		PlatformSubscription sub = getSubscription(userId);
		return sub.getRemainingCredits();
	}

	/**
	 * Get usage percentage for the current billing period (0-100+).
	 * @param userId The user ID
	 * @return Usage percentage
	 */
	public int getUsagePercentage(String userId) {
		PlatformSubscription sub = getSubscription(userId);
		return sub.getUsagePercentage();
	}

	/**
	 * Get current usage warning level.
	 * @param userId The user ID
	 * @return Warning level (none, warning, critical, blocked)
	 */
	public String getUsageWarningLevel(String userId) {
		PlatformSubscription sub = getSubscription(userId);
		return sub.getUsageWarningLevel();
	}

	/**
	 * Check if user can send a message (has credits available).
	 * Covers both Learn AI Chat and Labs Strategy Generation.
	 * ADMIN users bypass all limits for testing purposes.
	 * @param userId The user ID
	 * @return true if has credits or user is admin
	 * @deprecated Use {@link #hasCreditsAvailable(String)} instead
	 */
	@Deprecated(forRemoval = true)
	public boolean canSendMessage(String userId) {
		return hasCreditsAvailable(userId);
	}

	/**
	 * Record a message sent by the user (Learn chat or Labs strategy generation).
	 * @param userId The user ID
	 * @return The updated subscription
	 * @deprecated Use TokenUsageService.recordUsage() for accurate credit tracking
	 */
	@Deprecated(forRemoval = true)
	public PlatformSubscription recordMessageUsage(String userId) {
		logger.debug("Recording AI chat message usage for user {} (deprecated method)", userId);
		return repository.incrementMessageUsage(userId);
	}

	/**
	 * Get remaining messages for the day.
	 * @param userId The user ID
	 * @return Remaining messages (approximate based on credits)
	 * @deprecated Use {@link #getRemainingCredits(String)} instead
	 */
	@Deprecated(forRemoval = true)
	public int getRemainingMessages(String userId) {
		// Approximate: each "message" is roughly 1000 tokens at baseline rate
		int remainingCredits = getRemainingCredits(userId);
		return remainingCredits > 0 ? remainingCredits / 10 : 0;
	}

	/**
	 * Update subscription (for Stripe webhook handling).
	 * @param userId The user ID
	 * @param subscription The updated subscription
	 * @return The saved subscription
	 */
	public PlatformSubscription updateSubscription(String userId, PlatformSubscription subscription) {
		logger.info("Updating subscription for user {} to tier {}", userId, subscription.getTier());
		return repository.save(userId, subscription);
	}

	/**
	 * Upgrade user to a new tier.
	 * @param userId The user ID
	 * @param newTier The new tier
	 * @return The updated subscription
	 */
	public PlatformSubscription upgradeTier(String userId, SubscriptionTier newTier) {
		logger.info("Upgrading user {} to tier {}", userId, newTier.getId());
		return repository.updateTier(userId, newTier);
	}

	/**
	 * Cancel subscription (set to cancel at period end).
	 * @param userId The user ID
	 * @return The updated subscription
	 */
	public PlatformSubscription cancelSubscription(String userId) {
		logger.info("Canceling subscription for user {}", userId);
		PlatformSubscription sub = getSubscription(userId);
		sub.setCancelAtPeriodEnd(true);
		return repository.save(userId, sub);
	}

	/**
	 * Initialize a new user with the free Explorer tier.
	 * @param userId The user ID
	 * @return The initialized subscription
	 * @deprecated Trial tier has been removed. Use {@link #initializeExplorer(String)} instead.
	 */
	@Deprecated(forRemoval = true)
	public PlatformSubscription initializeTrial(String userId) {
		return initializeExplorer(userId);
	}

	/**
	 * Initialize a new user with the free Explorer tier.
	 * @param userId The user ID
	 * @return The initialized subscription
	 */
	public PlatformSubscription initializeExplorer(String userId) {
		logger.info("Initializing Explorer (free) tier for user {}", userId);

		PlatformSubscription sub = getSubscription(userId);
		sub.initializeForTier(SubscriptionTier.EXPLORER);
		sub.setStatus("active");

		return repository.save(userId, sub);
	}

	/**
	 * Check if user's trial is expired.
	 * @param userId The user ID
	 * @return Always returns false - trial tier has been removed
	 * @deprecated Trial tier has been removed. All users start on Explorer (free).
	 */
	@Deprecated(forRemoval = true)
	public boolean isTrialExpired(String userId) {
		return false; // Trial tier removed - no trial expiration
	}

	/**
	 * Get days remaining in trial.
	 * @param userId The user ID
	 * @return Always returns -1 - trial tier has been removed
	 * @deprecated Trial tier has been removed. All users start on Explorer (free).
	 */
	@Deprecated(forRemoval = true)
	public int getTrialDaysRemaining(String userId) {
		return -1; // Trial tier removed
	}

	/**
	 * Expire a user's trial (called by scheduled job or webhook).
	 * @param userId The user ID
	 * @return The subscription (unchanged)
	 * @deprecated Trial tier has been removed. All users start on Explorer (free).
	 */
	@Deprecated(forRemoval = true)
	public PlatformSubscription expireTrial(String userId) {
		logger.info("expireTrial called but trial tier no longer exists - no action taken");
		return getSubscription(userId);
	}

	/**
	 * Upgrade user to a paid tier (resets credits and activates subscription).
	 * @param userId The user ID
	 * @param newTier The new tier
	 * @param stripeCustomerId The Stripe customer ID
	 * @param stripeSubscriptionId The Stripe subscription ID
	 * @return The updated subscription
	 */
	public PlatformSubscription upgradeToPaidTier(String userId, SubscriptionTier newTier, String stripeCustomerId,
			String stripeSubscriptionId) {
		logger.info("Upgrading user {} to paid tier {}", userId, newTier.getId());

		PlatformSubscription sub = getSubscription(userId);
		sub.initializeForTier(newTier);
		sub.setStripeCustomerId(stripeCustomerId);
		sub.setStripeSubscriptionId(stripeSubscriptionId);
		sub.setStatus("active");

		// Clear trial dates
		sub.setTrialStartDate(null);
		sub.setTrialEndDate(null);

		return repository.save(userId, sub);
	}

	/**
	 * Get all available tiers with their details.
	 * @return List of all tiers (excluding TRIAL)
	 */
	public List<TierInfo> getAllTiers() {
		return Arrays.stream(SubscriptionTier.values())
				.filter(tier -> !tier.isTrial())
				.map(tier -> new TierInfo(tier.getId(), tier.getDisplayName(), tier.getPriceInCents(),
						tier.getDescription(), tier.getAllowedModels(), tier.getMonthlyCredits(),
						tier.getLevel()))
				.collect(Collectors.toList());
	}

	/**
	 * Get subscription summary for a user (useful for UI).
	 * @param userId The user ID
	 * @return Subscription summary with usage details
	 */
	public SubscriptionSummary getSubscriptionSummary(String userId) {
		PlatformSubscription sub = getSubscription(userId);
		SubscriptionTier tier = sub.getTierEnum();

		return new SubscriptionSummary(sub.getTier(), tier.getDisplayName(), sub.getStatus(),
				sub.getMonthlyCreditsAllowed(), sub.getMonthlyCreditsUsed(), sub.getRemainingCredits(),
				sub.getUsagePercentage(), sub.getUsageWarningLevel(), tier.getAllowedModels(),
				sub.isTrial() ? getTrialDaysRemaining(userId) : -1, sub.getCancelAtPeriodEnd());
	}

	/**
	 * DTO for tier information.
	 */
	public record TierInfo(String id, String name, int priceInCents, String description, List<String> allowedModels,
			int monthlyCredits, int level) {
	}

	/**
	 * DTO for subscription summary.
	 */
	public record SubscriptionSummary(String tierId, String tierName, String status, int totalCredits, int usedCredits,
			int remainingCredits, int usagePercentage, String warningLevel, List<String> allowedModels,
			int trialDaysRemaining, boolean cancelAtPeriodEnd) {
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
