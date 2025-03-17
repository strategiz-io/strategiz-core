package io.strategiz.coinbase.controller;

import io.strategiz.coinbase.service.CoinbaseService;
import io.strategiz.coinbase.service.firestore.FirestoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for Coinbase admin operations
 */
@RestController
@RequestMapping("/api/coinbase")
@Slf4j
public class CoinbaseAdminController {

    @Autowired
    private CoinbaseService coinbaseService;
    
    @Autowired
    @Qualifier("coinbaseFirestoreService")
    private FirestoreService firestoreService;

    /**
     * Get raw account data from Coinbase API
     * This endpoint returns the completely unmodified raw data from Coinbase API
     * 
     * @param httpRequest HTTP request
     * @return Raw account data
     */
    @GetMapping("/raw-account-data")
    @CrossOrigin(origins = {"http://localhost:3000", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<?> getRawAccountData(HttpServletRequest httpRequest) {
        log.info("Received request to get raw Coinbase account data");
        
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
            
            log.info("Getting raw Coinbase account data for user: {}", userId);
            
            // Get API credentials from Firestore
            Map<String, String> credentials = firestoreService.getCoinbaseCredentials(userId);
            
            if (credentials == null || credentials.get("apiKey") == null || credentials.get("secretKey") == null) {
                log.error("Coinbase API credentials not found for user: {}", userId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "error", "Bad Request",
                    "message", "Coinbase API credentials not configured"
                ));
            }
            
            String apiKey = credentials.get("apiKey");
            String secretKey = credentials.get("secretKey");
            
            log.info("Retrieved Coinbase API credentials for user: {}", userId);
            log.info("API key found (first 5 chars): {}", 
                apiKey.substring(0, Math.min(apiKey.length(), 5)) + "...");
            
            // Get raw account data from Coinbase API
            Object rawData = coinbaseService.getRawAccountData(apiKey, secretKey);
            
            log.info("Successfully retrieved raw Coinbase account data for user: {}", userId);
            return ResponseEntity.ok(rawData);
            
        } catch (Exception e) {
            log.error("Error getting raw Coinbase account data: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Internal Server Error",
                "message", "Error getting raw Coinbase account data: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Check Coinbase API health
     * 
     * @param httpRequest HTTP request
     * @return Health status
     */
    @GetMapping("/health")
    @CrossOrigin(origins = {"http://localhost:3000", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Map<String, Object>> checkHealth(HttpServletRequest httpRequest) {
        log.info("Received request to check Coinbase API health");
        
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
                        "timestamp", new Date().toString()
                    ));
                }
            }
            
            log.info("Checking Coinbase API health for user: {}", userId);
            
            // Get API credentials from Firestore
            Map<String, String> credentials = firestoreService.getCoinbaseCredentials(userId);
            
            Map<String, Object> healthStatus = new HashMap<>();
            healthStatus.put("timestamp", new Date().toString());
            
            if (credentials == null || credentials.get("apiKey") == null || credentials.get("secretKey") == null) {
                log.warn("Coinbase API credentials not found for user: {}", userId);
                healthStatus.put("status", "unhealthy");
                healthStatus.put("message", "API credentials not configured");
                healthStatus.put("credentialsFound", false);
                return ResponseEntity.ok(healthStatus);
            }
            
            String apiKey = credentials.get("apiKey");
            String secretKey = credentials.get("secretKey");
            
            log.info("Retrieved Coinbase API credentials for user: {}", userId);
            log.info("API key found (first 5 chars): {}", 
                apiKey.substring(0, Math.min(apiKey.length(), 5)) + "...");
            
            // Test connection to Coinbase API
            boolean connectionSuccessful = coinbaseService.testConnection(apiKey, secretKey);
            
            if (connectionSuccessful) {
                log.info("Coinbase API health check successful for user: {}", userId);
                healthStatus.put("status", "healthy");
                healthStatus.put("message", "API connection successful");
                healthStatus.put("credentialsFound", true);
                healthStatus.put("connectionSuccessful", true);
            } else {
                log.warn("Coinbase API health check failed for user: {}", userId);
                healthStatus.put("status", "unhealthy");
                healthStatus.put("message", "API connection failed");
                healthStatus.put("credentialsFound", true);
                healthStatus.put("connectionSuccessful", false);
            }
            
            return ResponseEntity.ok(healthStatus);
            
        } catch (Exception e) {
            log.error("Error checking Coinbase API health: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Error checking Coinbase API health: " + e.getMessage());
            errorResponse.put("timestamp", new Date().toString());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Get Coinbase API keys
     * 
     * @param httpRequest HTTP request
     * @return API keys
     */
    @GetMapping("/api-keys")
    @CrossOrigin(origins = {"http://localhost:3000", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Map<String, Object>> getApiKeys(HttpServletRequest httpRequest) {
        log.info("Received request to get Coinbase API keys");
        
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
            
            log.info("Getting Coinbase API keys for user: {}", userId);
            
            // Get API credentials from Firestore
            Map<String, String> credentials = firestoreService.getCoinbaseCredentials(userId);
            
            if (credentials == null) {
                log.warn("Coinbase API credentials not found for user: {}", userId);
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
            
            log.info("Retrieved Coinbase API keys for user: {}", userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("apiKey", apiKey);
            response.put("secretKey", maskedSecretKey);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting Coinbase API keys: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error getting Coinbase API keys: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Set Coinbase API credentials
     * 
     * @param request Request containing API key and secret key
     * @param httpRequest HTTP request
     * @return Configuration status
     */
    @PostMapping("/set-credentials")
    @CrossOrigin(origins = {"http://localhost:3000", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Map<String, Object>> setCoinbaseCredentials(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        log.info("Received request to set Coinbase API credentials");
        
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
            log.info("Testing connection to Coinbase API with provided credentials");
            boolean connectionSuccessful = coinbaseService.testConnection(apiKey, secretKey);
            
            if (!connectionSuccessful) {
                log.error("Connection test failed for user: {}", userId);
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Bad Request",
                    "message", "Connection test failed. Please check your API credentials."
                ));
            }
            
            // Save the credentials to Firestore
            log.info("Saving Coinbase credentials for user: {}", userId);
            firestoreService.saveCoinbaseCredentials(userId, apiKey, secretKey);
            
            log.info("Coinbase credentials saved successfully for user: {}", userId);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Coinbase API credentials saved successfully"
            ));
        } catch (Exception e) {
            log.error("Error setting Coinbase API credentials: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Internal Server Error",
                "message", "Error setting Coinbase API credentials: " + e.getMessage()
            ));
        }
    }
}
