package io.strategiz.service.marketplace.controller;

import io.strategiz.data.strategy.entity.StrategySubscriptionEntity;
import io.strategiz.service.marketplace.service.StrategySubscriptionService;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.framework.authorization.annotation.RequireAuth;
import io.strategiz.framework.authorization.annotation.AuthUser;
import io.strategiz.framework.authorization.context.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for strategy subscriptions.
 *
 * Owner Subscription Model: - Users subscribe to the current OWNER of a strategy (not the
 * creator) - When ownership transfers, subscriptions automatically route payments to new
 * owner - Original creator keeps attribution but receives no revenue after transfer
 *
 * Endpoints: - POST /v1/subscriptions - Subscribe to a strategy - DELETE
 * /v1/subscriptions/{id} - Cancel subscription - GET /v1/subscriptions - Get my
 * subscriptions - GET /v1/subscriptions/{id} - Get subscription details (subscriber or
 * owner) - GET /v1/subscriptions/check/{strategyId} - Check if subscribed - GET
 * /v1/subscriptions/strategy/{id}/subscribers - Get subscribers (current owner only) -
 * GET /v1/subscriptions/access/{strategyId} - Check if can access strategy
 */
@RestController
@RequestMapping("/v1/subscriptions")
@RequireAuth(minAcr = "1")
public class StrategySubscriptionController extends BaseController {

	private static final Logger log = LoggerFactory.getLogger(StrategySubscriptionController.class);

	@Override
	protected String getModuleName() {
		return "service-marketplace";
	}

	@Autowired
	private StrategySubscriptionService subscriptionService;

