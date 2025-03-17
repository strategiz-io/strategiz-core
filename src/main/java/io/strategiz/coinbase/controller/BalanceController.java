package io.strategiz.coinbase.controller;

import io.strategiz.coinbase.model.Account;
import io.strategiz.coinbase.service.CoinbaseService;
import io.strategiz.coinbase.service.firestore.FirestoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for Coinbase account balance endpoints
 */
@Slf4j
@RestController
@RequestMapping("/api/coinbase/balance")
public class BalanceController {

    private final CoinbaseService coinbaseService;
    private final FirestoreService firestoreService;

    @Autowired
    public BalanceController(CoinbaseService coinbaseService, 
                           @Autowired(required = false) FirestoreService firestoreService) {
        this.coinbaseService = coinbaseService;
        this.firestoreService = firestoreService;
    }

    /**
     * Get account balances for a user
     * @param userId User ID
     * @return Account balances with USD values
     */
    @GetMapping("/{userId}")
    public ResponseEntity<?> getAccountBalances(@PathVariable String userId) {
        try {
            // Get credentials from Firestore
            Map<String, String> credentials = firestoreService.getCoinbaseCredentials(userId);
            
            if (credentials == null) {
                Map<String, String> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "Coinbase API credentials not found for user");
                return ResponseEntity.badRequest().body(response);
            }
            
            String apiKey = credentials.get("apiKey");
            String secretKey = credentials.get("secretKey");
            
            // Get account balances
            List<Account> accounts = coinbaseService.getAccountBalances(apiKey, secretKey);
            
            // Calculate total USD value
            double totalUsdValue = coinbaseService.calculateTotalUsdValue(accounts);
            
            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("accounts", accounts);
            response.put("totalUsdValue", totalUsdValue);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting Coinbase account balances: {}", e.getMessage());
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Error getting Coinbase account balances: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Test Coinbase API connection
     * @param userId User ID
     * @return Test results
     */
    @GetMapping("/test/{userId}")
    public ResponseEntity<?> testConnection(@PathVariable String userId) {
        try {
            // Get credentials from Firestore
            Map<String, String> credentials = firestoreService.getCoinbaseCredentials(userId);
            
            if (credentials == null) {
                Map<String, String> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "Coinbase API credentials not found for user");
                return ResponseEntity.badRequest().body(response);
            }
            
            String apiKey = credentials.get("apiKey");
            String secretKey = credentials.get("secretKey");
            
            // Test connection
            boolean connectionSuccessful = coinbaseService.testConnection(apiKey, secretKey);
            
            Map<String, Object> testResult = new HashMap<>();
            testResult.put("status", connectionSuccessful ? "success" : "error");
            testResult.put("message", connectionSuccessful ? "Connection successful" : "Connection failed");
            testResult.put("connected", connectionSuccessful);
            
            return ResponseEntity.ok(testResult);
        } catch (Exception e) {
            log.error("Error testing Coinbase API connection: {}", e.getMessage());
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Error testing Coinbase API connection: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
