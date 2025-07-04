package io.strategiz.service.dashboard.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.strategiz.service.dashboard.RiskAnalysisService;
import io.strategiz.service.dashboard.model.riskanalysis.RiskAnalysisData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for risk analysis data.
 * Provides endpoints for retrieving risk analysis information.
 * Uses clean architecture - returns resources directly, no wrappers.
 */
@RestController
@RequestMapping("/api/dashboard/risk")
public class RiskAnalysisController {
    
    private static final Logger log = LoggerFactory.getLogger(RiskAnalysisController.class);

    private final RiskAnalysisService riskAnalysisService;
    
    public RiskAnalysisController(RiskAnalysisService riskAnalysisService) {
        this.riskAnalysisService = riskAnalysisService;
    }
    
    /**
     * Get risk analysis data for a user.
     * 
     * @param userId The user ID to get data for
     * @return Clean risk analysis data - no wrapper, let GlobalExceptionHandler handle errors
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getRiskAnalysis(@RequestParam(required = false) String userId) {
        // Use a default user ID if not provided
        if (userId == null || userId.isEmpty()) {
            userId = "test-user";
        }
        
        log.info("Retrieving risk analysis for user: {}", userId);
        
        // Get data from service - let exceptions bubble up
        RiskAnalysisData riskAnalysis = riskAnalysisService.getRiskAnalysis(userId);
        
        // Check if data exists
        if (riskAnalysis == null) {
            throw new RuntimeException("Risk analysis not found for user: " + userId);
        }
        
        // Create response
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("riskAnalysis", riskAnalysis);
        
        // Return clean response - headers added by StandardHeadersInterceptor
        return ResponseEntity.ok(responseData);
    }
}
