package io.strategiz.service.dashboard.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.strategiz.service.dashboard.WatchlistService;
import io.strategiz.service.dashboard.model.watchlist.WatchlistResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for watchlist data.
 * Provides endpoints for retrieving user watchlist information.
 * Uses clean architecture - returns resources directly, no wrappers.
 */
@RestController
@RequestMapping("/api/dashboard/watchlist")
public class WatchlistController {
    
    private static final Logger log = LoggerFactory.getLogger(WatchlistController.class);

    private final WatchlistService watchlistService;
    
    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }
    
    /**
     * Get watchlist data for a user.
     * 
     * @param userId The user ID to get data for
     * @return Clean watchlist data - no wrapper, let GlobalExceptionHandler handle errors
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getWatchlist(@RequestParam(required = false) String userId) {
        // Use a default user ID if not provided
        if (userId == null || userId.isEmpty()) {
            userId = "test-user";
        }
        
        log.info("Retrieving watchlist data for user: {}", userId);
        
        // Get watchlist data from service - let exceptions bubble up
        WatchlistResponse watchlistData = watchlistService.getWatchlist(userId);
        
        // Check if there is meaningful data to display
        if (watchlistData == null) {
            throw new RuntimeException("Watchlist not found for user: " + userId);
        }
        
        // Create response
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("watchlist", watchlistData);
        
        // Return clean response - headers added by StandardHeadersInterceptor
        return ResponseEntity.ok(responseData);
    }
}
