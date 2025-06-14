package io.strategiz.api.portfolio.controller;

import io.strategiz.service.portfolio.PortfolioService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for portfolio management operations across all brokerages
 */
@RestController
@RequestMapping("/api/portfolio")
@CrossOrigin(origins = "*")
public class PortfolioController {

    private static final Logger log = LoggerFactory.getLogger(PortfolioController.class);

    private final PortfolioService portfolioService;

    @Autowired
    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    /**
     * Get portfolio data for a specific brokerage
     * 
     * @param email User email
     * @param provider Brokerage provider (e.g., "robinhood", "coinbase")
     * @return Portfolio data or error information
     */
    @GetMapping("/{provider}/data")
    public ResponseEntity<Object> getPortfolioData(
            @RequestParam String email,
            @PathVariable String provider) {
        
        log.info("HTTP request to get {} portfolio data for user: {}", provider, email);
        Map<String, Object> result = portfolioService.getBrokeragePortfolioData(email, provider);
        return ResponseEntity.ok(result);
    }
}
