package io.strategiz.service.dashboard.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.strategiz.service.dashboard.PerformanceMetricsService;
import io.strategiz.service.dashboard.model.performancemetrics.PerformanceMetricsData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for performance metrics data.
 * Provides endpoints for retrieving performance metrics information.
 * Uses clean architecture - returns resources directly, no wrappers.
 */
@RestController
@RequestMapping("/api/dashboard/metrics")
public class PerformanceMetricsController {
    
    private static final Logger log = LoggerFactory.getLogger(PerformanceMetricsController.class);

    private final PerformanceMetricsService performanceMetricsService;
    
    public PerformanceMetricsController(PerformanceMetricsService performanceMetricsService) {
        this.performanceMetricsService = performanceMetricsService;
    }
    
    /**
     * Get performance metrics data for a user.
     * 
     * @param userId The user ID to get data for
     * @return Clean performance metrics data - no wrapper, let GlobalExceptionHandler handle errors
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getPerformanceMetrics(@RequestParam(required = false) String userId) {
        // Use a default user ID if not provided
        if (userId == null || userId.isEmpty()) {
            userId = "test-user";
        }
        
        log.info("Retrieving performance metrics for user: {}", userId);
        
        // Get data from service - let exceptions bubble up
        PerformanceMetricsData performanceMetrics = performanceMetricsService.getPerformanceMetrics(userId);
        
        // Check if data exists
        if (performanceMetrics == null) {
            throw new RuntimeException("Performance metrics not found for user: " + userId);
        }
        
        // Create response
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("performanceMetrics", performanceMetrics);
        
        // Return clean response - headers added by StandardHeadersInterceptor
        return ResponseEntity.ok(responseData);
    }
}
