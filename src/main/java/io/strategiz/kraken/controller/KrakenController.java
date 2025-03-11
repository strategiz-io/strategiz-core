package io.strategiz.kraken.controller;

import io.strategiz.kraken.model.KrakenAccount;
import io.strategiz.kraken.service.KrakenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for Kraken API
 */
@Slf4j
@RestController
@RequestMapping("/api/kraken")
public class KrakenController {

    private final KrakenService krakenService;

    @Autowired
    public KrakenController(KrakenService krakenService) {
        this.krakenService = krakenService;
    }

    /**
     * Configure Kraken API
     * @param request Request containing API key and secret key
     * @return Configuration status
     */
    @PostMapping("/configure")
    public ResponseEntity<Map<String, String>> configure(@RequestBody Map<String, String> request) {
        try {
            String apiKey = request.get("apiKey");
            String secretKey = request.get("secretKey");
            
            Map<String, String> result = krakenService.configure(apiKey, secretKey);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error configuring Kraken API", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Test Kraken API connection
     * @param request Request containing API key and secret key
     * @return Test results
     */
    @PostMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testConnection(@RequestBody Map<String, String> request) {
        try {
            String apiKey = request.get("apiKey");
            String secretKey = request.get("secretKey");
            
            Map<String, Object> result = krakenService.testConnection(apiKey, secretKey);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error testing Kraken API connection", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get raw account data from Kraken API
     * This endpoint returns the completely unmodified raw data from Kraken API
     * 
     * @param request Request containing API key and secret key
     * @return Raw account data from Kraken API
     */
    @PostMapping("/raw-data")
    @CrossOrigin(origins = {"http://localhost:3000", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Object> getRawAccountData(@RequestBody Map<String, String> request) {
        try {
            log.info("Received request for raw Kraken data");
            
            // For admin page, we'll simply use the provided API keys directly
            // This ensures we get the completely unmodified raw data from Kraken API
            String apiKey = request.get("apiKey");
            String secretKey = request.get("secretKey");

            if (apiKey == null || secretKey == null) {
                log.warn("Missing API key or secret key in request");
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "API Key and Secret Key are required"
                ));
            }
            
            // If using test keys, return sample data for admin page testing
            if ("YOUR_KRAKEN_API_KEY".equals(apiKey) && "YOUR_KRAKEN_SECRET_KEY".equals(secretKey)) {
                log.info("Using test keys, returning sample data for admin page");
                
                // Create a sample KrakenAccount with realistic data
                KrakenAccount sampleAccount = new KrakenAccount();
                
                // Create sample result with asset balances
                Map<String, Object> result = new HashMap<>();
                result.put("XXBT", "0.5432100000");  // Bitcoin
                result.put("XETH", "10.1234500000"); // Ethereum
                result.put("XXRP", "1000.0000000000"); // Ripple
                result.put("XLTC", "20.0000000000"); // Litecoin
                result.put("XXMR", "15.0000000000"); // Monero
                result.put("ZUSD", "5000.0000"); // USD
                result.put("ZEUR", "4500.0000"); // EUR
                
                sampleAccount.setResult(result);
                
                // Return the sample data
                return ResponseEntity.ok(sampleAccount);
            }

            log.info("Fetching raw account data from Kraken API");
            
            try {
                // Get the complete unmodified raw data from the Kraken API
                KrakenAccount rawAccount = krakenService.getAccount(apiKey, secretKey);
                
                // Log the response to help with debugging
                if (rawAccount != null) {
                    if (rawAccount.getError() != null && rawAccount.getError().length > 0) {
                        log.warn("Kraken API returned error: {}", String.join(", ", rawAccount.getError()));
                    } else {
                        log.info("Successfully retrieved raw account data from Kraken API");
                        if (rawAccount.getResult() != null) {
                            log.info("Kraken balance contains {} assets", rawAccount.getResult().size());
                        }
                    }
                }
                
                // Return the completely unmodified raw data directly
                // This ensures the frontend gets exactly what comes from the Kraken API
                return ResponseEntity.ok(rawAccount);
            } catch (Exception e) {
                log.error("Error from Kraken API: {}", e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                    "status", "error",
                    "message", "Error from Kraken API: " + e.getMessage(),
                    "error", e.getClass().getSimpleName()
                ));
            }
        } catch (Exception e) {
            log.error("Error processing request: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }
}
