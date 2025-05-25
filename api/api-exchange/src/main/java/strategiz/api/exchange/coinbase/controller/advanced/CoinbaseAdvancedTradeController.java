package io.strategiz.api.exchange.coinbase.controller.advanced;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.strategiz.service.exchange.coinbase.advanced.CoinbaseAdvancedTradeService;

/**
 * Controller for Coinbase Advanced Trade API
 * Provides endpoints for advanced trading operations
 */
@RestController
@RequestMapping("/api/coinbase-advanced")
@CrossOrigin(origins = {"http://localhost:3000", "https://strategiz.io"}, allowedHeaders = "*")
public class CoinbaseAdvancedTradeController {
    @Autowired
    private CoinbaseAdvancedTradeService service;

    /**
     * Get raw account data from Coinbase Advanced Trade API
     * 
     * @param email User email
     * @return Raw account data
     */
    @GetMapping("/raw-account-data")
    public ResponseEntity<String> getRawAccountData(@RequestParam String email) {
        return service.getRawAccountData(email);
    }
}
