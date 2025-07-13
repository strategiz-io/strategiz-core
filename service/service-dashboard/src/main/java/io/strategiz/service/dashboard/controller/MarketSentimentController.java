package io.strategiz.service.dashboard.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.strategiz.service.dashboard.MarketSentimentService;
import io.strategiz.service.dashboard.model.marketsentiment.MarketSentimentResponse;
import io.strategiz.service.dashboard.exception.DashboardErrorDetails;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.base.controller.BaseController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for market sentiment data.
 * Provides endpoints for retrieving market sentiment and trends information.
 * Uses clean architecture - returns resources directly, no wrappers.
 */
@RestController
@RequestMapping("/api/dashboard/market")
public class MarketSentimentController extends BaseController {

    private final MarketSentimentService marketSentimentService;
    
    public MarketSentimentController(MarketSentimentService marketSentimentService) {
        this.marketSentimentService = marketSentimentService;
    }
    
    /**
     * Get market sentiment data.
     * 
     * @param userId The user ID to get data for
     * @return Clean market sentiment data - no wrapper, let GlobalExceptionHandler handle errors
     */
    @GetMapping("/sentiment")
    public ResponseEntity<Map<String, Object>> getMarketSentiment(@RequestParam(required = false) String userId) {
        // Use a default user ID if not provided
        if (userId == null || userId.isEmpty()) {
            userId = "test-user";
        }
        
        log.info("Retrieving market sentiment for user: {}", userId);
        
        // Get data from service - let exceptions bubble up
        MarketSentimentResponse sentimentData = marketSentimentService.getMarketSentiment(userId);
        
        // Check if data exists
        if (sentimentData == null) {
            throw new StrategizException(DashboardErrorDetails.MARKET_DATA_UNAVAILABLE, "service-dashboard", "sentiment", "Data not found");
        }
        
        // Create response
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("marketSentiment", sentimentData);
        
        // Return clean response - headers added by StandardHeadersInterceptor
        return ResponseEntity.ok(responseData);
    }
    
    /**
     * Get market trends data.
     * 
     * @param userId The user ID to get data for
     * @return Clean market trends data - no wrapper, let GlobalExceptionHandler handle errors
     */
    @GetMapping("/trends")
    public ResponseEntity<Map<String, Object>> getMarketTrends(@RequestParam(required = false) String userId) {
        // Use a default user ID if not provided
        if (userId == null || userId.isEmpty()) {
            userId = "test-user";
        }
        
        log.info("Retrieving market trends for user: {}", userId);
        
        // Get data from service and extract trends - let exceptions bubble up
        MarketSentimentResponse sentimentData = marketSentimentService.getMarketSentiment(userId);
        
        // Check if data exists
        if (sentimentData == null || sentimentData.getMarketTrends() == null) {
            throw new StrategizException(DashboardErrorDetails.MARKET_DATA_UNAVAILABLE, "service-dashboard", "trends", "Data not found");
        }
        
        // Create response with just trends
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("marketTrends", sentimentData.getMarketTrends());
        
        // Return clean response - headers added by StandardHeadersInterceptor
        return ResponseEntity.ok(responseData);
    }
}
