package io.strategiz.service.portfolio.controller;

import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.portfolio.service.CoinbasePortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import io.strategiz.business.tokenauth.SessionAuthBusiness;

/**
 * Controller for fetching Coinbase portfolio data and crypto holdings
 */
@RestController
@RequestMapping("/v1/portfolios/providers/coinbase")
@Tag(name = "Coinbase Portfolio", description = "Fetch Coinbase account data and crypto holdings")
public class CoinbasePortfolioController extends BaseController {
    
    private static final Logger log = LoggerFactory.getLogger(CoinbasePortfolioController.class);
    
    private final CoinbasePortfolioService coinbasePortfolioService;
    private final SessionAuthBusiness sessionAuthBusiness;
    
    @Autowired
    public CoinbasePortfolioController(CoinbasePortfolioService coinbasePortfolioService,
                                       SessionAuthBusiness sessionAuthBusiness) {
        this.coinbasePortfolioService = coinbasePortfolioService;
        this.sessionAuthBusiness = sessionAuthBusiness;
    }
    
    /**
     * Validate access token and get current user info
     */
    @GetMapping("/connection")
    @Operation(summary = "Get Coinbase connection status", 
               description = "Validates the stored access token and returns connection status with user info")
    public ResponseEntity<Map<String, Object>> validateConnection(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Extract user ID from token
            String userId = extractUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Unauthorized: No valid session found"
                ));
            }
            log.info("Validating Coinbase connection for user: {}", userId);
            
            Map<String, Object> result = coinbasePortfolioService.validateConnection(userId);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error validating Coinbase connection", e);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "Connection validation failed: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Get portfolio data with all holdings and balances
     */
    @GetMapping("/accounts")
    @Operation(summary = "Get Coinbase accounts", 
               description = "Fetches all accounts with current balances, holdings, and profit/loss")
    public ResponseEntity<Map<String, Object>> getPortfolioData(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String userId = extractUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Unauthorized: No valid session found"
                ));
            }
            log.info("Fetching Coinbase portfolio data for user: {}", userId);
            
            Map<String, Object> portfolioData = coinbasePortfolioService.getPortfolioData(userId);
            return ResponseEntity.ok(portfolioData);
            
        } catch (Exception e) {
            log.error("Error fetching Coinbase portfolio data", e);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "Failed to fetch portfolio data: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Get detailed account holdings with profit/loss
     */
    @GetMapping("/holdings")
    @Operation(summary = "Get crypto holdings with P&L", 
               description = "Fetches detailed holdings with profit/loss calculations")
    public ResponseEntity<Map<String, Object>> getHoldings(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String userId = extractUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Unauthorized: No valid session found"
                ));
            }
            log.info("Fetching Coinbase holdings for user: {}", userId);
            
            Map<String, Object> holdings = coinbasePortfolioService.getHoldingsWithProfits(userId);
            return ResponseEntity.ok(holdings);
            
        } catch (Exception e) {
            log.error("Error fetching Coinbase holdings", e);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "Failed to fetch holdings: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Get recent transactions
     */
    @GetMapping("/transactions")
    @Operation(summary = "Get recent transactions", 
               description = "Fetches recent transactions across all accounts")
    public ResponseEntity<Map<String, Object>> getTransactions(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(defaultValue = "50") int limit) {
        try {
            String userId = extractUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Unauthorized: No valid session found"
                ));
            }
            log.info("Fetching Coinbase transactions for user: {}", userId);
            
            Map<String, Object> transactions = coinbasePortfolioService.getRecentTransactions(userId, limit);
            return ResponseEntity.ok(transactions);
            
        } catch (Exception e) {
            log.error("Error fetching Coinbase transactions", e);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "Failed to fetch transactions: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Get real-time prices for specific assets
     */
    @GetMapping("/prices")
    @Operation(summary = "Get current prices", 
               description = "Get current prices for crypto assets")
    public ResponseEntity<Map<String, Object>> getPrices(
            @RequestParam(required = false) String symbols,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Prices endpoint doesn't require authentication but we log if user is authenticated
            String userId = extractUserIdFromToken(authHeader);
            log.info("Fetching prices for symbols: {} (user: {})", symbols, userId != null ? userId : "anonymous");
            
            Map<String, Object> prices = coinbasePortfolioService.getCurrentPrices(symbols);
            return ResponseEntity.ok(prices);
            
        } catch (Exception e) {
            log.error("Error fetching prices", e);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "Failed to fetch prices: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Extract user ID from the authorization token
     */
    private String extractUserIdFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        
        String token = authHeader.substring(7); // Remove "Bearer " prefix
        Optional<String> userIdOpt = sessionAuthBusiness.validateSession(token);
        return userIdOpt.orElse(null);
    }
    
    @Override
    protected String getModuleName() {
        return "service-portfolio";
    }
}