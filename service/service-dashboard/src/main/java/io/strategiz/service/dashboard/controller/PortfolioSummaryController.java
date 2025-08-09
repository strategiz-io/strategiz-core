package io.strategiz.service.dashboard.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.strategiz.service.dashboard.PortfolioSummaryService;
import io.strategiz.service.dashboard.model.portfoliosummary.PortfolioSummaryResponse;
import io.strategiz.service.dashboard.exception.ServiceDashboardErrorDetails;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.base.constants.ModuleConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for portfolio summary data.
 * Provides endpoints for retrieving portfolio summary information.
 * Uses clean architecture - returns resources directly, no wrappers.
 */
@RestController
@RequestMapping("/v1/dashboard/portfolio")
public class PortfolioSummaryController extends BaseController {
    
    @Override
    protected String getModuleName() {
        return ModuleConstants.DASHBOARD_MODULE;
    }
    
    private final PortfolioSummaryService portfolioSummaryService;
    
    public PortfolioSummaryController(PortfolioSummaryService portfolioSummaryService) {
        this.portfolioSummaryService = portfolioSummaryService;
    }
    
    /**
     * Get portfolio summary data for a user.
     * 
     * @param userId The user ID to get data for
     * @return Clean portfolio summary data - no wrapper, let GlobalExceptionHandler handle errors
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getPortfolioSummary(@RequestParam(required = false) String userId) {
        // Use a default user ID if not provided
        if (userId == null || userId.isEmpty()) {
            userId = "test-user";
        }
        
        log.info("Retrieving portfolio summary for user: {}", userId);
        
        // Get data from service - let exceptions bubble up
        PortfolioSummaryResponse portfolioSummary = portfolioSummaryService.getPortfolioSummary(userId);
        
        // Check if portfolio exists
        if (portfolioSummary == null) {
            throw new StrategizException(ServiceDashboardErrorDetails.PORTFOLIO_NOT_FOUND, "service-dashboard", userId);
        }
        
        // Create response
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("portfolioSummary", portfolioSummary);
        
        // Return clean response - headers added by StandardHeadersInterceptor
        return ResponseEntity.ok(responseData);
    }
}
