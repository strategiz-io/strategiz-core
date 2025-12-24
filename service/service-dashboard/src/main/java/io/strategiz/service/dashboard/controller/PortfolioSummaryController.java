package io.strategiz.service.dashboard.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.strategiz.framework.authorization.annotation.RequireAuth;
import io.strategiz.framework.authorization.annotation.AuthUser;
import io.strategiz.framework.authorization.context.AuthenticatedUser;
import io.strategiz.service.dashboard.service.PortfolioSummaryService;
import io.strategiz.service.dashboard.model.portfoliosummary.PortfolioSummaryResponse;
import io.strategiz.service.dashboard.exception.ServiceDashboardErrorDetails;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.base.controller.BaseController;

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
@RequireAuth(minAcr = "1")
public class PortfolioSummaryController extends BaseController {
    
    @Override
    protected String getModuleName() {
        return "service-dashboard";
    }
    
    private final PortfolioSummaryService portfolioSummaryService;
    
    public PortfolioSummaryController(PortfolioSummaryService portfolioSummaryService) {
        this.portfolioSummaryService = portfolioSummaryService;
    }
    
    /**
     * Get portfolio summary data for the authenticated user.
     *
     * @param user The authenticated user from token
     * @return Clean portfolio summary data - no wrapper, let GlobalExceptionHandler handle errors
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getPortfolioSummary(@AuthUser AuthenticatedUser user) {
        String userId = user.getUserId();
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
