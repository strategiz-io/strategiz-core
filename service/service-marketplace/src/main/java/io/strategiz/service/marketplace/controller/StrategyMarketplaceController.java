package io.strategiz.service.marketplace.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.strategiz.service.marketplace.service.StrategyMarketplaceService;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.framework.authorization.annotation.RequireAuth;
import io.strategiz.framework.authorization.annotation.AuthUser;
import io.strategiz.framework.authorization.context.AuthenticatedUser;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.marketplace.exception.MarketplaceErrorDetails;
import io.strategiz.service.marketplace.model.response.StrategyDetailResponse;
import io.strategiz.service.marketplace.model.response.StrategySharePreviewResponse;

/**
 * Controller for the Strategy Marketplace.
 *
 * Handles operations related to creating, listing, purchasing, and applying strategies.
 *
 * Owner Subscription Model:
 * - Strategies have both creatorId (original author) and ownerId (current rights holder)
 * - creatorId: Immutable, for attribution and provenance tracking
 * - ownerId: Mutable, receives subscription revenue, can transfer ownership
 * - When ownership transfers, ownerId changes but creatorId remains the same
 *
 * Strategy responses include both fields so frontend can display:
 * - "Created by @username" (creatorId)
 * - "Owned by @username" (ownerId, only shown if different from creator)
 */
@RestController
@RequestMapping("/v1/marketplace/strategies")
@RequireAuth(minAcr = "1")
public class StrategyMarketplaceController extends BaseController {

    @Override
    protected String getModuleName() {
        return "service-marketplace";
    }

    @Autowired
    private StrategyMarketplaceService strategyService;

    private static final Logger log = LoggerFactory.getLogger(StrategyMarketplaceController.class);

