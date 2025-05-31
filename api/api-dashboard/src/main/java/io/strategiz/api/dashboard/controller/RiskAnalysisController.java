package io.strategiz.api.dashboard.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.strategiz.api.dashboard.model.riskanalysis.RiskAnalysisResponse;
import io.strategiz.api.dashboard.mapper.RiskAnalysisResponseBuilder;
import io.strategiz.service.dashboard.RiskAnalysisService;
import io.strategiz.service.dashboard.model.riskanalysis.RiskAnalysisData;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller for risk analysis data.
 * Provides endpoints for retrieving risk metrics and analysis information.
 */
@RestController
@RequestMapping("/api/dashboard/risk")
@Slf4j
public class RiskAnalysisController extends DashboardBaseController {
    
    @Override
    public RiskAnalysisResponse process(Object request) {
        // Default implementation
        String userId = request instanceof String ? (String) request : "test-user";
        RiskAnalysisData serviceData = riskAnalysisService.getRiskAnalysis(userId);
        
        // Use the ResponseBuilder to create the API response
        RiskAnalysisResponse response = responseBuilder.buildRiskAnalysisResponse(serviceData);
        response.setMetadata(responseBuilder.convertMetadataToMap(createSuccessMetadata()));
        
        return response;
    }
    
    private final RiskAnalysisService riskAnalysisService;
    private final RiskAnalysisResponseBuilder responseBuilder;
    
    @Autowired
    public RiskAnalysisController(RiskAnalysisService riskAnalysisService, 
                            RiskAnalysisResponseBuilder responseBuilder) {
        this.riskAnalysisService = riskAnalysisService;
        this.responseBuilder = responseBuilder;
    }
    
    /**
     * Get risk analysis data for a user.
     * 
     * @param userId The user ID to get data for
     * @return ResponseEntity with RiskAnalysisResponse
     */
    @GetMapping("/analysis")
    public ResponseEntity<?> getRiskAnalysis(@RequestParam(required = false) String userId) {
        // Set response headers
        HttpHeaders responseHeaders = createJsonResponseHeaders();
        
        try {
            // Use a default user ID if not provided
            if (userId == null || userId.isEmpty()) {
                userId = "test-user";
            }
            
            // Get risk analysis data from service
            RiskAnalysisData serviceData = riskAnalysisService.getRiskAnalysis(userId);
                
            // Use the ResponseBuilder to create the API response
            RiskAnalysisResponse dashboardResponse = responseBuilder.buildRiskAnalysisResponse(serviceData);
            dashboardResponse.setMetadata(responseBuilder.convertMetadataToMap(createSuccessMetadata()));
            
            // Check if there is meaningful data to display
            if (dashboardResponse.getVolatilityMetric() == null && dashboardResponse.getDiversificationMetric() == null) {
                // Return 204 No Content if there's no data
                return new ResponseEntity<>(responseHeaders, HttpStatus.NO_CONTENT);
            }
            
            // Return successful response with headers
            return new ResponseEntity<>(dashboardResponse, responseHeaders, HttpStatus.OK);
        } catch (Exception e) {
            // Log the error
            log.error("Error retrieving risk analysis: {}", e.getMessage(), e);
            
            // Create error response
            RiskAnalysisResponse errorResponse = new RiskAnalysisResponse();
            errorResponse.setMetadata(responseBuilder.convertMetadataToMap(createErrorMetadata("RISK_ANALYSIS_ERROR", e.getMessage())));
            
            // Return error response with headers
            return new ResponseEntity<>(errorResponse, responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
