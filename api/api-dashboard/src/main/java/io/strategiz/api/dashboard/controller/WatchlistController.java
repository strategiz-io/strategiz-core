package io.strategiz.api.dashboard.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.strategiz.api.dashboard.controller.DashboardBaseController;
import io.strategiz.api.dashboard.mapper.WatchlistResponseBuilder;
import io.strategiz.api.dashboard.model.watchlist.WatchlistResponse;
import io.strategiz.service.base.model.BaseServiceResponse;
import io.strategiz.service.dashboard.DashboardService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for watchlist data.
 * Provides endpoints for retrieving user watchlist information.
 */
@RestController
@RequestMapping("/api/dashboard/watchlist")
public class WatchlistController extends DashboardBaseController {
    
    private static final Logger log = LoggerFactory.getLogger(WatchlistController.class);

    private final DashboardService dashboardService;
    private final WatchlistResponseBuilder responseBuilder;
    
    @Autowired
    public WatchlistController(DashboardService dashboardService, 
                               WatchlistResponseBuilder responseBuilder) {
        this.dashboardService = dashboardService;
        this.responseBuilder = responseBuilder;
    }
    
    @Override
    public BaseServiceResponse process(Object request) {
        // Default implementation for the required abstract method
        // This is needed to satisfy the BaseController contract
        WatchlistResponse response = new WatchlistResponse();
        response.setMetadata(convertMetadataToMap(createSuccessMetadata()));
        return response;
    }
    
    /**
     * Get watchlist data for a user.
     * 
     * @param userId The user ID to get data for
     * @return ResponseEntity with WatchlistResponse
     */
    @GetMapping
    public ResponseEntity<WatchlistResponse> getWatchlist(@RequestParam(required = false) String userId) {
        try {
            // Use a default user ID if not provided
            if (userId == null || userId.isEmpty()) {
                userId = "test-user";
            }
            
            log.info("Retrieving watchlist data for user: {}", userId);
            
            // Get watchlist data from service
            io.strategiz.service.dashboard.model.watchlist.WatchlistResponse serviceResponse = 
                dashboardService.getWatchlist(userId);
            
            // Create and populate API response using the domain-specific builder
            WatchlistResponse dashboardResponse = responseBuilder.buildWatchlistResponse(serviceResponse);
            dashboardResponse.setMetadata(convertMetadataToMap(createSuccessMetadata()));
            
            // Return successful response with headers
            return new ResponseEntity<>(dashboardResponse, createJsonResponseHeaders(), HttpStatus.OK);
        } catch (Exception e) {
            // Log the error
            log.error("Error retrieving watchlist: {}", e.getMessage(), e);
            
            // Create error response
            WatchlistResponse errorResponse = new WatchlistResponse();
            errorResponse.setMetadata(convertMetadataToMap(createErrorMetadata("WATCHLIST_ERROR", e.getMessage())));
            
            // Return error response with headers
            return new ResponseEntity<>(errorResponse, createJsonResponseHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