    /**
     * List all public strategies in the marketplace.
     *
     * Each strategy includes:
     * - creatorId: Original author (for attribution)
     * - ownerId: Current owner (who receives subscription revenue)
     * - These may differ if ownership has been transferred
     */
    @GetMapping
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://strategiz.io"}, allowedHeaders = "*")
    @RequireAuth(required = false)  // Allow public access to marketplace listing
    public ResponseEntity<Object> listPublicStrategies(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "20") int limit,
            @RequestParam(required = false) Boolean featured) {

        try {
            List<Map<String, Object>> strategies = strategyService.listPublicStrategies(category, sortBy, limit, featured);

            // Return response wrapped in "strategies" key to match frontend expectation
            Map<String, Object> response = new HashMap<>();
            response.put("strategies", strategies);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error listing strategies", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to retrieve strategies: " + e.getMessage()));
        }
    }
    
    /**
     * Get comprehensive strategy details with access control.
     *
     * Query params:
     * - include: Comma-separated list (trades, equityCurve, comments)
     *
     * Example: GET /v1/marketplace/strategies/{id}?include=trades,equityCurve
     *
     * @param id Strategy ID
     * @param include Optional include parameters
     * @param user Authenticated user (optional - allows public access to public strategies)
     * @return Strategy detail response
     */
    @GetMapping("/{id}")
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://strategiz.io"}, allowedHeaders = "*")
    @RequireAuth(required = false)  // Allow public access to public strategies
    public ResponseEntity<Object> getStrategy(
            @PathVariable String id,
            @RequestParam(required = false) String include,
            @AuthUser(required = false) AuthenticatedUser user) {
        try {
            String userId = user != null ? user.getUserId() : null;

            // Parse include params
            Set<String> includeParams = new HashSet<>();
            if (include != null && !include.isEmpty()) {
                includeParams.addAll(Arrays.asList(include.split(",")));
            }

            StrategyDetailResponse response = strategyService.getStrategyDetail(id, userId, includeParams);
            return ResponseEntity.ok(response);
        } catch (StrategizException e) {
            // Handle specific exceptions
            if (e.getErrorDetails() == MarketplaceErrorDetails.STRATEGY_NOT_FOUND) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", e.getMessage()));
            } else if (e.getErrorDetails() == MarketplaceErrorDetails.VIEW_ACCESS_DENIED) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", e.getMessage()));
            }
            log.error("Error getting strategy", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve strategy: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting strategy", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve strategy: " + e.getMessage()));
        }
    }

    /**
     * Get strategy share preview for Open Graph metadata.
     * Used by social media platforms when sharing strategy links.
     *
     * @param id Strategy ID
     * @param user Authenticated user (optional - allows public access)
     * @return Share preview response
     */
    @GetMapping("/{id}/share-preview")
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://strategiz.io"}, allowedHeaders = "*")
    @RequireAuth(required = false)  // Allow public access for social media crawlers
    public ResponseEntity<Object> getSharePreview(
            @PathVariable String id,
            @AuthUser(required = false) AuthenticatedUser user) {
        try {
            String userId = user != null ? user.getUserId() : null;

            StrategySharePreviewResponse response = strategyService.getStrategySharePreview(id, userId);
            return ResponseEntity.ok(response);
        } catch (StrategizException e) {
            if (e.getErrorDetails() == MarketplaceErrorDetails.STRATEGY_NOT_FOUND) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Strategy not found"));
            }
            log.error("Error getting share preview for strategy {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to generate share preview"));
        } catch (Exception e) {
            log.error("Error getting share preview for strategy {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to generate share preview"));
        }
    }

    /**
     * Create a new strategy.
     *
     * Sets both creatorId and ownerId to the creating user:
     * - creatorId: Set once at creation, never changes (attribution)
     * - ownerId: Initially same as creator, can change via ownership transfer
     */
    @PostMapping
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Object> createStrategy(@RequestBody Map<String, Object> strategyData,
                                                @AuthUser AuthenticatedUser user) {
        try {
            String userId = user.getUserId();

            Map<String, Object> response = strategyService.createStrategy(userId, strategyData);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error creating strategy", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to create strategy: " + e.getMessage()));
        }
    }
    
    /**
     * Update an existing strategy.
     *
     * Only the current OWNER can update the strategy (not the original creator).
     * If ownership has been transferred, the original creator loses edit access.
     */
    @PutMapping("/{id}")
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Object> updateStrategy(@PathVariable String id,
                                               @RequestBody Map<String, Object> strategyData,
                                               @AuthUser AuthenticatedUser user) {
        try {
            String userId = user.getUserId();

            Map<String, Object> response = strategyService.updateStrategy(id, userId, strategyData);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Error updating strategy", e);
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
            } else if (e.getMessage().contains("permission")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to update strategy: " + e.getMessage()));
        }
    }
    
    /**
     * Delete a strategy
     */
    @DeleteMapping("/{id}")
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Object> deleteStrategy(@PathVariable String id,
                                               @AuthUser AuthenticatedUser user) {
        try {
            String userId = user.getUserId();

            Map<String, String> result = strategyService.deleteStrategy(id, userId);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            log.error("Error deleting strategy", e);
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
            } else if (e.getMessage().contains("permission")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to delete strategy: " + e.getMessage()));
        }
    }
    
    /**
     * Purchase a strategy
     */
    @PostMapping("/{id}/purchase")
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Object> purchaseStrategy(@PathVariable String id,
                                                 @AuthUser AuthenticatedUser user) {
        try {
            String userId = user.getUserId();

            Map<String, Object> result = strategyService.purchaseStrategy(id, userId);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            log.error("Error purchasing strategy", e);
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
            } else if (e.getMessage().contains("not available")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to purchase strategy: " + e.getMessage()));
        }
    }

    /**
     * Create Stripe checkout session for strategy purchase
     */
    @PostMapping("/{id}/checkout")
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Object> createCheckoutSession(@PathVariable String id,
                                                       @AuthUser AuthenticatedUser user) {
        try {
            String userId = user.getUserId();

            Map<String, String> result = strategyService.createStrategyCheckout(id, userId);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            log.error("Error creating checkout session", e);
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
            } else if (e.getMessage().contains("not available")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
            } else if (e.getMessage().contains("pricing")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to create checkout session: " + e.getMessage()));
        }
    }

    /**
     * Apply a strategy to a user's portfolio
     */
    @PostMapping("/{id}/apply")
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Object> applyStrategy(@PathVariable String id,
                                              @RequestBody Map<String, Object> applicationData,
                                              @AuthUser AuthenticatedUser user) {
        try {
            String userId = user.getUserId();

            Map<String, Object> result = strategyService.applyStrategy(id, userId, applicationData);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (RuntimeException e) {
            log.error("Error applying strategy", e);
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
            } else if (e.getMessage().contains("must purchase")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to apply strategy: " + e.getMessage()));
        }
    }
    
    /**
     * Get user's purchased strategies
     */
    @GetMapping("/user/purchases")
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Object> getUserPurchases(@AuthUser AuthenticatedUser user) {
        try {
            String userId = user.getUserId();

            List<Map<String, Object>> purchasedStrategies = strategyService.getUserPurchases(userId);
            return ResponseEntity.ok(purchasedStrategies);
        } catch (RuntimeException e) {
            log.error("Error getting user purchases", e);
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to retrieve purchases: " + e.getMessage()));
        }
    }
    
    /**
     * Get strategies created by the user
     */
    @GetMapping("/user/strategies")
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Object> getUserStrategies(@AuthUser AuthenticatedUser user) {
        try {
            String userId = user.getUserId();

            List<Map<String, Object>> strategies = strategyService.getUserStrategies(userId);
            return ResponseEntity.ok(strategies);
        } catch (Exception e) {
            log.error("Error getting user strategies", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to retrieve strategies: " + e.getMessage()));
        }
    }
}
