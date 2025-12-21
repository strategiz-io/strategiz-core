package io.strategiz.service.marketplace.controller;

import io.strategiz.data.strategy.entity.StrategySubscriptionEntity;
import io.strategiz.service.marketplace.service.StrategySubscriptionService;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.base.constants.ModuleConstants;
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
 * Endpoints:
 * - POST   /v1/subscriptions                          - Subscribe to a strategy
 * - DELETE /v1/subscriptions/{id}                     - Cancel subscription
 * - GET    /v1/subscriptions                          - Get my subscriptions
 * - GET    /v1/subscriptions/{id}                     - Get subscription details
 * - GET    /v1/subscriptions/check/{strategyId}       - Check if subscribed
 * - GET    /v1/subscriptions/strategy/{id}/subscribers - Get subscribers (creator only)
 * - GET    /v1/subscriptions/access/{strategyId}      - Check if can access strategy
 */
@RestController
@RequestMapping("/v1/subscriptions")
public class StrategySubscriptionController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(StrategySubscriptionController.class);

    @Override
    protected String getModuleName() {
        return ModuleConstants.MARKETPLACE_MODULE;
    }

    @Autowired
    private StrategySubscriptionService subscriptionService;

    /**
     * Subscribe to a strategy.
     */
    @PostMapping
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Object> subscribe(
            @RequestBody Map<String, String> requestBody,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String userId = extractUserIdFromAuthHeader(authHeader);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Unauthorized"));
            }

            String strategyId = requestBody.get("strategyId");
            if (strategyId == null || strategyId.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Strategy ID is required"));
            }

            StrategySubscriptionEntity subscription = subscriptionService.subscribe(strategyId.trim(), userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(subscription);
        } catch (Exception e) {
            log.error("Error subscribing to strategy", e);
            return handleException(e);
        }
    }

    /**
     * Cancel a subscription.
     */
    @DeleteMapping("/{subscriptionId}")
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Object> cancelSubscription(
            @PathVariable String subscriptionId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String userId = extractUserIdFromAuthHeader(authHeader);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Unauthorized"));
            }

            StrategySubscriptionEntity cancelled = subscriptionService.cancelSubscription(subscriptionId, userId);
            return ResponseEntity.ok(Map.of(
                    "message", "Subscription cancelled successfully",
                    "subscription", cancelled
            ));
        } catch (Exception e) {
            log.error("Error cancelling subscription {}", subscriptionId, e);
            return handleException(e);
        }
    }

    /**
     * Get my subscriptions.
     */
    @GetMapping
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Object> getMySubscriptions(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "50") int limit) {
        try {
            String userId = extractUserIdFromAuthHeader(authHeader);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Unauthorized"));
            }

            List<StrategySubscriptionEntity> subscriptions = subscriptionService.getUserSubscriptions(userId, limit);
            return ResponseEntity.ok(Map.of(
                    "subscriptions", subscriptions,
                    "count", subscriptions.size()
            ));
        } catch (Exception e) {
            log.error("Error getting subscriptions", e);
            return handleException(e);
        }
    }

    /**
     * Get subscription details.
     */
    @GetMapping("/{subscriptionId}")
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Object> getSubscription(
            @PathVariable String subscriptionId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String userId = extractUserIdFromAuthHeader(authHeader);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Unauthorized"));
            }

            Optional<StrategySubscriptionEntity> subscription = subscriptionService.getSubscription(subscriptionId);
            if (subscription.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Subscription not found"));
            }

            // Verify ownership
            if (!subscription.get().getUserId().equals(userId) &&
                    !subscription.get().getCreatorId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Access denied"));
            }

            return ResponseEntity.ok(subscription.get());
        } catch (Exception e) {
            log.error("Error getting subscription {}", subscriptionId, e);
            return handleException(e);
        }
    }

    /**
     * Check if subscribed to a strategy.
     */
    @GetMapping("/check/{strategyId}")
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Object> checkSubscription(
            @PathVariable String strategyId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String userId = extractUserIdFromAuthHeader(authHeader);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Unauthorized"));
            }

            boolean isSubscribed = subscriptionService.hasActiveSubscription(userId, strategyId);
            Optional<StrategySubscriptionEntity> subscription =
                    subscriptionService.getSubscriptionByUserAndStrategy(userId, strategyId);

            return ResponseEntity.ok(Map.of(
                    "strategyId", strategyId,
                    "isSubscribed", isSubscribed,
                    "subscription", subscription.orElse(null)
            ));
        } catch (Exception e) {
            log.error("Error checking subscription for strategy {}", strategyId, e);
            return handleException(e);
        }
    }

    /**
     * Check if can access a strategy (owns it or has subscription).
     */
    @GetMapping("/access/{strategyId}")
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Object> checkAccess(
            @PathVariable String strategyId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String userId = extractUserIdFromAuthHeader(authHeader);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Unauthorized"));
            }

            boolean canAccess = subscriptionService.canAccessStrategy(userId, strategyId);
            return ResponseEntity.ok(Map.of(
                    "strategyId", strategyId,
                    "canAccess", canAccess
            ));
        } catch (Exception e) {
            log.error("Error checking access for strategy {}", strategyId, e);
            return handleException(e);
        }
    }

    /**
     * Get subscribers for a strategy (creator only).
     */
    @GetMapping("/strategy/{strategyId}/subscribers")
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Object> getStrategySubscribers(
            @PathVariable String strategyId,
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "50") int limit) {
        try {
            String userId = extractUserIdFromAuthHeader(authHeader);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Unauthorized"));
            }

            List<StrategySubscriptionEntity> subscribers =
                    subscriptionService.getStrategySubscribers(strategyId, userId, limit);
            return ResponseEntity.ok(Map.of(
                    "subscribers", subscribers,
                    "count", subscribers.size()
            ));
        } catch (Exception e) {
            log.error("Error getting subscribers for strategy {}", strategyId, e);
            return handleException(e);
        }
    }

    // Helper methods

    private String extractUserIdFromAuthHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        // In a real implementation, decode and validate JWT token
        // For now, return mock user ID
        return "user123";
    }

    private ResponseEntity<Object> handleException(Exception e) {
        String message = e.getMessage();
        if (message != null) {
            if (message.contains("not found") || message.contains("NOT_FOUND")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", message));
            } else if (message.contains("unauthorized") || message.contains("UNAUTHORIZED")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", message));
            } else if (message.contains("already subscribed") || message.contains("ALREADY_SUBSCRIBED")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", message));
            } else if (message.contains("own strategy") || message.contains("CANNOT_SUBSCRIBE_OWN")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", message));
            } else if (message.contains("not published") || message.contains("NOT_PUBLISHED")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", message));
            } else if (message.contains("expired") || message.contains("EXPIRED")) {
                return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                        .body(Map.of("error", message));
            }
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "An error occurred: " + message));
    }
}
