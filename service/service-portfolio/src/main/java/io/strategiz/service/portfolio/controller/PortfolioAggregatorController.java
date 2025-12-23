package io.strategiz.service.portfolio.controller;

import io.strategiz.service.portfolio.service.PortfolioAggregatorService;
import io.strategiz.service.portfolio.constants.ServicePortfolioConstants;
import io.strategiz.service.portfolio.model.response.PortfolioSummaryResponse;
import io.strategiz.service.portfolio.model.response.PortfolioOverviewResponse;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.framework.authorization.annotation.RequireAuth;
import io.strategiz.framework.authorization.annotation.AuthUser;
import io.strategiz.framework.authorization.context.AuthenticatedUser;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.framework.exception.ErrorCode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for aggregated portfolio data across all connected providers.
 * This controller reads from stored provider_data in Firestore and returns
 * a unified view of the user's complete portfolio.
 */
@RestController
@RequestMapping(ServicePortfolioConstants.BASE_PATH)
@CrossOrigin(origins = "${strategiz.cors.allowed-origins:*}")
@RequireAuth(minAcr = "1")
public class PortfolioAggregatorController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(PortfolioAggregatorController.class);

    private final PortfolioAggregatorService portfolioAggregatorService;

    @Autowired
    public PortfolioAggregatorController(
            PortfolioAggregatorService portfolioAggregatorService) {
        this.portfolioAggregatorService = portfolioAggregatorService;
    }

    @Override
    protected String getModuleName() {
        return ServicePortfolioConstants.MODULE_NAME;
    }

    /**
     * Get lightweight portfolio summary for dashboard.
     * Returns only essential data for quick loading.
     *
     * @param user Authenticated user from HTTP-only cookie
     * @return Summary data with total value, day change, and top holdings
     */
    @GetMapping(ServicePortfolioConstants.SUMMARY_PATH)
    public ResponseEntity<PortfolioSummaryResponse> getPortfolioSummary(@AuthUser AuthenticatedUser user) {

        String userId = user.getUserId();
        log.info("Fetching portfolio summary for user: {}", userId);

        try {
            PortfolioSummaryResponse summaryData = portfolioAggregatorService.getPortfolioSummary(userId);
            return ResponseEntity.ok(summaryData);

        } catch (Exception e) {
            log.error("Error fetching portfolio summary for user {}: {}", userId, e.getMessage(), e);
            throw new StrategizException(ErrorCode.INTERNAL_ERROR,
                ServicePortfolioConstants.ERROR_PORTFOLIO_FETCH_FAILED);
        }
    }

    /**
     * Get complete portfolio overview for portfolio page.
     * This endpoint aggregates data from all connected providers.
     *
     * @param user Authenticated user from HTTP-only cookie
     * @return Full portfolio data including all providers and holdings
     */
    @GetMapping(ServicePortfolioConstants.OVERVIEW_PATH)
    public ResponseEntity<PortfolioOverviewResponse> getPortfolioOverview(@AuthUser AuthenticatedUser user) {

        String userId = user.getUserId();
        log.info("Fetching portfolio overview for user: {}", userId);

        try {
            PortfolioOverviewResponse portfolioData = portfolioAggregatorService.getPortfolioOverview(userId);

            // Log summary for debugging
            if (portfolioData.getProviders() != null) {
                int providerCount = portfolioData.getProviders().size();
                log.info("Returning portfolio data for user {} with {} provider(s)", userId, providerCount);
            }

            return ResponseEntity.ok(portfolioData);

        } catch (Exception e) {
            log.error("Error fetching portfolio overview for user {}: {}", userId, e.getMessage(), e);
            throw new StrategizException(ErrorCode.INTERNAL_ERROR,
                ServicePortfolioConstants.ERROR_PORTFOLIO_FETCH_FAILED);
        }
    }
}