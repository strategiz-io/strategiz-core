package io.strategiz.api.exchange.coinbase.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.strategiz.service.exchange.coinbase.admin.CoinbaseAdminDashboardService;

/**
 * Controller for Coinbase Admin Dashboard
 * Provides endpoints for admin operations related to Coinbase accounts
 */
@RestController
@RequestMapping("/api/coinbase-admin-dashboard")
@CrossOrigin(origins = {"http://localhost:3000", "https://strategiz.io"}, allowedHeaders = "*")
public class CoinbaseAdminDashboardController {
    @Autowired
    private CoinbaseAdminDashboardService service;

    /**
     * Get raw account data from Coinbase
     * @param email User email
     * @return Raw account data
     */
    @GetMapping("/raw-account-data")
    public ResponseEntity<String> getRawAccountData(@RequestParam String email) {
        return service.getRawAccountData(email);
    }

    /**
     * Get portfolio data from Coinbase
     * @param email User email
     * @return Portfolio data
     */
    @GetMapping("/portfolio-data")
    public ResponseEntity<String> getPortfolioData(@RequestParam String email) {
        return service.getPortfolioData(email);
    }

    /**
     * Check if Coinbase credentials are valid
     * @param email User email
     * @return Credential status
     */
    @GetMapping("/check-credentials")
    public ResponseEntity<String> checkCredentials(@RequestParam String email) {
        return service.checkCredentials(email);
    }
}
