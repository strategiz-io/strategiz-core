package io.strategiz.service.portfolio.controller;

import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.portfolio.constants.ServicePortfolioConstants;
import io.strategiz.service.portfolio.model.response.ProviderPortfolioResponse;
import io.strategiz.service.portfolio.service.KrakenPortfolioService;
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
 * REST controller for Kraken-specific portfolio operations.
 * Single Responsibility: Handles only Kraken portfolio endpoints.
 * Open/Closed: Can be extended for Kraken-specific features without modifying base logic.
 */
@RestController
@RequestMapping(ServicePortfolioConstants.BASE_PATH + "/providers/kraken")
@CrossOrigin(origins = "${strategiz.cors.allowed-origins:*}")
public class KrakenPortfolioController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(KrakenPortfolioController.class);

    private final KrakenPortfolioService krakenPortfolioService;
    private final SessionAuthBusiness sessionAuthBusiness;

    @Autowired
    public KrakenPortfolioController(
            KrakenPortfolioService krakenPortfolioService,
            SessionAuthBusiness sessionAuthBusiness) {
        this.krakenPortfolioService = krakenPortfolioService;
        this.sessionAuthBusiness = sessionAuthBusiness;
    }

    @Override
    protected String getModuleName() {
        return ServicePortfolioConstants.MODULE_NAME;
    }

    /**
     * Get Kraken portfolio data.
     * 
     * @param authHeader Authorization header with bearer token
     * @return Kraken portfolio data
     */
    @GetMapping
    public ResponseEntity<ProviderPortfolioResponse> getKrakenPortfolio(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        String userId = extractUserIdFromToken(authHeader);
        if (userId == null) {
            log.error("No valid authentication token provided for Kraken portfolio");
            throw new StrategizException(ErrorCode.AUTHENTICATION_ERROR, 
                ServicePortfolioConstants.ERROR_NO_AUTH);
        }
        
        log.info("Fetching Kraken portfolio for user: {}", userId);
        
        try {
            ProviderPortfolioResponse portfolioData = krakenPortfolioService.getKrakenPortfolio(userId);
            
            if (portfolioData == null) {
                log.warn("No Kraken data found for user {}", userId);
                throw new StrategizException(ErrorCode.RESOURCE_NOT_FOUND,
                    "Kraken portfolio not found");
            }
            
            return ResponseEntity.ok(portfolioData);
            
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching Kraken portfolio for user {}: {}", userId, e.getMessage(), e);
            throw new StrategizException(ErrorCode.INTERNAL_ERROR, 
                ServicePortfolioConstants.ERROR_PORTFOLIO_FETCH_FAILED);
        }
    }

    /**
     * Get Kraken account balances.
     * 
     * @param authHeader Authorization header with bearer token
     * @return Kraken account balances
     */
    @GetMapping("/balances")
    public ResponseEntity<Map<String, Object>> getKrakenBalances(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        String userId = extractUserIdFromToken(authHeader);
        if (userId == null) {
            log.error("No valid authentication token provided for Kraken balances");
            throw new StrategizException(ErrorCode.AUTHENTICATION_ERROR, 
                ServicePortfolioConstants.ERROR_NO_AUTH);
        }
        
        log.info("Fetching Kraken balances for user: {}", userId);
        
        try {
            Map<String, Object> balances = krakenPortfolioService.getKrakenBalances(userId);
            
            return ResponseEntity.ok(Map.of(
                "providerId", ServicePortfolioConstants.PROVIDER_KRAKEN,
                "balances", balances,
                "timestamp", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            log.error("Error fetching Kraken balances for user {}: {}", userId, e.getMessage(), e);
            throw new StrategizException(ErrorCode.INTERNAL_ERROR, 
                "Failed to fetch Kraken balances");
        }
    }

    /**
     * Get Kraken open positions.
     * 
     * @param authHeader Authorization header with bearer token
     * @return Kraken open positions
     */
    @GetMapping("/positions")
    public ResponseEntity<Map<String, Object>> getKrakenPositions(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        String userId = extractUserIdFromToken(authHeader);
        if (userId == null) {
            log.error("No valid authentication token provided for Kraken positions");
            throw new StrategizException(ErrorCode.AUTHENTICATION_ERROR, 
                ServicePortfolioConstants.ERROR_NO_AUTH);
        }
        
        log.info("Fetching Kraken positions for user: {}", userId);
        
        try {
            Map<String, Object> positions = krakenPortfolioService.getKrakenPositions(userId);
            
            return ResponseEntity.ok(Map.of(
                "providerId", ServicePortfolioConstants.PROVIDER_KRAKEN,
                "positions", positions,
                "timestamp", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            log.error("Error fetching Kraken positions for user {}: {}", userId, e.getMessage(), e);
            throw new StrategizException(ErrorCode.INTERNAL_ERROR, 
                "Failed to fetch Kraken positions");
        }
    }

    /**
     * Refresh Kraken portfolio data.
     * 
     * @param authHeader Authorization header with bearer token
     * @return Refresh status
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshKrakenData(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        String userId = extractUserIdFromToken(authHeader);
        if (userId == null) {
            log.error("No valid authentication token provided for Kraken refresh");
            throw new StrategizException(ErrorCode.AUTHENTICATION_ERROR, 
                ServicePortfolioConstants.ERROR_NO_AUTH);
        }
        
        log.info("Refreshing Kraken data for user: {}", userId);
        
        try {
            boolean success = krakenPortfolioService.refreshKrakenData(userId);
            
            return ResponseEntity.ok(Map.of(
                "success", success,
                "providerId", ServicePortfolioConstants.PROVIDER_KRAKEN,
                "message", success ? "Kraken data refreshed successfully" : "Failed to refresh Kraken data",
                "timestamp", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            log.error("Error refreshing Kraken data for user {}: {}", userId, e.getMessage(), e);
            throw new StrategizException(ErrorCode.INTERNAL_ERROR, 
                "Failed to refresh Kraken data");
        }
    }

    /**
     * Get Kraken trade history.
     * 
     * @param authHeader Authorization header with bearer token
     * @param limit Optional limit for number of trades to return
     * @return Kraken trade history
     */
    @GetMapping("/trades")
    public ResponseEntity<Map<String, Object>> getKrakenTradeHistory(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "limit", required = false, defaultValue = "50") Integer limit) {
        
        String userId = extractUserIdFromToken(authHeader);
        if (userId == null) {
            log.error("No valid authentication token provided for Kraken trades");
            throw new StrategizException(ErrorCode.AUTHENTICATION_ERROR, 
                ServicePortfolioConstants.ERROR_NO_AUTH);
        }
        
        log.info("Fetching Kraken trade history for user: {}, limit: {}", userId, limit);
        
        try {
            Map<String, Object> trades = krakenPortfolioService.getKrakenTradeHistory(userId, limit);
            
            return ResponseEntity.ok(Map.of(
                "providerId", ServicePortfolioConstants.PROVIDER_KRAKEN,
                "trades", trades,
                "limit", limit,
                "timestamp", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            log.error("Error fetching Kraken trades for user {}: {}", userId, e.getMessage(), e);
            throw new StrategizException(ErrorCode.INTERNAL_ERROR, 
                "Failed to fetch Kraken trade history");
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