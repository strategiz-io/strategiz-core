package io.strategiz.business.preferences.service;

import io.strategiz.business.preferences.exception.PreferencesErrorDetails;
import io.strategiz.data.preferences.entity.PlatformSubscription;
import io.strategiz.data.preferences.entity.SubscriptionTier;
import io.strategiz.data.preferences.entity.TokenUsageRecord;
import io.strategiz.data.preferences.repository.SubscriptionRepository;
import io.strategiz.data.preferences.repository.TokenUsageRepository;
import io.strategiz.framework.exception.StrategizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

/**
 * Service for tracking token/credit usage across AI operations.
 * Handles credit consumption, usage warnings, and limit enforcement.
 *
 * <p>Credit System:</p>
 * <ul>
 *   <li>1 credit = $0.001 in AI costs</li>
 *   <li>Models have different weights (Gemini Flash = 1x, Claude Opus = 115x)</li>
 *   <li>Usage warnings at 80%, critical at 95%, blocked at 100%</li>
 * </ul>
 */
@Service
public class TokenUsageService {

	private static final Logger logger = LoggerFactory.getLogger(TokenUsageService.class);

	private static final String MODULE_NAME = "business-preferences";

	// Warning thresholds as percentages
	private static final int WARNING_THRESHOLD = 80;
	private static final int CRITICAL_THRESHOLD = 95;
	private static final int BLOCKED_THRESHOLD = 100;

	private final TokenUsageRepository tokenUsageRepository;

	private final SubscriptionRepository subscriptionRepository;

	public TokenUsageService(TokenUsageRepository tokenUsageRepository, SubscriptionRepository subscriptionRepository) {
		this.tokenUsageRepository = tokenUsageRepository;
		this.subscriptionRepository = subscriptionRepository;
	}

	/**
	 * Record token usage for an AI API call.
	 * Updates user's credit balance and creates audit record.
	 * @param userId The user ID
	 * @param modelId The AI model used
	 * @param promptTokens Number of input tokens
	 * @param completionTokens Number of output tokens
	 * @param requestType Type of request (chat, strategy, historical_insights, etc.)
	 * @return The usage record with credits consumed
	 */
	public TokenUsageRecord recordUsage(String userId, String modelId, int promptTokens, int completionTokens,
			String requestType) {
		return recordUsage(userId, modelId, promptTokens, completionTokens, requestType, null);
	}

	/**
	 * Record token usage with session ID for grouping.
	 * @param userId The user ID
	 * @param modelId The AI model used
	 * @param promptTokens Number of input tokens
	 * @param completionTokens Number of output tokens
	 * @param requestType Type of request
	 * @param sessionId Optional session ID for grouping related requests
	 * @return The usage record with credits consumed
	 */
	public TokenUsageRecord recordUsage(String userId, String modelId, int promptTokens, int completionTokens,
			String requestType, String sessionId) {
		logger.debug("Recording usage for user {}: model={}, prompt={}, completion={}, type={}",
				userId, modelId, promptTokens, completionTokens, requestType);

		// Get current subscription
		PlatformSubscription subscription = subscriptionRepository.getByUserId(userId);

		// Calculate credits for this usage
		int creditsConsumed = SubscriptionTier.calculateCredits(modelId, promptTokens, completionTokens);

		// Create usage record
		TokenUsageRecord record = TokenUsageRecord.create(userId, modelId, promptTokens, completionTokens, requestType,
				sessionId, subscription.getCreditResetDate());

		// Update subscription credits
		subscription.setMonthlyCreditsUsed(subscription.getMonthlyCreditsUsed() + creditsConsumed);
		subscription.updateWarningLevel();
		subscriptionRepository.save(userId, subscription);

		// Save usage record for audit trail
		tokenUsageRepository.save(userId, record);

		logger.info("User {} consumed {} credits ({} model, {} tokens). Remaining: {}/{}",
				userId, creditsConsumed, modelId, promptTokens + completionTokens,
				subscription.getRemainingCredits(), subscription.getMonthlyCreditsAllowed());

		return record;
	}

	/**
	 * Calculate credits that would be consumed for a request.
	 * Use this to pre-check before making expensive API calls.
	 * @param modelId The AI model
	 * @param estimatedPromptTokens Estimated input tokens
	 * @param estimatedCompletionTokens Estimated output tokens
	 * @return Estimated credits to consume
	 */
	public int calculateCredits(String modelId, int estimatedPromptTokens, int estimatedCompletionTokens) {
		return SubscriptionTier.calculateCredits(modelId, estimatedPromptTokens, estimatedCompletionTokens);
	}

	/**
	 * Get remaining credits for a user.
	 * @param userId The user ID
	 * @return Remaining credits
	 */
	public int getRemainingCredits(String userId) {
		PlatformSubscription subscription = subscriptionRepository.getByUserId(userId);
		return subscription.getRemainingCredits();
	}

	/**
	 * Get current usage percentage (0-100+).
	 * @param userId The user ID
	 * @return Usage percentage
	 */
	public int getUsagePercentage(String userId) {
		PlatformSubscription subscription = subscriptionRepository.getByUserId(userId);
		return subscription.getUsagePercentage();
	}

