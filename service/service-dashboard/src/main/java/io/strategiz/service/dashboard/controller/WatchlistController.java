package io.strategiz.service.dashboard.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.strategiz.data.user.model.watchlist.*;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.data.user.model.UserProfile;
import io.strategiz.data.user.model.User;
import io.strategiz.service.dashboard.exception.DashboardErrorDetails;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.base.controller.BaseController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.List;

/**
 * Controller for watchlist data.
 * Returns real integrated data regardless of user's demo/real mode.
 * The mode only affects UI behavior, not the data itself.
 */
@RestController
@RequestMapping("/api/dashboard/watchlist")
@CrossOrigin(origins = "*")
public class WatchlistController extends BaseController {
    
    private static final Logger log = LoggerFactory.getLogger(WatchlistController.class);

    private final UserRepository userRepository;
    
    public WatchlistController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    /**
     * Get watchlist data for a user.
     * Always returns real market data regardless of trading mode.
     * 
     * @param userId The user ID to get data for
     * @return Real watchlist data with market information
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getWatchlist(@RequestParam(required = false) String userId) {
        // Use a default user ID if not provided
        if (userId == null || userId.isEmpty()) {
            userId = "test-user";
        }
        
        log.info("Retrieving watchlist data for user: {}", userId);
        
        try {
            // Get user trading mode for UI context only
            String tradingMode = getUserTradingMode(userId);
            
            // Get real watchlist data from our data layer
            WatchlistCollectionResponse watchlist = userRepository.readUserWatchlist(userId);
            
            if (watchlist == null) {
                throw new StrategizException(DashboardErrorDetails.WATCHLIST_NOT_FOUND, "service-dashboard", userId);
            }
            
            // Create response with real data
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("userId", userId);
            responseData.put("tradingMode", tradingMode); // For UI context only
            responseData.put("watchlist", formatWatchlistForUI(watchlist));
            responseData.put("metadata", createMetadata(watchlist, tradingMode));
            
            return ResponseEntity.ok(responseData);
            
        } catch (StrategizException e) {
            // Re-throw business exceptions
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving watchlist for user: {}", userId, e);
            throw new StrategizException(DashboardErrorDetails.DASHBOARD_ERROR, "service-dashboard", "get_watchlist", e.getMessage());
        }
    }
    
    /**
     * Create a new watchlist item.
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createWatchlistItem(
            @RequestParam String userId,
            @RequestBody CreateWatchlistItemRequest request) {
        
        log.info("Creating watchlist item for user: {} - {}", userId, request.getSymbol());
        
        // Validate input
        if (request.getSymbol() == null || request.getSymbol().trim().isEmpty()) {
            throw new StrategizException(DashboardErrorDetails.INVALID_SYMBOL, "service-dashboard", request.getSymbol());
        }
        
        try {
            // Check if item already exists
            boolean exists = userRepository.isAssetInWatchlist(userId, request.getSymbol());
            if (exists) {
                throw new StrategizException(DashboardErrorDetails.DUPLICATE_WATCHLIST_ITEM, "service-dashboard", userId, request.getSymbol());
            }
            
            WatchlistOperationResponse response = userRepository.createWatchlistItem(userId, request);
            
            if (response.getSuccess()) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "operation", response.getOperation(),
                    "id", response.getId(),
                    "symbol", response.getSymbol(),
                    "message", response.getMessage()
                ));
            } else {
                throw new StrategizException(DashboardErrorDetails.DASHBOARD_ERROR, "service-dashboard", "create_watchlist_item", response.getMessage());
            }
        } catch (StrategizException e) {
            // Re-throw business exceptions
            throw e;
        } catch (Exception e) {
            log.error("Error creating watchlist item for user: {} - {}", userId, request.getSymbol(), e);
            throw new StrategizException(DashboardErrorDetails.DASHBOARD_ERROR, "service-dashboard", "create_watchlist_item", e.getMessage());
        }
    }
    
    /**
     * Delete a watchlist item.
     */
    @DeleteMapping("/{itemId}")
    public ResponseEntity<Map<String, Object>> deleteWatchlistItem(
            @RequestParam String userId,
            @PathVariable String itemId) {
        
        log.info("Deleting watchlist item: {} for user: {}", itemId, userId);
        
        // Validate input
        if (itemId == null || itemId.trim().isEmpty()) {
            throw new StrategizException(DashboardErrorDetails.WATCHLIST_ITEM_NOT_FOUND, "service-dashboard", userId, itemId);
        }
        
        try {
            DeleteWatchlistItemRequest request = DeleteWatchlistItemRequest.forId(itemId);
            WatchlistOperationResponse response = userRepository.deleteWatchlistItem(userId, request);
            
            if (response.getSuccess()) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "operation", response.getOperation(),
                    "message", response.getMessage()
                ));
            } else {
                throw new StrategizException(DashboardErrorDetails.WATCHLIST_ITEM_NOT_FOUND, "service-dashboard", userId, itemId);
            }
        } catch (StrategizException e) {
            // Re-throw business exceptions
            throw e;
        } catch (Exception e) {
            log.error("Error deleting watchlist item: {} for user: {}", itemId, userId, e);
            throw new StrategizException(DashboardErrorDetails.DASHBOARD_ERROR, "service-dashboard", "delete_watchlist_item", e.getMessage());
        }
    }
    
    /**
     * Check if symbol is in user's watchlist.
     */
    @GetMapping("/check/{symbol}")
    public ResponseEntity<Map<String, Object>> checkSymbolInWatchlist(
            @RequestParam String userId,
            @PathVariable String symbol) {
        
        // Validate input
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new StrategizException(DashboardErrorDetails.INVALID_SYMBOL, "service-dashboard", symbol);
        }
        
        try {
            boolean inWatchlist = userRepository.isAssetInWatchlist(userId, symbol);
            return ResponseEntity.ok(Map.of(
                "symbol", symbol,
                "inWatchlist", inWatchlist
            ));
        } catch (StrategizException e) {
            // Re-throw business exceptions
            throw e;
        } catch (Exception e) {
            log.error("Error checking symbol in watchlist for user: {} - {}", userId, symbol, e);
            throw new StrategizException(DashboardErrorDetails.DASHBOARD_ERROR, "service-dashboard", "check_symbol", e.getMessage());
        }
    }
    
    /**
     * Get user's trading mode for UI context.
     */
    private String getUserTradingMode(String userId) {
        try {
            Optional<User> userOpt = userRepository.getUserById(userId);
            if (userOpt.isPresent()) {
                UserProfile profile = userOpt.get().getProfile();
                return profile != null ? profile.getTradingMode() : "demo";
            }
        } catch (Exception e) {
            log.warn("Could not retrieve user trading mode for {}, defaulting to demo", userId);
        }
        return "demo";
    }
    
    /**
     * Formats watchlist data for UI consumption.
     * Always returns real market data regardless of mode.
     */
    private Map<String, Object> formatWatchlistForUI(WatchlistCollectionResponse watchlist) {
        Map<String, Object> formattedWatchlist = new HashMap<>();
        
        // Real watchlist data
        formattedWatchlist.put("items", watchlist.getItems());
        formattedWatchlist.put("totalCount", watchlist.getTotalCount());
        formattedWatchlist.put("activeCount", watchlist.getActiveCount());
        formattedWatchlist.put("isEmpty", watchlist.getIsEmpty());
        
        // Standard categories for all users
        formattedWatchlist.put("availableCategories", 
            List.of("All", "Crypto", "Stocks", "ETFs", "Commodities"));
        formattedWatchlist.put("description", "Your market watchlist");
        
        return formattedWatchlist;
    }
    
    /**
     * Creates metadata about the watchlist.
     */
    private Map<String, Object> createMetadata(WatchlistCollectionResponse watchlist, String tradingMode) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("lastUpdated", System.currentTimeMillis());
        metadata.put("tradingMode", tradingMode); // For UI context only
        metadata.put("dataType", "real"); // Always real data
        
        // Summary stats
        metadata.put("summary", Map.of(
            "totalSymbols", watchlist.getTotalCount(),
            "activeSymbols", watchlist.getActiveCount(),
            "cryptoCount", countByType(watchlist, "crypto"),
            "stockCount", countByType(watchlist, "stock"),
            "etfCount", countByType(watchlist, "etf")
        ));
        
        return metadata;
    }
    
    /**
     * Helper to count items by type.
     */
    private long countByType(WatchlistCollectionResponse watchlist, String type) {
        return watchlist.getItems().stream()
            .filter(item -> type.equals(item.getType()))
            .count();
    }
}
