package io.strategiz.service.profile.controller;

import io.strategiz.framework.authorization.annotation.AuthUser;
import io.strategiz.framework.authorization.annotation.RequireAuth;
import io.strategiz.framework.authorization.context.AuthenticatedUser;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.profile.model.SubscribeToOwnerRequest;
import io.strategiz.service.profile.model.SubscriberResponse;
import io.strategiz.service.profile.model.StrategyAccessCheckResponse;
import io.strategiz.service.profile.model.UserSubscriptionResponse;
import io.strategiz.service.profile.service.UserSubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * Controller for user subscription management. Handles subscribing to strategy owners and
 * managing subscriptions.
 *
 * API Endpoints: - POST /v1/user-subscriptions/subscribe/{ownerId} - Create subscription
 * checkout - POST /v1/user-subscriptions/{id}/cancel - Cancel subscription - GET
 * /v1/user-subscriptions/mine - Get my subscriptions - GET
 * /v1/user-subscriptions/check-access/{ownerId} - Check access to owner's strategies -
 * GET /v1/owner-subscriptions/subscribers - Get my subscribers (owner view)
 */
@RestController
@RequestMapping("/v1")
@Validated
public class UserSubscriptionController extends BaseController {

	private static final Logger logger = LoggerFactory.getLogger(UserSubscriptionController.class);

	private final UserSubscriptionService userSubscriptionService;

	public UserSubscriptionController(UserSubscriptionService userSubscriptionService) {
		this.userSubscriptionService = userSubscriptionService;
	}

	@Override
	protected String getModuleName() {
		return "service-profile";
	}

	/**
	 * Create a checkout session to subscribe to an owner.
	 * @param ownerId The owner to subscribe to
	 * @param request Optional request with Stripe customer ID
	 * @param user The authenticated user
	 * @return Checkout URL and session details
	 */
	@PostMapping("/user-subscriptions/subscribe/{ownerId}")
	@RequireAuth(minAcr = "1")
	public ResponseEntity<Map<String, Object>> subscribeToOwner(@PathVariable String ownerId,
			@Valid @RequestBody(required = false) SubscribeToOwnerRequest request, @AuthUser AuthenticatedUser user) {

		String userId = user.getUserId();
		logger.info("Creating subscription checkout: subscriber={}, owner={}", userId, ownerId);

		String stripeCustomerId = request != null ? request.getStripeCustomerId() : null;
		Map<String, Object> checkout = userSubscriptionService.createSubscriptionCheckout(userId, ownerId,
				stripeCustomerId);

		return ResponseEntity.ok(checkout);
	}

	/**
	 * Cancel a subscription.
	 * @param subscriptionId The subscription to cancel
	 * @param request Optional request with cancellation reason
	 * @param user The authenticated user
	 * @return The cancelled subscription
	 */
	@PostMapping("/user-subscriptions/{subscriptionId}/cancel")
	@RequireAuth(minAcr = "1")
	public ResponseEntity<UserSubscriptionResponse> cancelSubscription(@PathVariable String subscriptionId,
			@RequestBody(required = false) Map<String, String> request, @AuthUser AuthenticatedUser user) {

		String userId = user.getUserId();
		String reason = request != null ? request.get("reason") : null;
		logger.info("Cancelling subscription {} by user {}", subscriptionId, userId);

		UserSubscriptionResponse cancelled = userSubscriptionService.cancelSubscription(subscriptionId, userId, reason);

		return ResponseEntity.ok(cancelled);
	}

	/**
	 * Get my subscriptions (what I'm subscribed to).
	 * @param user The authenticated user
	 * @return List of subscriptions
	 */
	@GetMapping("/user-subscriptions/mine")
	@RequireAuth(minAcr = "1")
	public ResponseEntity<List<UserSubscriptionResponse>> getMySubscriptions(@AuthUser AuthenticatedUser user) {

		String userId = user.getUserId();
		logger.debug("Getting subscriptions for user {}", userId);

		List<UserSubscriptionResponse> subscriptions = userSubscriptionService.getMySubscriptions(userId);

		return ResponseEntity.ok(subscriptions);
	}

	/**
	 * Get a specific subscription by ID.
	 * @param subscriptionId The subscription ID
	 * @param user The authenticated user
	 * @return The subscription
	 */
	@GetMapping("/user-subscriptions/{subscriptionId}")
	@RequireAuth(minAcr = "1")
	public ResponseEntity<UserSubscriptionResponse> getSubscription(@PathVariable String subscriptionId,
			@AuthUser AuthenticatedUser user) {

		String userId = user.getUserId();
		logger.debug("Getting subscription {} for user {}", subscriptionId, userId);

		// Get the subscription from the user's subscriptions
		List<UserSubscriptionResponse> subscriptions = userSubscriptionService.getMySubscriptions(userId);
		UserSubscriptionResponse subscription = subscriptions.stream()
			.filter(s -> s.getId().equals(subscriptionId))
			.findFirst()
			.orElse(null);

		if (subscription == null) {
			return ResponseEntity.notFound().build();
		}

		return ResponseEntity.ok(subscription);
	}

	/**
	 * Check if current user has access to an owner's strategies.
	 * @param ownerId The owner's user ID
	 * @param user The authenticated user
	 * @return Access check result
	 */
	@GetMapping("/user-subscriptions/check-access/{ownerId}")
	@RequireAuth(minAcr = "1")
	public ResponseEntity<StrategyAccessCheckResponse> checkAccessToOwner(@PathVariable String ownerId,
			@AuthUser AuthenticatedUser user) {

		String userId = user.getUserId();
		logger.debug("Checking access for user {} to owner {}", userId, ownerId);

		StrategyAccessCheckResponse accessCheck = userSubscriptionService.checkAccess(userId, ownerId);

		return ResponseEntity.ok(accessCheck);
	}

	/**
	 * Get my subscribers (as an owner).
	 * @param user The authenticated user (owner)
	 * @return List of subscribers
	 */
	@GetMapping("/owner-subscriptions/subscribers")
	@RequireAuth(minAcr = "1")
	public ResponseEntity<List<SubscriberResponse>> getMySubscribers(@AuthUser AuthenticatedUser user) {

		String userId = user.getUserId();
		logger.debug("Getting subscribers for owner {}", userId);

		List<SubscriberResponse> subscribers = userSubscriptionService.getMySubscribers(userId);

		return ResponseEntity.ok(subscribers);
	}

	/**
	 * Check if current user has access to deploy a specific strategy. This is a
	 * convenience endpoint that checks strategy ownership and subscription status.
	 * @param strategyOwnerId The strategy owner's ID
	 * @param user The authenticated user
	 * @return Whether user has access
	 */
	@GetMapping("/user-subscriptions/can-deploy")
	@RequireAuth(minAcr = "1")
	public ResponseEntity<Map<String, Object>> canDeployStrategy(@RequestParam String strategyOwnerId,
			@AuthUser AuthenticatedUser user) {

		String userId = user.getUserId();
		logger.debug("Checking if user {} can deploy strategy from owner {}", userId, strategyOwnerId);

		boolean hasAccess = userSubscriptionService.hasAccessToStrategy(userId, strategyOwnerId);

		return ResponseEntity.ok(Map.of("hasAccess", hasAccess, "userId", userId, "ownerId", strategyOwnerId));
	}

}
