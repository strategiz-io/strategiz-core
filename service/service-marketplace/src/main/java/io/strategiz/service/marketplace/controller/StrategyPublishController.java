package io.strategiz.service.marketplace.controller;

import io.strategiz.data.strategy.entity.PricingType;
import io.strategiz.data.strategy.entity.Strategy;
import io.strategiz.service.marketplace.service.StrategyPublishService;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.base.constants.ModuleConstants;
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
        return ModuleConstants.MARKETPLACE_MODULE;
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
     * Update pricing for a published strategy.
     */
    @PutMapping("/{strategyId}/pricing")
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Object> updatePricing(
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

            Strategy updated = publishService.updatePricing(
                    strategyId, userId, pricingType, oneTimePrice, monthlyPrice);

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
            return ResponseEntity.ok(Map.of(
                    "strategyId", strategyId,
                    "isPublished", strategy.isPublished(),
                    "publishedAt", strategy.getPublishedAt(),
                    "pricing", strategy.getPricing(),
                    "subscriberCount", strategy.getSubscriberCount(),
                    "commentCount", strategy.getCommentCount()
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
