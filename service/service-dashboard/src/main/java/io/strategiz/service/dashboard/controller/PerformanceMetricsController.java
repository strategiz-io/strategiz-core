package io.strategiz.service.dashboard.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.strategiz.service.base.controller.BaseApiController;
import io.strategiz.service.base.model.ApiResponseWrapper;
import io.strategiz.service.dashboard.PerformanceMetricsService;
import io.strategiz.service.dashboard.model.performancemetrics.PerformanceMetricsData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for performance metrics data.
 * Provides endpoints for retrieving performance metrics information.
 */
@RestController
@RequestMapping("/api/dashboard/performance")
public class PerformanceMetricsController extends BaseApiController {
    
    private static final Logger log = LoggerFactory.getLogger(PerformanceMetricsController.class);
    
    private final PerformanceMetricsService performanceMetricsService;
    
    public PerformanceMetricsController(PerformanceMetricsService performanceMetricsService) {
        this.performanceMetricsService = performanceMetricsService;
    }
    
    /**
     * Get performance metrics data for a user.
     * 
     * @param userId The user ID to get data for
     * @return ResponseEntity with performance metrics data
     */
    @GetMapping
    public ResponseEntity<ApiResponseWrapper<Map<String, Object>>> getPerformanceMetrics(@RequestParam(required = false) String userId) {
        try {
            // Use a default user ID if not provided
            if (userId == null || userId.isEmpty()) {
                userId = "test-user";
            }
            
            log.info("Retrieving performance metrics for user: {}", userId);
            
            // Get data from service
            PerformanceMetricsData metricsData = performanceMetricsService.getPerformanceMetrics(userId);
            
            // Check if data is available
            if (metricsData == null) {
                return notFound("Performance Metrics", userId);
            }
            
            // Create response
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("performanceMetrics", metricsData);
            
            // Return successful response
            return success(responseData);
                
        } catch (Exception e) {
            log.error("Error retrieving performance metrics data", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, 
                "PERFORMANCE_METRICS_ERROR", 
                "Error retrieving performance metrics data: " + e.getMessage());
        }
    }
}
