package io.strategiz.coinbase.controller;

import io.strategiz.coinbase.service.CoinbaseService;
import io.strategiz.coinbase.service.firestore.FirestoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for Coinbase raw data endpoints (admin only)
 */
@Slf4j
@RestController
@RequestMapping("/api/coinbase/raw-data")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://strategiz.io"}, allowedHeaders = "*")
public class RawDataController {

    private final CoinbaseService coinbaseService;
    private final FirestoreService firestoreService;

    @Autowired
    public RawDataController(CoinbaseService coinbaseService, 
                           @Autowired(required = false) FirestoreService firestoreService) {
        this.coinbaseService = coinbaseService;
        this.firestoreService = firestoreService;
    }

    /**
     * Get raw account data for admin viewing
     * @param userId User ID
     * @return Raw account data from Coinbase API
     */
    @GetMapping("/accounts/{userId}")
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<?> getRawAccountData(@PathVariable String userId) {
        try {
            log.info("Received request for raw Coinbase data for user: {}", userId);
            
            // Get credentials from Firestore
            Map<String, String> credentials = firestoreService.getCoinbaseCredentials(userId);
            
            if (credentials == null) {
                log.warn("Coinbase API credentials not found for user: {}", userId);
                Map<String, String> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "Coinbase API credentials not found for user");
                return ResponseEntity.badRequest().body(response);
            }
            
            String apiKey = credentials.get("apiKey");
            String secretKey = credentials.get("secretKey");
            
            if (apiKey == null || secretKey == null) {
                log.warn("Incomplete Coinbase API credentials for user: {}", userId);
                Map<String, String> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "Incomplete Coinbase API credentials. Both API key and Secret key are required.");
                return ResponseEntity.badRequest().body(response);
            }
            
            log.info("Successfully retrieved Coinbase credentials for user: {}", userId);
            log.info("Making request to Coinbase API with key: {}", apiKey.substring(0, Math.min(5, apiKey.length())) + "...");
            
            try {
                // Get raw account data
                Object rawData = coinbaseService.getRawAccountData(apiKey, secretKey);
                
                log.info("Successfully retrieved raw Coinbase data");
                return ResponseEntity.ok(rawData);
            } catch (Exception e) {
                log.error("Error from Coinbase API: {}", e.getMessage(), e);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "Error from Coinbase API: " + e.getMessage());
                errorResponse.put("error", e.getClass().getSimpleName());
                return ResponseEntity.badRequest().body(errorResponse);
            }
        } catch (Exception e) {
            log.error("Unexpected error processing request: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Unexpected error: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
