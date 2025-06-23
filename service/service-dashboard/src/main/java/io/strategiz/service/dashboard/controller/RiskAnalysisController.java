package io.strategiz.service.dashboard.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.strategiz.service.base.controller.BaseApiController;
import io.strategiz.service.base.model.ApiResponseWrapper;
import io.strategiz.service.dashboard.RiskAnalysisService;
import io.strategiz.service.dashboard.model.riskanalysis.RiskAnalysisData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for risk analysis data.
 * Provides endpoints for retrieving risk metrics and analysis information.
 */
@RestController
@RequestMapping("/api/dashboard/risk")
public class RiskAnalysisController extends BaseApiController {
    
    private static final Logger log = LoggerFactory.getLogger(RiskAnalysisController.class);
    
    private final RiskAnalysisService riskAnalysisService;
    
    public RiskAnalysisController(RiskAnalysisService riskAnalysisService) {
        this.riskAnalysisService = riskAnalysisService;
    }
    
    /**
     * Get risk analysis data for a user.
     * 
     * @param userId The user ID to get data for
     * @return ResponseEntity with risk analysis data
     */
    @GetMapping
    public ResponseEntity<ApiResponseWrapper<Map<String, Object>>> getRiskAnalysis(@RequestParam(required = false) String userId) {
        try {
            // Use a default user ID if not provided
            if (userId == null || userId.isEmpty()) {
                userId = "test-user";
            }
            
            log.info("Retrieving risk analysis for user: {}", userId);
            
            // Get data from service
            RiskAnalysisData riskData = riskAnalysisService.getRiskAnalysis(userId);
            
            // Check if there is risk data to display
            if (riskData == null) {
                return notFound("Risk Analysis", userId);
            }
            
            // Create response
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("riskAnalysis", riskData);
            
            // Return successful response
            return success(responseData);
                
        } catch (Exception e) {
            log.error("Error retrieving risk analysis data", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, 
                "RISK_ANALYSIS_ERROR", 
                "Error retrieving risk analysis data: " + e.getMessage());
        }
    }
}
