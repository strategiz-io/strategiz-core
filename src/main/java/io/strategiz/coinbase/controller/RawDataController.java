package io.strategiz.coinbase.controller;

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
import java.util.Map;

/**
 * Controller for Coinbase raw data endpoints (admin only)
 */
@Slf4j
@RestController
@RequestMapping("/api/coinbase/raw-data")
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
    public ResponseEntity<?> getRawAccountData(@PathVariable String userId) {
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
            
            // Get raw account data
            Object rawData = coinbaseService.getRawAccountData(apiKey, secretKey);
            
            return ResponseEntity.ok(rawData);
        } catch (Exception e) {
            log.error("Error getting raw Coinbase account data: {}", e.getMessage());
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Error getting raw Coinbase account data: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