	/**
	 * Check usage status and return warning level if applicable.
	 * @param userId The user ID
	 * @return UsageStatus with level and message
	 */
	public UsageStatus checkUsageStatus(String userId) {
		PlatformSubscription subscription = subscriptionRepository.getByUserId(userId);
		int percentage = subscription.getUsagePercentage();
		int remaining = subscription.getRemainingCredits();

		if (percentage >= BLOCKED_THRESHOLD) {
			return new UsageStatus("blocked", percentage, remaining,
					"You've used all your credits for this billing period. Please upgrade to continue.");
		}
		else if (percentage >= CRITICAL_THRESHOLD) {
			return new UsageStatus("critical", percentage, remaining,
					"Warning: You've used " + percentage + "% of your monthly credits. Consider upgrading soon.");
		}
		else if (percentage >= WARNING_THRESHOLD) {
			return new UsageStatus("warning", percentage, remaining,
					"You've used " + percentage + "% of your monthly credits.");
		}
		else {
			return new UsageStatus("none", percentage, remaining, null);
		}
	}

	/**
	 * Check if user can consume the specified credits.
	 * Throws exception if insufficient credits.
	 * @param userId The user ID
	 * @param creditsNeeded Credits needed for the operation
	 * @throws StrategizException if insufficient credits
	 */
	public void checkCreditsAvailable(String userId, int creditsNeeded) {
		PlatformSubscription subscription = subscriptionRepository.getByUserId(userId);

		// Check if trial is expired
		if (subscription.isTrialExpired()) {
			throw new StrategizException(PreferencesErrorDetails.TRIAL_EXPIRED, MODULE_NAME,
					"Your trial has expired. Please subscribe to continue using the platform.");
		}

		// Check if blocked
		if (subscription.isBlocked()) {
			throw new StrategizException(PreferencesErrorDetails.CREDITS_EXHAUSTED, MODULE_NAME,
					"You've used all your credits for this billing period. Please upgrade to continue.");
		}

		// Check if enough credits available
		if (subscription.getRemainingCredits() < creditsNeeded) {
			throw new StrategizException(PreferencesErrorDetails.INSUFFICIENT_CREDITS, MODULE_NAME,
					"Insufficient credits. You have " + subscription.getRemainingCredits()
							+ " credits but need " + creditsNeeded + ".");
		}
	}

	/**
	 * Check if user can use a specific model (tier check + credits check).
	 * @param userId The user ID
	 * @param modelId The model to check
	 * @param estimatedTokens Estimated total tokens for the request
	 * @return true if user can use the model with available credits
	 */
	public boolean canUseModel(String userId, String modelId, int estimatedTokens) {
		PlatformSubscription subscription = subscriptionRepository.getByUserId(userId);
		SubscriptionTier tier = subscription.getTierEnum();

		// Check if model is allowed for tier
		if (!tier.isModelAllowed(modelId)) {
			logger.debug("Model {} not allowed for user {} on tier {}", modelId, userId, tier.getId());
			return false;
		}

		// Estimate credits (assuming 70/30 input/output split)
		int estimatedPrompt = (int) (estimatedTokens * 0.7);
		int estimatedCompletion = estimatedTokens - estimatedPrompt;
		int creditsNeeded = calculateCredits(modelId, estimatedPrompt, estimatedCompletion);

		// Check if enough credits
		return subscription.getRemainingCredits() >= creditsNeeded;
	}

	/**
	 * Get usage analytics for a user in the current billing period.
	 * @param userId The user ID
	 * @return UsageAnalytics with breakdown by model and type
	 */
	public UsageAnalytics getUsageAnalytics(String userId) {
		PlatformSubscription subscription = subscriptionRepository.getByUserId(userId);
		Instant periodStart = subscription.getCreditResetDate() != null ? subscription.getCreditResetDate()
				: Instant.now().minusSeconds(30 * 24 * 60 * 60);

		Map<String, Integer> creditsByModel = tokenUsageRepository.getCreditsByModel(userId, periodStart);
		Map<String, Integer> creditsByType = tokenUsageRepository.getCreditsByRequestType(userId, periodStart);

		return new UsageAnalytics(userId, subscription.getMonthlyCreditsAllowed(), subscription.getMonthlyCreditsUsed(),
				subscription.getRemainingCredits(), subscription.getUsagePercentage(),
				subscription.getUsageWarningLevel(), creditsByModel, creditsByType);
	}

	/**
	 * Reset credits for a new billing period.
	 * Called by webhook handler when subscription renews.
	 * @param userId The user ID
	 * @return Updated subscription
	 */
	public PlatformSubscription resetCredits(String userId) {
		logger.info("Resetting credits for user {} - new billing period", userId);

		PlatformSubscription subscription = subscriptionRepository.getByUserId(userId);
		subscription.resetCredits();

		return subscriptionRepository.save(userId, subscription);
	}

	/**
	 * Initialize credits for a new subscriber or tier change.
	 * @param userId The user ID
	 * @param tier The subscription tier
	 * @return Updated subscription
	 */
	public PlatformSubscription initializeCredits(String userId, SubscriptionTier tier) {
		logger.info("Initializing credits for user {} to tier {}", userId, tier.getId());

		PlatformSubscription subscription = subscriptionRepository.getByUserId(userId);
		subscription.initializeForTier(tier);

		return subscriptionRepository.save(userId, subscription);
	}

	/**
	 * Usage status record.
	 */
	public record UsageStatus(String level, int usagePercentage, int remainingCredits, String message) {

		public boolean isBlocked() {
			return "blocked".equals(level);
		}

		public boolean hasWarning() {
			return "warning".equals(level) || "critical".equals(level);
		}

	}

	/**
	 * Usage analytics for a billing period.
	 */
	public record UsageAnalytics(String userId, int totalCredits, int usedCredits, int remainingCredits,
			int usagePercentage, String warningLevel, Map<String, Integer> creditsByModel,
			Map<String, Integer> creditsByRequestType) {
	}

}
