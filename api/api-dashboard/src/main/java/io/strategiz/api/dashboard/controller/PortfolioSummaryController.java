package io.strategiz.api.dashboard.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.strategiz.api.dashboard.mapper.PortfolioSummaryResponseBuilder;
import io.strategiz.api.dashboard.model.portfoliosummary.PortfolioSummaryResponse;
import io.strategiz.service.base.model.BaseServiceResponse;
import io.strategiz.service.dashboard.DashboardService;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller for portfolio summary API endpoints.
 * Extends the DashboardBaseController for common functionality.
 */
@RestController
@RequestMapping("/api/dashboard/portfolio")
@Slf4j
public class PortfolioSummaryController extends DashboardBaseController {

    private final DashboardService dashboardService;
    private final PortfolioSummaryResponseBuilder responseBuilder;
    
    /**
     * Constructor with required dependencies.
     * 
     * @param dashboardService Service for dashboard operations
     * @param responseBuilder Builder for portfolio summary responses
     */
    @Autowired
    public PortfolioSummaryController(DashboardService dashboardService, PortfolioSummaryResponseBuilder responseBuilder) {
        this.dashboardService = dashboardService;
        this.responseBuilder = responseBuilder;
    }
    
    @Override
    public BaseServiceResponse process(Object request) {
        // Default implementation for the required abstract method
        // This is needed to satisfy the BaseController contract
        PortfolioSummaryResponse response = new PortfolioSummaryResponse();
        response.setMetadata(responseBuilder.convertMetadataToMap(createSuccessMetadata()));
        return response;
    }
    
    /**
     * Get portfolio summary data for a user.
     * 
     * @param userId The user ID to get data for
     * @return ResponseEntity with PortfolioSummaryResponse
     */
    @GetMapping("/summary")
    public ResponseEntity<?> getPortfolioSummary(@RequestParam(required = false) String userId) {
        // Set response headers
        HttpHeaders responseHeaders = createJsonResponseHeaders();
        
        try {
            // Use a default user ID if not provided
            if (userId == null || userId.isEmpty()) {
                userId = "test-user";
            }
            
            // Get portfolio data from service
            io.strategiz.service.dashboard.model.portfoliosummary.PortfolioSummaryResponse serviceResponse = 
                dashboardService.getPortfolioSummary(userId);
            
            // Use the builder to create and populate API response
            PortfolioSummaryResponse dashboardResponse = responseBuilder.buildPortfolioSummaryResponse(serviceResponse);
            
            // Set metadata using the builder
            dashboardResponse.setMetadata(responseBuilder.convertMetadataToMap(createSuccessMetadata()));
            
            // Return successful response with headers
            return new ResponseEntity<>(dashboardResponse, responseHeaders, HttpStatus.OK);
        } catch (Exception e) {
            // Log the error
            log.error("Error retrieving portfolio summary: {}", e.getMessage(), e);
            
            // Create error response
            PortfolioSummaryResponse errorResponse = new PortfolioSummaryResponse();
            errorResponse.setMetadata(responseBuilder.convertMetadataToMap(createErrorMetadata("PORTFOLIO_ERROR", e.getMessage())));
            
            // Return error response with headers
            return new ResponseEntity<>(errorResponse, responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
