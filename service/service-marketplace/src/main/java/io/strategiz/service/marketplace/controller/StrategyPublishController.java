package io.strategiz.service.marketplace.controller;

import io.strategiz.data.strategy.entity.PricingType;
import io.strategiz.data.strategy.entity.Strategy;
import io.strategiz.service.marketplace.service.StrategyPublishService;
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

import java.math.BigDecimal;
import java.util.Map;

/**
 * Controller for strategy publishing operations.
 *
 * Endpoints:
 * - POST /v1/strategies/{strategyId}/publish   - Publish strategy with pricing
 * - POST /v1/strategies/{strategyId}/unpublish - Make strategy private
 * - PUT  /v1/strategies/{strategyId}/pricing   - Update pricing
 * - GET  /v1/strategies/{strategyId}/status    - Get publishing status
 */
@RestController
@RequestMapping("/v1/strategies")
@RequireAuth(minAcr = "1")
public class StrategyPublishController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(StrategyPublishController.class);

    @Override
    protected String getModuleName() {
        return "service-marketplace";
    }

    @Autowired
    private StrategyPublishService publishService;

    /**
     * Publish a strategy with pricing configuration.
     *
     * Request body:
     * {
     *   "pricingType": "FREE" | "ONE_TIME" | "SUBSCRIPTION",
     *   "oneTimePrice": 29.99,  // required for ONE_TIME
     *   "monthlyPrice": 9.99    // required for SUBSCRIPTION
     * }
     */
    @PostMapping("/{strategyId}/publish")
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Object> publishStrategy(
            @PathVariable String strategyId,
            @RequestBody Map<String, Object> requestBody,
            @AuthUser AuthenticatedUser user) {
        try {
            String userId = user.getUserId();

            // Parse pricing type
            String pricingTypeStr = (String) requestBody.get("pricingType");
            if (pricingTypeStr == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Pricing type is required"));
            }

            PricingType pricingType;
            try {
                pricingType = PricingType.valueOf(pricingTypeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid pricing type. Must be FREE, ONE_TIME, or SUBSCRIPTION"));
            }

            // Parse prices
            BigDecimal oneTimePrice = parsePrice(requestBody.get("oneTimePrice"));
            BigDecimal monthlyPrice = parsePrice(requestBody.get("monthlyPrice"));

            Strategy published = publishService.publishStrategy(
                    strategyId, userId, pricingType, oneTimePrice, monthlyPrice);

            return ResponseEntity.ok(Map.of(
                    "message", "Strategy published successfully",
                    "strategy", published
            ));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid publish request for strategy {}: {}", strategyId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error publishing strategy {}", strategyId, e);
            return handleException(e);
        }
    }

    /**
     * Unpublish a strategy (make it private).
     */
    @PostMapping("/{strategyId}/unpublish")
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Object> unpublishStrategy(
            @PathVariable String strategyId,
            @AuthUser AuthenticatedUser user) {
        try {
            String userId = user.getUserId();

            Strategy unpublished = publishService.unpublishStrategy(strategyId, userId);
            return ResponseEntity.ok(Map.of(
                    "message", "Strategy unpublished successfully",
                    "strategy", unpublished
            ));
        } catch (Exception e) {
            log.error("Error unpublishing strategy {}", strategyId, e);
            return handleException(e);
        }
    }

    /**
     * Update pricing and listing status for a strategy.
     *
     * Supports two request formats:
     *
     * Format 1 - Simple (for marketplace listing):
     * {
     *   "price": 99.99,              // optional - the sale price
     *   "listedStatus": "LISTED"     // optional - "LISTED" or "NOT_LISTED"
     * }
     *
     * Format 2 - Full pricing configuration:
     * {
     *   "pricingType": "ONE_TIME",   // "FREE", "ONE_TIME", or "SUBSCRIPTION"
     *   "oneTimePrice": 29.99,       // for ONE_TIME
     *   "monthlyPrice": 9.99         // for SUBSCRIPTION
     * }
     */
    @PutMapping("/{strategyId}/pricing")
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Object> updatePricing(
            @PathVariable String strategyId,
            @RequestBody Map<String, Object> requestBody,
            @AuthUser AuthenticatedUser user) {
        try {
            String userId = user.getUserId();

            // Check for simple format (price + listedStatus)
            BigDecimal price = parsePrice(requestBody.get("price"));
            String listedStatus = (String) requestBody.get("listedStatus");

            // Check for full format (pricingType + oneTimePrice/monthlyPrice)
            String pricingTypeStr = (String) requestBody.get("pricingType");

            Strategy updated;

            if (price != null || listedStatus != null) {
                // Simple format - update price and/or listedStatus
                updated = publishService.updatePricingAndListingStatus(
                        strategyId, userId, price, listedStatus);
            } else if (pricingTypeStr != null) {
                // Full format - legacy pricing update
                PricingType pricingType;
                try {
                    pricingType = PricingType.valueOf(pricingTypeStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Invalid pricing type. Must be FREE, ONE_TIME, or SUBSCRIPTION"));
                }

                BigDecimal oneTimePrice = parsePrice(requestBody.get("oneTimePrice"));
                BigDecimal monthlyPrice = parsePrice(requestBody.get("monthlyPrice"));

                updated = publishService.updatePricing(
                        strategyId, userId, pricingType, oneTimePrice, monthlyPrice);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Either 'price'/'listedStatus' or 'pricingType' must be provided"));
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Pricing updated successfully",
                    "strategy", updated
            ));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid pricing update for strategy {}: {}", strategyId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating pricing for strategy {}", strategyId, e);
            return handleException(e);
        }
    }

    /**
     * Get strategy publishing status.
     */
    @GetMapping("/{strategyId}/status")
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Object> getPublishStatus(@PathVariable String strategyId) {
        try {
            Strategy strategy = publishService.getStrategyDetails(strategyId);
            // Note: publishedAt field removed from data model - use createdDate or track separately
            return ResponseEntity.ok(Map.of(
                    "strategyId", strategyId,
                    "isPublished", Boolean.TRUE.equals(strategy.getIsPublished()),
                    "pricing", strategy.getPricing() != null ? strategy.getPricing() : "FREE",
                    "subscriberCount", strategy.getSubscriberCount() != null ? strategy.getSubscriberCount() : 0,
                    "commentCount", strategy.getCommentCount() != null ? strategy.getCommentCount() : 0
            ));
        } catch (Exception e) {
            log.error("Error getting status for strategy {}", strategyId, e);
            return handleException(e);
        }
    }

    // Helper methods

    private BigDecimal parsePrice(Object priceObj) {
        if (priceObj == null) {
            return null;
        }
        if (priceObj instanceof Number) {
            return BigDecimal.valueOf(((Number) priceObj).doubleValue());
        }
        if (priceObj instanceof String) {
            try {
                return new BigDecimal((String) priceObj);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
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
            } else if (message.contains("not published") || message.contains("NOT_PUBLISHED")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", message));
            }
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "An error occurred: " + message));
    }
}
