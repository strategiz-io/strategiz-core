package io.strategiz.service.profile.controller;

import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import io.strategiz.business.preferences.service.SubscriptionService;
import io.strategiz.business.preferences.service.SubscriptionService.TierInfo;
import io.strategiz.client.stripe.StripeService;
import io.strategiz.client.stripe.StripeService.CheckoutResult;
import io.strategiz.client.stripe.StripeService.SubscriptionUpdate;
import io.strategiz.data.preferences.entity.SubscriptionTier;
import io.strategiz.data.preferences.entity.UserSubscription;
import io.strategiz.framework.authorization.annotation.AuthUser;
import io.strategiz.framework.authorization.annotation.RequireAuth;
import io.strategiz.framework.authorization.context.AuthenticatedUser;
import io.strategiz.service.base.constants.ModuleConstants;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.profile.model.CheckoutRequest;
import io.strategiz.service.profile.model.SubscriptionResponse;
import io.strategiz.service.profile.model.UsageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
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

	private final StripeService stripeService;

	public SubscriptionController(SubscriptionService subscriptionService, StripeService stripeService) {
		this.subscriptionService = subscriptionService;
		this.stripeService = stripeService;
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

	// ==================== Stripe Checkout Endpoints ====================

	/**
	 * Get Stripe configuration for frontend.
	 * @return Stripe publishable key and configuration
	 */
	@GetMapping("/config")
	public ResponseEntity<Map<String, Object>> getStripeConfig() {
		return ResponseEntity.ok(Map.of("publishableKey", stripeService.getPublishableKey(), "enabled",
				stripeService.isConfigured()));
	}

	/**
	 * Create a Stripe checkout session for subscription upgrade.
	 * @param request The checkout request with tier ID
	 * @param user The authenticated user
	 * @return Checkout session URL
	 */
	@PostMapping("/checkout")
	@RequireAuth(minAcr = "1")
	public ResponseEntity<?> createCheckoutSession(@RequestBody CheckoutRequest request,
			@AuthUser AuthenticatedUser user) {

		if (!stripeService.isConfigured()) {
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(Map.of("error", "Payment processing is not available"));
		}

		String userId = user.getUserId();
		String tierId = request.getTierId();

		// Validate tier
		if (!"trader".equals(tierId) && !"strategist".equals(tierId)) {
			return ResponseEntity.badRequest().body(Map.of("error", "Invalid tier: " + tierId));
		}

		// Check if already on this tier or higher
		UserSubscription currentSub = subscriptionService.getSubscription(userId);
		if (tierId.equals(currentSub.getTier())) {
			return ResponseEntity.badRequest().body(Map.of("error", "Already on this tier"));
		}

		try {
			CheckoutResult result = stripeService.createCheckoutSession(userId, user.getEmail(), tierId,
					currentSub.getStripeCustomerId());

			logger.info("Created checkout session for user {} tier {}", userId, tierId);

			return ResponseEntity.ok(Map.of("sessionId", result.sessionId(), "url", result.url()));
		}
		catch (StripeException e) {
			logger.error("Stripe error creating checkout session: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("error", "Failed to create checkout session"));
		}
		catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}

	/**
	 * Handle Stripe webhook events.
	 * @param payload The raw request body
	 * @param signature The Stripe-Signature header
	 * @return Acknowledgement
	 */
	@PostMapping("/webhook")
	public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload,
			@RequestHeader("Stripe-Signature") String signature) {

		logger.debug("Received Stripe webhook");

		// Verify webhook signature
		Event event = stripeService.verifyWebhookSignature(payload, signature);
		if (event == null) {
			logger.warn("Webhook signature verification failed");
			return ResponseEntity.badRequest().body("Invalid signature");
		}

		// Process the event
		SubscriptionUpdate update = stripeService.handleWebhookEvent(event);

		if (update != null && update.userId() != null) {
			// Update user subscription in Firestore
			UserSubscription subscription = subscriptionService.getSubscription(update.userId());

			if (update.tier() != null) {
				subscription.setTier(update.tier());
			}
			if (update.status() != null) {
				subscription.setStatus(update.status());
			}
			if (update.stripeCustomerId() != null) {
				subscription.setStripeCustomerId(update.stripeCustomerId());
			}
			if (update.stripeSubscriptionId() != null) {
				subscription.setStripeSubscriptionId(update.stripeSubscriptionId());
			}
			if (update.currentPeriodStart() != null) {
				subscription.setCurrentPeriodStart(update.currentPeriodStart());
			}
			if (update.currentPeriodEnd() != null) {
				subscription.setCurrentPeriodEnd(update.currentPeriodEnd());
			}
			if (update.cancelAtPeriodEnd() != null) {
				subscription.setCancelAtPeriodEnd(update.cancelAtPeriodEnd());
			}

			subscriptionService.updateSubscription(update.userId(), subscription);
			logger.info("Updated subscription for user {} from webhook", update.userId());
		}

		return ResponseEntity.ok("Received");
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
