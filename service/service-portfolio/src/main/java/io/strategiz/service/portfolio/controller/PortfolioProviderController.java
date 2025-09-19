package io.strategiz.service.portfolio.controller;

import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.portfolio.constants.ServicePortfolioConstants;
import io.strategiz.service.portfolio.model.response.ProviderPortfolioResponse;
import io.strategiz.service.portfolio.service.PortfolioProviderService;
import io.strategiz.business.tokenauth.SessionAuthBusiness;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.framework.exception.ErrorCode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * REST controller for provider-specific portfolio operations.
 * Single Responsibility: Handles individual provider portfolio endpoints.
 */
@RestController
@RequestMapping(ServicePortfolioConstants.BASE_PATH + ServicePortfolioConstants.PROVIDERS_PATH)
@CrossOrigin(origins = "${strategiz.cors.allowed-origins:*}")
public class PortfolioProviderController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(PortfolioProviderController.class);

    private final PortfolioProviderService portfolioProviderService;
    private final SessionAuthBusiness sessionAuthBusiness;

    @Autowired
    public PortfolioProviderController(
            PortfolioProviderService portfolioProviderService,
            SessionAuthBusiness sessionAuthBusiness) {
        this.portfolioProviderService = portfolioProviderService;
        this.sessionAuthBusiness = sessionAuthBusiness;
    }

    @Override
    protected String getModuleName() {
        return ServicePortfolioConstants.MODULE_NAME;
    }

    /**
     * Get portfolio data for a specific provider.
     * 
     * @param providerId Provider ID (kraken, coinbase, etc.)
     * @param authHeader Authorization header with bearer token
     * @return Provider portfolio data
     */
    @GetMapping("/{providerId}")
    public ResponseEntity<ProviderPortfolioResponse> getProviderPortfolio(
            @PathVariable String providerId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        String userId = extractUserIdFromToken(authHeader);
        if (userId == null) {
            log.error("No valid authentication token provided for provider portfolio");
            throw new StrategizException(ErrorCode.AUTHENTICATION_ERROR, 
                ServicePortfolioConstants.ERROR_NO_AUTH);
        }
        
        log.info("Fetching {} portfolio for user: {}", providerId, userId);
        
        try {
            ProviderPortfolioResponse portfolioData = portfolioProviderService.getProviderPortfolio(userId, providerId);
            
            if (portfolioData == null) {
                log.warn("No data found for provider {} and user {}", providerId, userId);
                throw new StrategizException(ErrorCode.RESOURCE_NOT_FOUND,
                    ServicePortfolioConstants.ERROR_PROVIDER_NOT_FOUND);
            }
            
            return ResponseEntity.ok(portfolioData);
            
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching {} portfolio for user {}: {}", providerId, userId, e.getMessage(), e);
            throw new StrategizException(ErrorCode.INTERNAL_ERROR, 
                ServicePortfolioConstants.ERROR_PORTFOLIO_FETCH_FAILED);
        }
    }

    /**
     * Refresh portfolio data for a specific provider.
     * 
     * @param providerId Provider ID
     * @param authHeader Authorization header with bearer token
     * @return Refresh status
     */
    @PostMapping("/{providerId}" + ServicePortfolioConstants.REFRESH_PATH)
    public ResponseEntity<Map<String, Object>> refreshProviderData(
            @PathVariable String providerId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        String userId = extractUserIdFromToken(authHeader);
        if (userId == null) {
            log.error("No valid authentication token provided for provider refresh");
            throw new StrategizException(ErrorCode.AUTHENTICATION_ERROR, 
                ServicePortfolioConstants.ERROR_NO_AUTH);
        }
        
        log.info("Refreshing {} data for user: {}", providerId, userId);
        
        try {
            boolean success = portfolioProviderService.refreshProviderData(userId, providerId);
            
            return ResponseEntity.ok(Map.of(
                "success", success,
                "providerId", providerId,
                "message", success ? "Refresh initiated successfully" : "Failed to refresh data",
                "timestamp", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            log.error("Error refreshing {} data for user {}: {}", providerId, userId, e.getMessage(), e);
            throw new StrategizException(ErrorCode.INTERNAL_ERROR, 
                "Failed to refresh provider data");
        }
    }

    /**
     * Extract user ID from the authorization token
     */
    private String extractUserIdFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        
        String token = authHeader.substring(7);
        Optional<String> userIdOpt = sessionAuthBusiness.validateSession(token);
        return userIdOpt.orElse(null);
    }
}