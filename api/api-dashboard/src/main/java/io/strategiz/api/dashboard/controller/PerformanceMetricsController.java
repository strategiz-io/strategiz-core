package io.strategiz.api.dashboard.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.strategiz.api.dashboard.mapper.PerformanceMetricsResponseBuilder;
import io.strategiz.api.dashboard.model.performancemetrics.PerformanceMetricsResponse;
import io.strategiz.service.base.model.BaseServiceResponse;
import io.strategiz.service.dashboard.PerformanceMetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for performance metrics data.
 * Provides endpoints for retrieving performance and return information.
 */
@RestController
@RequestMapping("/api/dashboard/performance")
public class PerformanceMetricsController extends DashboardBaseController {
    
    private static final Logger log = LoggerFactory.getLogger(PerformanceMetricsController.class);

    @Override
    public BaseServiceResponse process(Object request) {
        // Default implementation
        String userId = request instanceof String ? (String) request : "test-user";
        io.strategiz.service.dashboard.model.performancemetrics.PerformanceMetricsData serviceData = 
            performanceMetricsService.getPerformanceMetrics(userId);
        PerformanceMetricsResponse response = responseBuilder.buildPerformanceMetricsResponse(serviceData);
        response.setMetadata(convertMetadataToMap(createSuccessMetadata()));
        return response;
    }
    
    private final PerformanceMetricsService performanceMetricsService;
    private final PerformanceMetricsResponseBuilder responseBuilder;
    
    @Autowired
    public PerformanceMetricsController(PerformanceMetricsService performanceMetricsService, 
                                  PerformanceMetricsResponseBuilder responseBuilder) {
        this.performanceMetricsService = performanceMetricsService;
        this.responseBuilder = responseBuilder;
    }
    
    /**
     * Get performance metrics data for a user.
     * 
     * @param userId The user ID to get data for
     * @return ResponseEntity with PerformanceMetricsResponse
     */
    @GetMapping("/metrics")
    public ResponseEntity<?> getPerformanceMetrics(@RequestParam(required = false) String userId) {
        // Set response headers
        HttpHeaders responseHeaders = createJsonResponseHeaders();
        
        try {
            // Use a default user ID if not provided
            if (userId == null || userId.isEmpty()) {
                userId = "test-user";
            }
            
            // Get performance metrics from service
            io.strategiz.service.dashboard.model.performancemetrics.PerformanceMetricsData serviceData = 
                performanceMetricsService.getPerformanceMetrics(userId);
                
            // Convert to API response using the mapper
            PerformanceMetricsResponse dashboardResponse = responseBuilder.buildPerformanceMetricsResponse(serviceData);
            dashboardResponse.setMetadata(responseBuilder.convertMetadataToMap(createSuccessMetadata()));
            
            // Check if there is meaningful data to display
            if (dashboardResponse.getHistoricalValues() == null || dashboardResponse.getHistoricalValues().isEmpty()) {
                log.debug("No historical values found for user {}", userId);
                // Return 204 No Content if there's no data
                return new ResponseEntity<>(responseHeaders, HttpStatus.NO_CONTENT);
            }
            
            // Return successful response with headers
            return new ResponseEntity<>(dashboardResponse, responseHeaders, HttpStatus.OK);
        } catch (Exception e) {
            // Log the error
            log.error("Error retrieving performance metrics: {}", e.getMessage(), e);
            
            // Create error response
            PerformanceMetricsResponse errorResponse = new PerformanceMetricsResponse();
            errorResponse.setMetadata(responseBuilder.convertMetadataToMap(createErrorMetadata("PERFORMANCE_ERROR", e.getMessage())));
            
            // Return error response with headers
            return new ResponseEntity<>(errorResponse, responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
