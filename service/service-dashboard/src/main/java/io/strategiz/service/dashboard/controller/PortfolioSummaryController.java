package io.strategiz.service.dashboard.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.strategiz.service.base.controller.BaseApiController;
import io.strategiz.service.base.model.ApiResponseWrapper;
import io.strategiz.service.dashboard.PortfolioSummaryService;
import io.strategiz.service.dashboard.model.portfoliosummary.PortfolioSummaryResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for portfolio summary data.
 * Provides endpoints for retrieving portfolio summary information.
 */
@RestController
@RequestMapping("/api/dashboard/portfolio")
public class PortfolioSummaryController extends BaseApiController {
    
    private static final Logger log = LoggerFactory.getLogger(PortfolioSummaryController.class);
    
    private final PortfolioSummaryService portfolioSummaryService;
    
    public PortfolioSummaryController(PortfolioSummaryService portfolioSummaryService) {
        this.portfolioSummaryService = portfolioSummaryService;
    }
    
    /**
     * Get portfolio summary data for a user.
     * 
     * @param userId The user ID to get data for
     * @return ResponseEntity with portfolio summary data
     */
    @GetMapping("/summary")
    public ResponseEntity<ApiResponseWrapper<Map<String, Object>>> getPortfolioSummary(@RequestParam(required = false) String userId) {
        try {
            // Use a default user ID if not provided
            if (userId == null || userId.isEmpty()) {
                userId = "test-user";
            }
            
            log.info("Retrieving portfolio summary for user: {}", userId);
            
            // Get data from service
            PortfolioSummaryResponse portfolioSummary = portfolioSummaryService.getPortfolioSummary(userId);
            
            // Check if portfolio exists
            if (portfolioSummary == null) {
                return notFound("Portfolio", userId);
            }
            
            // Create response
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("portfolioSummary", portfolioSummary);
            
            // Return successful response
            return success(responseData);
                
        } catch (Exception e) {
            log.error("Error retrieving portfolio summary data", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR,
                "PORTFOLIO_SUMMARY_ERROR", 
                "Error retrieving portfolio summary data: " + e.getMessage());
        }
    }
}
