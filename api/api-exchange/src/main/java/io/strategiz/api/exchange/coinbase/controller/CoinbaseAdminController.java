package io.strategiz.api.exchange.coinbase.controller;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

// Never use mock data - always use real API data
// Important: This class must always use real API data, no mocks

import io.strategiz.service.exchange.coinbase.CoinbaseService;
import io.strategiz.service.exchange.coinbase.CoinbaseCloudService;
import io.strategiz.service.exchange.coinbase.FirestoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;

import com.google.firebase.cloud.FirestoreClient;
import com.google.cloud.firestore.QuerySnapshot;
import org.slf4j.MDC;

/**
 * Controller for Coinbase admin operations
 * This controller provides endpoints for managing Coinbase API integration,
 * including health checks, raw data retrieval, and API key management.
 * All endpoints use real API data, never mock responses.
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
    
    @Autowired
    private CoinbaseCloudService coinbaseCloudService;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    /**
     * Health check endpoint for Coinbase API integration
     * This verifies that the API controller is responding and attempts to check connectivity to Coinbase
     * using real credentials (never mock data)
     * 
     * @param httpRequest HTTP request
     * @return Health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck(HttpServletRequest httpRequest) {
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
                        "timestamp", System.currentTimeMillis()
                    ));
                }
            }
            
            log.info("Checking Coinbase API health for user: {}", userId);
            
            // Get API credentials from Firestore
            Map<String, String> credentials = firestoreService.getCoinbaseCredentials(userId);
            
            Map<String, Object> healthStatus = new HashMap<>();
            healthStatus.put("timestamp", System.currentTimeMillis());
            
            if (credentials == null || credentials.get("apiKey") == null || credentials.get("privateKey") == null) {
                log.warn("Coinbase API credentials not found for user: {}", userId);
                healthStatus.put("status", "unhealthy");
                healthStatus.put("message", "API credentials not configured");
                healthStatus.put("credentialsFound", false);
                return ResponseEntity.ok(healthStatus);
            }
            
            String apiKey = credentials.get("apiKey");
            String privateKey = credentials.get("privateKey");
            
            log.info("Retrieved Coinbase API credentials for user: {}", userId);
            log.info("API key found (first 5 chars): {}", 
                apiKey.substring(0, Math.min(apiKey.length(), 5)) + "...");
            
            // Test connection to Coinbase API
            Map<String, Object> connectionResult = coinbaseService.testConnection(apiKey, privateKey);
            boolean connectionSuccessful = "ok".equals(connectionResult.get("status"));
            
            if (connectionSuccessful) {
                log.info("Coinbase API health check successful for user: {}", userId);
                healthStatus.put("status", "healthy");
                healthStatus.put("message", "API connection successful");
                healthStatus.put("credentialsFound", true);
                healthStatus.put("connectionSuccessful", true);
            } else {
                log.warn("Coinbase API health check failed for user: {}", userId);
                healthStatus.put("status", "unhealthy");
                healthStatus.put("message", connectionResult.get("message"));
                healthStatus.put("credentialsFound", true);
                healthStatus.put("connectionSuccessful", false);
            }
            
            return ResponseEntity.ok(healthStatus);
            
        } catch (Exception e) {
            log.error("Error checking Coinbase API health: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error checking Coinbase API health: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()
            ));
        }
    }
    
    /**
     * Get raw account data from Coinbase API - dedicated endpoint for frontend
     * This endpoint returns the completely unmodified raw data from Coinbase API
     * 
     * @param httpRequest HTTP request
     * @return Raw account data from Coinbase API
     */
    @GetMapping("/raw-account-data")
    public ResponseEntity<?> getRawAccountDataForFrontend(HttpServletRequest httpRequest) {
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
            
            if (credentials == null || credentials.get("apiKey") == null || credentials.get("privateKey") == null) {
                log.error("Coinbase API credentials not found for user: {}", userId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "error", "Bad Request",
                    "message", "Coinbase API credentials not configured"
                ));
            }
            
            String apiKey = credentials.get("apiKey");
            String privateKey = credentials.get("privateKey");
            
            log.info("Retrieved Coinbase API credentials for user: {}", userId);
            log.info("API key found (first 5 chars): {}", 
                apiKey.substring(0, Math.min(apiKey.length(), 5)) + "...");
            
            // Get raw account data from Coinbase API
            Object rawData = coinbaseService.getRawAccountData(apiKey, privateKey);
            
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
     * Get Coinbase API keys
     * 
     * @param httpRequest HTTP request
     * @return API keys
     */
    @GetMapping("/api-keys")
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
            
            Map<String, Object> response = new HashMap<>();
            response.put("timestamp", System.currentTimeMillis());
            
            if (credentials == null || credentials.get("apiKey") == null || credentials.get("privateKey") == null) {
                log.warn("Coinbase API credentials not found for user: {}", userId);
                response.put("status", "not_configured");
                response.put("message", "API credentials not configured");
                response.put("apiKey", "");
                response.put("privateKey", "");
            } else {
                String apiKey = credentials.get("apiKey");
                String privateKey = credentials.get("privateKey");
                
                // Mask the private key for security
                String maskedPrivateKey = "PEM key available (not shown for security)";
                
                log.info("Retrieved Coinbase API credentials for user: {}", userId);
                response.put("status", "configured");
                response.put("message", "API credentials found");
                response.put("apiKey", apiKey);
                response.put("privateKey", maskedPrivateKey);
            }
            
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
     * Helper method to get all headers from a request for logging purposes
     * 
     * @param request The HTTP request
     * @return Map of header names and values
     */
    private Map<String, String> getHeadersInfo(HttpServletRequest request) {
        Map<String, String> map = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String key = headerNames.nextElement();
            String value = request.getHeader(key);
            map.put(key, value);
        }
        return map;
    }
}
