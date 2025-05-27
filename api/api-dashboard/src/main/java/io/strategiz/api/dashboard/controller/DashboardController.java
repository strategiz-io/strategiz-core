package io.strategiz.api.dashboard.controller;

import io.americanexpress.synapse.service.rest.controller.BaseController;
import io.strategiz.api.dashboard.model.AssetAllocationResponse;
import io.strategiz.api.dashboard.model.DashboardResponse;
import io.strategiz.api.dashboard.model.MarketSentimentResponse;
import io.strategiz.api.dashboard.model.PerformanceMetricsResponse;
import io.strategiz.api.dashboard.model.PortfolioSummaryResponse;
import io.strategiz.api.dashboard.model.RiskAnalysisResponse;
import io.strategiz.api.dashboard.model.WatchlistResponse;
import io.strategiz.service.dashboard.AssetAllocationService;
import io.strategiz.service.dashboard.DashboardService;
import io.strategiz.service.dashboard.MarketSentimentService;
import io.strategiz.service.dashboard.PerformanceMetricsService;
import io.strategiz.service.dashboard.RiskAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller for dashboard operations.
 * Exposes endpoints for retrieving dashboard data.
 * Implements Synapse BaseController pattern.
 */
@Slf4j
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController extends BaseController {

    private final DashboardService dashboardService;
    private final AssetAllocationService assetAllocationService;
    private final PerformanceMetricsService performanceMetricsService;
    private final RiskAnalysisService riskAnalysisService;
    private final MarketSentimentService marketSentimentService;

    @Autowired
    public DashboardController(
            DashboardService dashboardService,
            AssetAllocationService assetAllocationService,
            PerformanceMetricsService performanceMetricsService,
            RiskAnalysisService riskAnalysisService,
            MarketSentimentService marketSentimentService) {
        this.dashboardService = dashboardService;
        this.assetAllocationService = assetAllocationService;
        this.performanceMetricsService = performanceMetricsService;
        this.riskAnalysisService = riskAnalysisService;
        this.marketSentimentService = marketSentimentService;
    }

    /**
     * Gets all dashboard data for the authenticated user following Synapse patterns
     * 
     * @param authHeader Authorization header containing Firebase ID token
     * @return Dashboard data response
     */
    @GetMapping(value = "", produces = "application/json")
    public ResponseEntity<DashboardResponse> getDashboardData(@RequestHeader("Authorization") String authHeader) {
        // Create HTTP headers with correlation ID for tracking
        HttpHeaders responseHeaders = getResponseHeaders();
        
        try {
            // In a real implementation, you would extract the user ID from the auth token
            // For simplicity in this example, we'll use a fixed user ID
            String userId = "test-user";
            
            // Get dashboard data from service
            Map<String, Object> dashboardData = dashboardService.getDashboardData(userId);
            
            // Convert to Synapse response model
            DashboardResponse response = convertToDashboardResponse(dashboardData);
            response.setMetadata(createSuccessMetadata());
            
            // Return successful response with headers
            return new ResponseEntity<>(response, responseHeaders, HttpStatus.OK);
        } catch (Exception e) {
            // Log the error
            log.error("Error retrieving dashboard data: {}", e.getMessage(), e);
            
            // Create error response following Synapse patterns
            DashboardResponse errorResponse = new DashboardResponse();
            errorResponse.setMetadata(createErrorMetadata("DASHBOARD_ERROR", e.getMessage()));
            
            // Return error response with headers
            return new ResponseEntity<>(errorResponse, responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Gets portfolio summary data for the authenticated user
     * 
     * @param authHeader Authorization header containing Firebase ID token
     * @return Portfolio summary response
     */
    @GetMapping(value = "/portfolio", produces = "application/json")
    public ResponseEntity<PortfolioSummaryResponse> getPortfolioSummary(@RequestHeader("Authorization") String authHeader) {
        // Create HTTP headers with correlation ID for tracking
        HttpHeaders responseHeaders = getResponseHeaders();
        
        try {
            // In a real implementation, you would extract the user ID from the auth token
            String userId = "test-user";
            
            // Get portfolio data from service
            PortfolioSummaryResponse response = dashboardService.getPortfolioSummary(userId);
            response.setMetadata(createSuccessMetadata());
            
            // Return successful response with headers
            return new ResponseEntity<>(response, responseHeaders, HttpStatus.OK);
        } catch (Exception e) {
            // Log the error
            log.error("Error retrieving portfolio summary: {}", e.getMessage(), e);
            
            // Create error response following Synapse patterns
            PortfolioSummaryResponse errorResponse = new PortfolioSummaryResponse();
            errorResponse.setMetadata(createErrorMetadata("PORTFOLIO_ERROR", e.getMessage()));
            
            // Return error response with headers
            return new ResponseEntity<>(errorResponse, responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Gets watchlist data for the authenticated user
     * 
     * @param authHeader Authorization header containing Firebase ID token
     * @return Watchlist response
     */
    @GetMapping(value = "/watchlist", produces = "application/json")
    public ResponseEntity<WatchlistResponse> getWatchlist(@RequestHeader("Authorization") String authHeader) {
        // Create HTTP headers with correlation ID for tracking
        HttpHeaders responseHeaders = getResponseHeaders();
        
        try {
            // In a real implementation, you would extract the user ID from the auth token
            String userId = "test-user";
            
            // Get watchlist data from service
            WatchlistResponse response = dashboardService.getWatchlist(userId);
            response.setMetadata(createSuccessMetadata());
            
            // Return successful response with headers
            return new ResponseEntity<>(response, responseHeaders, HttpStatus.OK);
        } catch (Exception e) {
            // Log the error
            log.error("Error retrieving watchlist: {}", e.getMessage(), e);
            
            // Create error response following Synapse patterns
            WatchlistResponse errorResponse = new WatchlistResponse();
            errorResponse.setMetadata(createErrorMetadata("WATCHLIST_ERROR", e.getMessage()));
            
            // Return error response with headers
            return new ResponseEntity<>(errorResponse, responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Gets asset allocation data for the authenticated user
     * 
     * @param authHeader Authorization header containing Firebase ID token
     * @return Asset allocation response
     */
    @GetMapping(value = "/asset-allocation", produces = "application/json")
    public ResponseEntity<AssetAllocationResponse> getAssetAllocation(@RequestHeader("Authorization") String authHeader) {
        // Create HTTP headers with correlation ID for tracking
        HttpHeaders responseHeaders = getResponseHeaders();
        
        try {
            // In a real implementation, you would extract the user ID from the auth token
            String userId = "test-user";
            
            // Get asset allocation data from service
            AssetAllocationResponse response = assetAllocationService.getAssetAllocation(userId);
            response.setMetadata(createSuccessMetadata());
            
            // Return successful response with headers
            return new ResponseEntity<>(response, responseHeaders, HttpStatus.OK);
        } catch (Exception e) {
            // Log the error
            log.error("Error retrieving asset allocation: {}", e.getMessage(), e);
            
            // Create error response following Synapse patterns
            AssetAllocationResponse errorResponse = new AssetAllocationResponse();
            errorResponse.setMetadata(createErrorMetadata("ASSET_ALLOCATION_ERROR", e.getMessage()));
            
            // Return error response with headers
            return new ResponseEntity<>(errorResponse, responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Gets performance metrics for the authenticated user
     * 
     * @param authHeader Authorization header containing Firebase ID token
     * @return Performance metrics response
     */
    @GetMapping(value = "/performance", produces = "application/json")
    public ResponseEntity<PerformanceMetricsResponse> getPerformanceMetrics(@RequestHeader("Authorization") String authHeader) {
        // Create HTTP headers with correlation ID for tracking
        HttpHeaders responseHeaders = getResponseHeaders();
        
        try {
            // In a real implementation, you would extract the user ID from the auth token
            String userId = "test-user";
            
            // Get performance metrics data from service
            PerformanceMetricsResponse response = performanceMetricsService.getPerformanceMetrics(userId);
            response.setMetadata(createSuccessMetadata());
            
            // Return successful response with headers
            return new ResponseEntity<>(response, responseHeaders, HttpStatus.OK);
        } catch (Exception e) {
            // Log the error
            log.error("Error retrieving performance metrics: {}", e.getMessage(), e);
            
            // Create error response following Synapse patterns
            PerformanceMetricsResponse errorResponse = new PerformanceMetricsResponse();
            errorResponse.setMetadata(createErrorMetadata("PERFORMANCE_METRICS_ERROR", e.getMessage()));
            
            // Return error response with headers
            return new ResponseEntity<>(errorResponse, responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Gets risk analysis for the authenticated user
     * 
     * @param authHeader Authorization header containing Firebase ID token
     * @return Risk analysis response
     */
    @GetMapping(value = "/risk", produces = "application/json")
    public ResponseEntity<RiskAnalysisResponse> getRiskAnalysis(@RequestHeader("Authorization") String authHeader) {
        // Create HTTP headers with correlation ID for tracking
        HttpHeaders responseHeaders = getResponseHeaders();
        
        try {
            // In a real implementation, you would extract the user ID from the auth token
            String userId = "test-user";
            
            // Get risk analysis data from service
            RiskAnalysisResponse response = riskAnalysisService.getRiskAnalysis(userId);
            response.setMetadata(createSuccessMetadata());
            
            // Return successful response with headers
            return new ResponseEntity<>(response, responseHeaders, HttpStatus.OK);
        } catch (Exception e) {
            // Log the error
            log.error("Error retrieving risk analysis: {}", e.getMessage(), e);
            
            // Create error response following Synapse patterns
            RiskAnalysisResponse errorResponse = new RiskAnalysisResponse();
            errorResponse.setMetadata(createErrorMetadata("RISK_ANALYSIS_ERROR", e.getMessage()));
            
            // Return error response with headers
            return new ResponseEntity<>(errorResponse, responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Gets market sentiment data for the authenticated user
     * 
     * @param authHeader Authorization header containing Firebase ID token
     * @return Market sentiment response
     */
    @GetMapping(value = "/sentiment", produces = "application/json")
    public ResponseEntity<MarketSentimentResponse> getMarketSentiment(@RequestHeader("Authorization") String authHeader) {
        // Create HTTP headers with correlation ID for tracking
        HttpHeaders responseHeaders = getResponseHeaders();
        
        try {
            // In a real implementation, you would extract the user ID from the auth token
            String userId = "test-user";
            
            // Get market sentiment data from service
            MarketSentimentResponse response = marketSentimentService.getMarketSentiment(userId);
            response.setMetadata(createSuccessMetadata());
            
            // Return successful response with headers
            return new ResponseEntity<>(response, responseHeaders, HttpStatus.OK);
        } catch (Exception e) {
            // Log the error
            log.error("Error retrieving market sentiment: {}", e.getMessage(), e);
            
            // Create error response following Synapse patterns
            MarketSentimentResponse errorResponse = new MarketSentimentResponse();
            errorResponse.setMetadata(createErrorMetadata("MARKET_SENTIMENT_ERROR", e.getMessage()));
            
            // Return error response with headers
            return new ResponseEntity<>(errorResponse, responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Converts the raw dashboard data to a structured DashboardResponse
     * 
     * @param dashboardData Raw dashboard data from service
     * @return Structured DashboardResponse object
     */
    private DashboardResponse convertToDashboardResponse(Map<String, Object> dashboardData) {
        // Create response object
        DashboardResponse response = new DashboardResponse();
        
        // For now, this is just a placeholder implementation
        // In a real implementation, you would map the dashboardData to the DashboardResponse fields
        // This would involve parsing the Map and creating the appropriate objects
        
        return response;
    }
}
