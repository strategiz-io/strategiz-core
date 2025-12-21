package io.strategiz.service.profile.controller;

import io.strategiz.business.preferences.service.SubscriptionService;
import io.strategiz.business.preferences.service.SubscriptionService.TierInfo;
import io.strategiz.data.preferences.entity.SubscriptionTier;
import io.strategiz.data.preferences.entity.UserSubscription;
import io.strategiz.framework.authorization.annotation.AuthUser;
import io.strategiz.framework.authorization.annotation.RequireAuth;
import io.strategiz.framework.authorization.context.AuthenticatedUser;
import io.strategiz.service.base.constants.ModuleConstants;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.profile.model.SubscriptionResponse;
import io.strategiz.service.profile.model.UsageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for subscription management. Provides endpoints for viewing subscription
 * status, available tiers, and usage information.
 */
@RestController
@RequestMapping("/v1/subscription")
@Validated
public class SubscriptionController extends BaseController {

	private static final Logger logger = LoggerFactory.getLogger(SubscriptionController.class);

	private final SubscriptionService subscriptionService;

	public SubscriptionController(SubscriptionService subscriptionService) {
		this.subscriptionService = subscriptionService;
	}

	@Override
	protected String getModuleName() {
		return ModuleConstants.PROFILE_MODULE;
	}

	/**
	 * Get current user's subscription.
	 * @param user The authenticated user
	 * @return The subscription details
	 */
	@GetMapping
	@RequireAuth(minAcr = "1")
	public ResponseEntity<SubscriptionResponse> getSubscription(@AuthUser AuthenticatedUser user) {
		String userId = user.getUserId();
		logger.info("Getting subscription for user {}", userId);

		UserSubscription sub = subscriptionService.getSubscription(userId);
		SubscriptionTier tier = sub.getTierEnum();

		SubscriptionResponse response = new SubscriptionResponse();
		response.setTier(tier.getId());
		response.setTierName(tier.getDisplayName());
		response.setStatus(sub.getStatus());
		response.setPriceInCents(tier.getPriceInCents());
		response.setAllowedModels(tier.getAllowedModels());
		response.setDailyMessageLimit(tier.getDailyMessageLimit());
		response.setDailyStrategyLimit(tier.getDailyStrategyLimit());
		response.setDailyMessagesUsed(sub.getDailyMessagesUsed());
		response.setDailyStrategiesUsed(sub.getDailyStrategiesUsed());
		response.setCancelAtPeriodEnd(sub.getCancelAtPeriodEnd());

		return ResponseEntity.ok(response);
	}

	/**
	 * Get usage information for current user.
	 * @param user The authenticated user
	 * @return Usage details
	 */
	@GetMapping("/usage")
	@RequireAuth(minAcr = "1")
	public ResponseEntity<UsageResponse> getUsage(@AuthUser AuthenticatedUser user) {
		String userId = user.getUserId();

		UserSubscription sub = subscriptionService.getSubscription(userId);
		SubscriptionTier tier = sub.getTierEnum();

		UsageResponse response = new UsageResponse();
		response.setMessagesUsed(sub.getDailyMessagesUsed());
		response.setMessagesLimit(tier.getDailyMessageLimit());
		response.setMessagesRemaining(subscriptionService.getRemainingMessages(userId));
		response.setStrategiesUsed(sub.getDailyStrategiesUsed());
		response.setStrategiesLimit(tier.getDailyStrategyLimit());
		response.setStrategiesRemaining(subscriptionService.getRemainingStrategies(userId));
		response.setResetDate(sub.getUsageResetDate());

		return ResponseEntity.ok(response);
	}

	/**
	 * Get all available subscription tiers.
	 * @return List of all tiers
	 */
	@GetMapping("/tiers")
	public ResponseEntity<List<TierInfo>> getTiers() {
		return ResponseEntity.ok(subscriptionService.getAllTiers());
	}

	/**
	 * Check if a specific model is allowed for the user.
	 * @param modelId The model to check
	 * @param user The authenticated user
	 * @return Whether the model is allowed
	 */
	@GetMapping("/check-model/{modelId}")
	@RequireAuth(minAcr = "1")
	public ResponseEntity<Map<String, Object>> checkModelAccess(@PathVariable String modelId,
			@AuthUser AuthenticatedUser user) {
		String userId = user.getUserId();
		boolean allowed = subscriptionService.isModelAllowed(userId, modelId);
		SubscriptionTier tier = subscriptionService.getTier(userId);

		return ResponseEntity.ok(Map.of("allowed", allowed, "model", modelId, "currentTier", tier.getId(),
				"requiredTier", getRequiredTierForModel(modelId)));
	}

	/**
	 * Check if user can send a message.
	 * @param user The authenticated user
	 * @return Whether the user can send a message
	 */
	@GetMapping("/can-message")
	@RequireAuth(minAcr = "1")
	public ResponseEntity<Map<String, Object>> canSendMessage(@AuthUser AuthenticatedUser user) {
		String userId = user.getUserId();
		boolean canSend = subscriptionService.canSendMessage(userId);
		int remaining = subscriptionService.getRemainingMessages(userId);

		return ResponseEntity.ok(Map.of("allowed", canSend, "remaining", remaining));
	}

	/**
	 * Check if user can generate a strategy.
	 * @param user The authenticated user
	 * @return Whether the user can generate a strategy
	 */
	@GetMapping("/can-generate")
	@RequireAuth(minAcr = "1")
	public ResponseEntity<Map<String, Object>> canGenerateStrategy(@AuthUser AuthenticatedUser user) {
		String userId = user.getUserId();
		boolean canGenerate = subscriptionService.canGenerateStrategy(userId);
		int remaining = subscriptionService.getRemainingStrategies(userId);

		return ResponseEntity.ok(Map.of("allowed", canGenerate, "remaining", remaining));
	}

	/**
	 * Get allowed models for current user.
	 * @param user The authenticated user
	 * @return List of allowed model IDs
	 */
	@GetMapping("/allowed-models")
	@RequireAuth(minAcr = "1")
	public ResponseEntity<Map<String, Object>> getAllowedModels(@AuthUser AuthenticatedUser user) {
		String userId = user.getUserId();
		List<String> models = subscriptionService.getAllowedModels(userId);
		SubscriptionTier tier = subscriptionService.getTier(userId);

		return ResponseEntity.ok(Map.of("models", models, "tier", tier.getId()));
	}

	// Helper method to determine minimum tier for a model
	private String getRequiredTierForModel(String modelId) {
		for (SubscriptionTier tier : SubscriptionTier.values()) {
			if (tier.isModelAllowed(modelId)) {
				return tier.getId();
			}
		}
		return "strategist"; // Default to highest tier
	}

}
