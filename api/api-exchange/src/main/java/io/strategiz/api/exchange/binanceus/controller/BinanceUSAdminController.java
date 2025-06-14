package io.strategiz.api.exchange.binanceus.controller;

import io.strategiz.service.exchange.binanceus.BinanceUSService;
import io.strategiz.service.exchange.binanceus.FirestoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for Binance US admin operations
 */
@RestController
@RequestMapping("/api/binanceus")
public class BinanceUSAdminController {

    private static final Logger log = LoggerFactory.getLogger(BinanceUSAdminController.class);

    @Autowired
    private BinanceUSService binanceUSService;
    
    @Autowired
    private FirestoreService firestoreService;

    /**
     * Get raw account data from Binance US API
     * 
     * @param httpRequest HTTP request
     * @return Raw account data
     */
    @GetMapping("/raw-account-data")
    public ResponseEntity<?> getRawAccountData(HttpServletRequest httpRequest) {
        log.info("Received request to get raw Binance US account data");
        
        try {
            String userId = httpRequest.getHeader("X-User-Email");
            if (userId == null || userId.isEmpty()) {
                log.warn("No user email provided in request header, falling back to authenticated user");
                userId = httpRequest.getParameter("email");
                
                if (userId == null || userId.isEmpty()) {
                    log.error("No user email provided");
                    return ResponseEntity.badRequest().body(Map.of(
                        "error", "Bad Request",
                        "message", "User email is required"
                    ));
                }
            }
            
            log.info("Getting raw Binance US account data for user: {}", userId);
            
            // Get API credentials from Firestore
            Map<String, String> credentials = firestoreService.getBinanceUSCredentials(userId);
            
            if (credentials == null || credentials.get("apiKey") == null || credentials.get("secretKey") == null) {
                log.error("Binance US API credentials not found for user: {}", userId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "error", "Bad Request",
                    "message", "Binance US API credentials not configured"
                ));
            }
            
            String apiKey = credentials.get("apiKey");
            String secretKey = credentials.get("secretKey");
            
            log.info("Retrieved Binance US API credentials for user: {}", userId);
            log.info("API key found (first 5 chars): {}", 
                apiKey.substring(0, Math.min(apiKey.length(), 5)) + "...");
            
            // Get raw account data from Binance US API
            Object rawData = binanceUSService.getRawAccountData(apiKey, secretKey);
            
            log.info("Successfully retrieved raw Binance US account data for user: {}", userId);
            return ResponseEntity.ok(rawData);
            
        } catch (Exception e) {
            log.error("Error getting raw Binance US account data: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Internal Server Error",
                "message", "Error getting raw Binance US account data: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Check Binance US API health
     * 
     * @param httpRequest HTTP request
     * @return Health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> checkHealth(HttpServletRequest httpRequest) {
        log.info("Received request to check Binance US API health");
        
        try {
            String userId = httpRequest.getHeader("X-User-Email");
            if (userId == null || userId.isEmpty()) {
                log.warn("No user email provided in request header, falling back to authenticated user");
                userId = httpRequest.getParameter("email");
                
                if (userId == null || userId.isEmpty()) {
                    log.error("No user email provided");
                    return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "User email is required",
                        "timestamp", System.currentTimeMillis()
                    ));
                }
            }
            
            log.info("Checking Binance US API health for user: {}", userId);
            
            // Get API credentials from Firestore
            Map<String, String> credentials = firestoreService.getBinanceUSCredentials(userId);
            
            Map<String, Object> healthStatus = new HashMap<>();
            healthStatus.put("timestamp", System.currentTimeMillis());
            
            if (credentials == null || credentials.get("apiKey") == null || credentials.get("secretKey") == null) {
                log.warn("Binance US API credentials not found for user: {}", userId);
                healthStatus.put("status", "unhealthy");
                healthStatus.put("message", "API credentials not configured");
                healthStatus.put("credentialsFound", false);
                return ResponseEntity.ok(healthStatus);
            }
            
            String apiKey = credentials.get("apiKey");
            String secretKey = credentials.get("secretKey");
            
            log.info("Retrieved Binance US API credentials for user: {}", userId);
            log.info("API key found (first 5 chars): {}", 
                apiKey.substring(0, Math.min(apiKey.length(), 5)) + "...");
            
            // Test connection to Binance US API
            Map<String, Object> connectionResult = binanceUSService.testConnection(apiKey, secretKey);
            boolean connectionSuccessful = "ok".equals(connectionResult.get("status"));
            
            if (connectionSuccessful) {
                log.info("Binance US API health check successful for user: {}", userId);
                healthStatus.put("status", "healthy");
                healthStatus.put("message", "API connection successful");
                healthStatus.put("credentialsFound", true);
                healthStatus.put("connectionSuccessful", true);
            } else {
                log.warn("Binance US API health check failed for user: {}", userId);
                healthStatus.put("status", "unhealthy");
                healthStatus.put("message", connectionResult.get("message"));
                healthStatus.put("credentialsFound", true);
                healthStatus.put("connectionSuccessful", false);
            }
            
            return ResponseEntity.ok(healthStatus);
            
        } catch (Exception e) {
            log.error("Error checking Binance US API health: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error checking Binance US API health: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()
            ));
        }
    }
    
    /**
     * Get Binance US API keys
     * 
     * @param httpRequest HTTP request
     * @return API keys
     */
    @GetMapping("/api-keys")
    public ResponseEntity<Map<String, Object>> getApiKeys(HttpServletRequest httpRequest) {
        log.info("Received request to get Binance US API keys");
        
        try {
            String userId = httpRequest.getHeader("X-User-Email");
            if (userId == null || userId.isEmpty()) {
                log.warn("No user email provided in request header, falling back to authenticated user");
                userId = httpRequest.getParameter("email");
                
                if (userId == null || userId.isEmpty()) {
                    log.error("No user email provided");
                    return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "User email is required"
                    ));
                }
            }
            
            log.info("Getting Binance US API keys for user: {}", userId);
            
            // Get API credentials from Firestore
            Map<String, String> credentials = firestoreService.getBinanceUSCredentials(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("timestamp", System.currentTimeMillis());
            
            if (credentials == null || credentials.get("apiKey") == null || credentials.get("secretKey") == null) {
                log.warn("Binance US API credentials not found for user: {}", userId);
                response.put("status", "not_configured");
                response.put("message", "API credentials not configured");
                response.put("apiKey", "");
                response.put("secretKey", "");
            } else {
                String apiKey = credentials.get("apiKey");
                String secretKey = credentials.get("secretKey");
                
                // Mask the secret key for security
                String maskedSecretKey = secretKey.substring(0, Math.min(secretKey.length(), 5)) + 
                    "...".repeat(3) + 
                    (secretKey.length() > 10 ? secretKey.substring(secretKey.length() - 5) : "");
                
                log.info("Retrieved Binance US API credentials for user: {}", userId);
                response.put("status", "configured");
                response.put("message", "API credentials found");
                response.put("apiKey", apiKey);
                response.put("secretKey", maskedSecretKey);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting Binance US API keys: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error getting Binance US API keys: " + e.getMessage()
            ));
        }
    }
}
