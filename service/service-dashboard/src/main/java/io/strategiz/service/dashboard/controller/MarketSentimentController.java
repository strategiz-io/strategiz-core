package io.strategiz.service.dashboard.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.strategiz.service.base.controller.BaseApiController;
import io.strategiz.service.base.model.ApiResponseWrapper;
import io.strategiz.service.dashboard.MarketSentimentService;
import io.strategiz.service.dashboard.model.marketsentiment.MarketSentimentResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for market sentiment data.
 * Provides endpoints for retrieving market sentiment and trend information.
 */
@RestController
@RequestMapping("/api/dashboard/market")
public class MarketSentimentController extends BaseApiController {
    
    private static final Logger log = LoggerFactory.getLogger(MarketSentimentController.class);
    
    private final MarketSentimentService marketSentimentService;
    
    public MarketSentimentController(MarketSentimentService marketSentimentService) {
        this.marketSentimentService = marketSentimentService;
    }
    
    /**
     * Get market sentiment data for a user.
     * 
     * @param userId The user ID to get data for
     * @return ResponseEntity with market sentiment data
     */
    @GetMapping("/sentiment")
    public ResponseEntity<ApiResponseWrapper<Map<String, Object>>> getMarketSentiment(@RequestParam(required = false) String userId) {
        try {
            // Use a default user ID if not provided
            if (userId == null || userId.isEmpty()) {
                userId = "test-user";
            }
            
            log.info("Retrieving market sentiment for user: {}", userId);
            
            // Get market sentiment data from service
            MarketSentimentResponse marketSentimentData = marketSentimentService.getMarketSentiment(userId);
            
            // Create response
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("marketSentiment", marketSentimentData);
            
            // Return successful response
            return success(responseData);
                
        } catch (Exception e) {
            log.error("Error retrieving market sentiment data", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, 
                "MARKET_SENTIMENT_ERROR", 
                "Error retrieving market sentiment data: " + e.getMessage());
        }
    }
    
    /**
     * Get market trends data for a user.
     * 
     * @param userId The user ID to get data for
     * @return ResponseEntity with market trends data
     */
    @GetMapping("/trends") 
    public ResponseEntity<ApiResponseWrapper<Map<String, Object>>> getMarketTrends(@RequestParam(required = false) String userId) {
        try {
            // Use a default user ID if not provided
            if (userId == null || userId.isEmpty()) {
                userId = "test-user";
            }
            
            log.info("Retrieving market trends for user: {}", userId);
            
            // Get market trends data from service
            // Note: If getMarketTrends doesn't exist in the service, you'll need to implement it
            // or modify this code to use a different method
            Map<String, Object> marketTrendsData = new HashMap<>(); // Placeholder for real data
            
            // Create response
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("marketTrends", marketTrendsData);
            
            // Return successful response
            return success(responseData);
                
        } catch (Exception e) {
            log.error("Error retrieving market trends data", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, 
                "MARKET_TRENDS_ERROR", 
                "Error retrieving market trends data: " + e.getMessage());
        }
    }
}