	/**
	 * Subscribe to a strategy.
	 */
	@PostMapping
	@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:3001", "https://strategiz.io" },
			allowedHeaders = "*")
	public ResponseEntity<Object> subscribe(@RequestBody Map<String, String> requestBody,
			@AuthUser AuthenticatedUser user) {
		try {
			String userId = user.getUserId();

			String strategyId = requestBody.get("strategyId");
			if (strategyId == null || strategyId.trim().isEmpty()) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Strategy ID is required"));
			}

			StrategySubscriptionEntity subscription = subscriptionService.subscribe(strategyId.trim(), userId);
			return ResponseEntity.status(HttpStatus.CREATED).body(subscription);
		}
		catch (Exception e) {
			log.error("Error subscribing to strategy", e);
			return handleException(e);
		}
	}

	/**
	 * Cancel a subscription.
	 */
	@DeleteMapping("/{subscriptionId}")
	@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:3001", "https://strategiz.io" },
			allowedHeaders = "*")
	public ResponseEntity<Object> cancelSubscription(@PathVariable String subscriptionId,
			@AuthUser AuthenticatedUser user) {
		try {
			String userId = user.getUserId();

			StrategySubscriptionEntity cancelled = subscriptionService.cancelSubscription(subscriptionId, userId);
			return ResponseEntity
				.ok(Map.of("message", "Subscription cancelled successfully", "subscription", cancelled));
		}
		catch (Exception e) {
			log.error("Error cancelling subscription {}", subscriptionId, e);
			return handleException(e);
		}
	}

	/**
	 * Get my subscriptions.
	 */
	@GetMapping
	@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:3001", "https://strategiz.io" },
			allowedHeaders = "*")
	public ResponseEntity<Object> getMySubscriptions(@AuthUser AuthenticatedUser user,
			@RequestParam(defaultValue = "50") int limit) {
		try {
			String userId = user.getUserId();

			List<StrategySubscriptionEntity> subscriptions = subscriptionService.getUserSubscriptions(userId, limit);
			return ResponseEntity.ok(Map.of("subscriptions", subscriptions, "count", subscriptions.size()));
		}
		catch (Exception e) {
			log.error("Error getting subscriptions", e);
			return handleException(e);
		}
	}

	/**
	 * Get subscription details.
	 *
	 * Access granted to: - The subscriber (userId) - The current strategy owner (ownerId)
	 * who receives payments
	 */
	@GetMapping("/{subscriptionId}")
	@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:3001", "https://strategiz.io" },
			allowedHeaders = "*")
	public ResponseEntity<Object> getSubscription(@PathVariable String subscriptionId,
			@AuthUser AuthenticatedUser user) {
		try {
			String userId = user.getUserId();

			Optional<StrategySubscriptionEntity> subscription = subscriptionService.getSubscription(subscriptionId);
			if (subscription.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Subscription not found"));
			}

			// Verify access: subscriber OR current owner (not creator)
			// In owner subscription model, current owner receives payments and can view
			// subscriptions
			if (!subscription.get().getOwnerId().equals(userId) && !subscription.get().getOwnerId().equals(userId)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied"));
			}

			return ResponseEntity.ok(subscription.get());
		}
		catch (Exception e) {
			log.error("Error getting subscription {}", subscriptionId, e);
			return handleException(e);
		}
	}

	/**
	 * Check if subscribed to a strategy.
	 */
	@GetMapping("/check/{strategyId}")
	@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:3001", "https://strategiz.io" },
			allowedHeaders = "*")
	public ResponseEntity<Object> checkSubscription(@PathVariable String strategyId, @AuthUser AuthenticatedUser user) {
		try {
			String userId = user.getUserId();

			boolean isSubscribed = subscriptionService.hasActiveSubscription(userId, strategyId);
			Optional<StrategySubscriptionEntity> subscription = subscriptionService
				.getSubscriptionByUserAndStrategy(userId, strategyId);

			return ResponseEntity.ok(Map.of("strategyId", strategyId, "isSubscribed", isSubscribed, "subscription",
					subscription.orElse(null)));
		}
		catch (Exception e) {
			log.error("Error checking subscription for strategy {}", strategyId, e);
			return handleException(e);
		}
	}

	/**
	 * Check if can access a strategy (owns it or has subscription).
	 */
	@GetMapping("/access/{strategyId}")
	@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:3001", "https://strategiz.io" },
			allowedHeaders = "*")
	public ResponseEntity<Object> checkAccess(@PathVariable String strategyId, @AuthUser AuthenticatedUser user) {
		try {
			String userId = user.getUserId();

			boolean canAccess = subscriptionService.canAccessStrategy(userId, strategyId);
			return ResponseEntity.ok(Map.of("strategyId", strategyId, "canAccess", canAccess));
		}
		catch (Exception e) {
			log.error("Error checking access for strategy {}", strategyId, e);
			return handleException(e);
		}
	}

	/**
	 * Get subscribers for a strategy (current owner only).
	 *
	 * Only the current owner can view subscriber list and revenue data. Original creator
	 * cannot access this after ownership transfer.
	 */
	@GetMapping("/strategy/{strategyId}/subscribers")
	@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:3001", "https://strategiz.io" },
			allowedHeaders = "*")
	public ResponseEntity<Object> getStrategySubscribers(@PathVariable String strategyId,
			@AuthUser AuthenticatedUser user, @RequestParam(defaultValue = "50") int limit) {
		try {
			String userId = user.getUserId();

			// This service method should validate that userId is the current owner
			List<StrategySubscriptionEntity> subscribers = subscriptionService.getStrategySubscribers(strategyId,
					userId, limit);
			return ResponseEntity.ok(Map.of("subscribers", subscribers, "count", subscribers.size()));
		}
		catch (Exception e) {
			log.error("Error getting subscribers for strategy {}", strategyId, e);
			return handleException(e);
		}
	}

	// Helper methods

	private ResponseEntity<Object> handleException(Exception e) {
		String message = e.getMessage();
		if (message != null) {
			if (message.contains("not found") || message.contains("NOT_FOUND")) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", message));
			}
			else if (message.contains("unauthorized") || message.contains("UNAUTHORIZED")) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", message));
			}
			else if (message.contains("already subscribed") || message.contains("ALREADY_SUBSCRIBED")) {
				return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", message));
			}
			else if (message.contains("own strategy") || message.contains("CANNOT_SUBSCRIBE_OWN")) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", message));
			}
			else if (message.contains("not published") || message.contains("NOT_PUBLISHED")) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", message));
			}
			else if (message.contains("expired") || message.contains("EXPIRED")) {
				return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(Map.of("error", message));
			}
		}
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(Map.of("error", "An error occurred: " + message));
	}

}
