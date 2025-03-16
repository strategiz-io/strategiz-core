package io.strategiz.binanceus.controller;

import io.strategiz.binanceus.service.BinanceUSService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for Binance US admin operations
 */
@RestController
@RequestMapping("/api/binanceus")
@Slf4j
public class BinanceUSAdminController {

    @Autowired
    private BinanceUSService binanceUSService;
    
    @Autowired
    private io.strategiz.binanceus.service.FirestoreService firestoreService;

    /**
     * Get raw account data from Binance US API
     * 
     * @param httpRequest HTTP request
     * @return Raw account data
     */
    @GetMapping("/raw-account-data")
    @CrossOrigin(origins = {"http://localhost:3000", "https://strategiz.io"}, allowedHeaders = "*")
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
    @CrossOrigin(origins = {"http://localhost:3000", "https://strategiz.io"}, allowedHeaders = "*")
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
                healthStatus.put("message", "API connection failed");
                healthStatus.put("credentialsFound", true);
                healthStatus.put("connectionSuccessful", false);
            }
            
            return ResponseEntity.ok(healthStatus);
            
        } catch (Exception e) {
            log.error("Error checking Binance US API health: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Error checking Binance US API health: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Get Binance US API keys
     * 
     * @param httpRequest HTTP request
     * @return API keys
     */
    @GetMapping("/api-keys")
    @CrossOrigin(origins = {"http://localhost:3000", "https://strategiz.io"}, allowedHeaders = "*")
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
            
            if (credentials == null) {
                log.warn("Binance US API credentials not found for user: {}", userId);
                return ResponseEntity.ok(Map.of(
                    "status", "not_found",
                    "message", "API credentials not configured"
                ));
            }
            
            String apiKey = credentials.get("apiKey");
            String secretKey = credentials.get("secretKey");
            
            // Mask the secret key for security
            String maskedSecretKey = null;
            if (secretKey != null && !secretKey.isEmpty()) {
                if (secretKey.length() <= 8) {
                    maskedSecretKey = "********";
                } else {
                    maskedSecretKey = secretKey.substring(0, 4) + "..." + 
                        secretKey.substring(secretKey.length() - 4);
                }
            }
            
            log.info("Retrieved Binance US API keys for user: {}", userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("apiKey", apiKey);
            response.put("secretKey", maskedSecretKey);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting Binance US API keys: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error getting Binance US API keys: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Set Binance US API credentials
     * 
     * @param request Request containing API key and secret key
     * @param httpRequest HTTP request
     * @return Configuration status
     */
    @PostMapping("/set-credentials")
    @CrossOrigin(origins = {"http://localhost:3000", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Map<String, Object>> setBinanceUSCredentials(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        log.info("Received request to set Binance US API credentials");
        
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
            
            String apiKey = request.get("apiKey");
            String secretKey = request.get("secretKey");
            
            if (apiKey == null || apiKey.isEmpty() || secretKey == null || secretKey.isEmpty()) {
                log.error("API key or secret key is null or empty");
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Bad Request",
                    "message", "API key and secret key are required"
                ));
            }
            
            // Test the connection before saving the credentials
            log.info("Testing connection to Binance US API with provided credentials");
            Map<String, Object> connectionResult = binanceUSService.testConnection(apiKey, secretKey);
            boolean connectionSuccessful = "ok".equals(connectionResult.get("status"));
            
            if (!connectionSuccessful) {
                log.error("Connection test failed for user: {}", userId);
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Bad Request",
                    "message", "Connection test failed. Please check your API credentials."
                ));
            }
            
            // Save the credentials to Firestore
            log.info("Saving Binance US credentials for user: {}", userId);
            firestoreService.saveBinanceUSCredentials(userId, apiKey, secretKey);
            
            log.info("Binance US credentials saved successfully for user: {}", userId);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Binance US API credentials saved successfully"
            ));
        } catch (Exception e) {
            log.error("Error setting Binance US API credentials: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Internal Server Error",
                "message", "Error setting Binance US API credentials: " + e.getMessage()
            ));
        }
    }
}
