package io.strategiz.api.dashboard.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.strategiz.api.dashboard.mapper.MarketSentimentResponseBuilder;
import io.strategiz.api.dashboard.model.marketsentiment.MarketSentimentResponse;
import io.strategiz.service.dashboard.MarketSentimentService;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller for market sentiment data.
 * Provides endpoints for retrieving market sentiment and trend information.
 */
@RestController
@RequestMapping("/api/dashboard/market")
@Slf4j
public class MarketSentimentController extends DashboardBaseController {
    
    @Override
    public MarketSentimentResponse process(Object request) {
        // Default implementation
        String userId = request instanceof String ? (String) request : "test-user";
        io.strategiz.service.dashboard.model.marketsentiment.MarketSentimentResponse serviceResponse = marketSentimentService.getMarketSentiment(userId);
        
        // Use the ResponseBuilder to create the API response
        MarketSentimentResponse response = responseBuilder.buildMarketSentimentResponse(serviceResponse);
        response.setMetadata(responseBuilder.convertMetadataToMap(createSuccessMetadata()));
        
        return response;
    }
    
    private final MarketSentimentService marketSentimentService;
    private final MarketSentimentResponseBuilder responseBuilder;
    
    @Autowired
    public MarketSentimentController(MarketSentimentService marketSentimentService, 
                               MarketSentimentResponseBuilder responseBuilder) {
        this.marketSentimentService = marketSentimentService;
        this.responseBuilder = responseBuilder;
    }
    
    /**
     * Get market sentiment data for a user.
     * 
     * @param userId The user ID to get data for
     * @return ResponseEntity with MarketSentimentResponse
     */
    @GetMapping("/sentiment")
    public ResponseEntity<?> getMarketSentiment(@RequestParam(required = false) String userId) {
        // Set response headers
        HttpHeaders responseHeaders = createJsonResponseHeaders();
        
        try {
            // Use a default user ID if not provided
            if (userId == null || userId.isEmpty()) {
                userId = "test-user";
            }
            
            // Get market sentiment data from service
            io.strategiz.service.dashboard.model.marketsentiment.MarketSentimentResponse serviceResponse = marketSentimentService.getMarketSentiment(userId);
            
            // Use the ResponseBuilder to create the API response
            MarketSentimentResponse dashboardResponse = responseBuilder.buildMarketSentimentResponse(serviceResponse);
            dashboardResponse.setMetadata(responseBuilder.convertMetadataToMap(createSuccessMetadata()));
            
            // Check if there is meaningful data to display
            if (dashboardResponse.getAssetSentiments() == null || dashboardResponse.getAssetSentiments().isEmpty()) {
                // Return 204 No Content if there's no data
                return new ResponseEntity<>(responseHeaders, HttpStatus.NO_CONTENT);
            }
            
            // Return successful response with headers
            return new ResponseEntity<>(dashboardResponse, responseHeaders, HttpStatus.OK);
        } catch (Exception e) {
            // Log the error
            log.error("Error retrieving market sentiment: {}", e.getMessage(), e);
            
            // Create error response
            MarketSentimentResponse errorResponse = new MarketSentimentResponse();
            errorResponse.setMetadata(responseBuilder.convertMetadataToMap(createErrorMetadata("MARKET_SENTIMENT_ERROR", e.getMessage())));

            
            // Return error response with headers
            return new ResponseEntity<>(errorResponse, responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
