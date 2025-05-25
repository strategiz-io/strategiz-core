package io.strategiz.api.marketplace.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.strategiz.service.marketplace.service.StrategyMarketplaceService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for the Strategy Marketplace
 * Handles operations related to creating, listing, purchasing, and applying strategies
 */
@RestController
@RequestMapping("/api/marketplace/strategies")
public class StrategyMarketplaceController {

    @Autowired
    private StrategyMarketplaceService strategyService;

    private static final Logger log = LoggerFactory.getLogger(StrategyMarketplaceController.class);

    /**
     * List all public strategies in the marketplace
     */
    @GetMapping
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Object> listPublicStrategies(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "20") int limit) {
        
        try {
            List<Map<String, Object>> strategies = strategyService.listPublicStrategies(category, sortBy, limit);
            return ResponseEntity.ok(strategies);
        } catch (Exception e) {
            log.error("Error listing strategies", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to retrieve strategies: " + e.getMessage()));
        }
    }
    
    /**
     * Get a specific strategy by ID
     */
    @GetMapping("/{id}")
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Object> getStrategy(@PathVariable String id) {
        try {
            Map<String, Object> strategy = strategyService.getStrategy(id);
            return ResponseEntity.ok(strategy);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
            }
            log.error("Error getting strategy", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to retrieve strategy: " + e.getMessage()));
        }
    }
    
    /**
     * Create a new strategy
     */
    @PostMapping
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Object> createStrategy(@RequestBody Map<String, Object> strategyData, 
                                                @RequestHeader("Authorization") String authHeader) {
        try {
            // Extract user ID from auth header (simplified for example)
            String userId = extractUserIdFromAuthHeader(authHeader);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
            }
            
            Map<String, Object> response = strategyService.createStrategy(userId, strategyData);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error creating strategy", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to create strategy: " + e.getMessage()));
        }
    }
    
    /**
     * Update an existing strategy
     */
    @PutMapping("/{id}")
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Object> updateStrategy(@PathVariable String id, 
                                               @RequestBody Map<String, Object> strategyData,
                                               @RequestHeader("Authorization") String authHeader) {
        try {
            // Extract user ID from auth header (simplified for example)
            String userId = extractUserIdFromAuthHeader(authHeader);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
            }
            
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
                                               @RequestHeader("Authorization") String authHeader) {
        try {
            // Extract user ID from auth header (simplified for example)
            String userId = extractUserIdFromAuthHeader(authHeader);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
            }
            
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
                                                 @RequestHeader("Authorization") String authHeader) {
        try {
            // Extract user ID from auth header (simplified for example)
            String userId = extractUserIdFromAuthHeader(authHeader);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
            }
            
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
     * Apply a strategy to a user's portfolio
     */
    @PostMapping("/{id}/apply")
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Object> applyStrategy(@PathVariable String id,
                                              @RequestBody Map<String, Object> applicationData,
                                              @RequestHeader("Authorization") String authHeader) {
        try {
            // Extract user ID from auth header (simplified for example)
            String userId = extractUserIdFromAuthHeader(authHeader);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
            }
            
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
    public ResponseEntity<Object> getUserPurchases(@RequestHeader("Authorization") String authHeader) {
        try {
            // Extract user ID from auth header (simplified for example)
            String userId = extractUserIdFromAuthHeader(authHeader);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
            }
            
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
    public ResponseEntity<Object> getUserStrategies(@RequestHeader("Authorization") String authHeader) {
        try {
            // Extract user ID from auth header (simplified for example)
            String userId = extractUserIdFromAuthHeader(authHeader);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
            }
            
            List<Map<String, Object>> strategies = strategyService.getUserStrategies(userId);
            return ResponseEntity.ok(strategies);
        } catch (Exception e) {
            log.error("Error getting user strategies", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to retrieve strategies: " + e.getMessage()));
        }
    }
    
    /**
     * Helper method to extract user ID from auth header
     * In a real application, this would validate the token and extract the user ID
     */
    private String extractUserIdFromAuthHeader(String authHeader) {
        // This is a simplified example
        // In a real application, you would validate the JWT token and extract the user ID
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        
        String token = authHeader.substring(7);
        
        try {
            // For demo purposes, just returning a mock user ID
            // In a real application, you would decode and validate the JWT token
            return "user123";
        } catch (Exception e) {
            log.error("Error extracting user ID from token", e);
            return null;
        }
    }
}
