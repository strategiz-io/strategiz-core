package io.strategiz.kraken.controller;

import io.strategiz.kraken.model.KrakenAccount;
import io.strategiz.kraken.service.FirestoreService;
import io.strategiz.kraken.service.KrakenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for Kraken API
 * Handles regular user functionality for the Kraken integration
 */
@Slf4j
@RestController
@RequestMapping("/api/kraken")
public class KrakenController {

    private final KrakenService krakenService;
    private final FirestoreService firestoreService;

    @Autowired
    public KrakenController(KrakenService krakenService, FirestoreService firestoreService) {
        this.krakenService = krakenService;
        this.firestoreService = firestoreService;
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
     * Test connection to Kraken API
     * 
     * @param request Request containing API key and secret key
     * @return Test results
     */
    @PostMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testConnection(@RequestBody Map<String, String> request) {
        try {
            String apiKey = request.get("apiKey");
            String secretKey = request.get("secretKey");
            
            boolean connectionSuccessful = krakenService.testConnection(apiKey, secretKey);
            
            Map<String, Object> result = new HashMap<>();
            if (connectionSuccessful) {
                result.put("status", "success");
                result.put("message", "Connection to Kraken API successful");
            } else {
                result.put("status", "error");
                result.put("message", "Connection to Kraken API failed");
            }
            
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
     * Set Kraken API credentials for a user
     * 
     * @param request Request containing API key and secret key
     * @param httpRequest HttpServletRequest
     * @return Configuration status
     */
    @PostMapping("/set-credentials")
    @CrossOrigin(origins = {"http://localhost:3000", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Map<String, Object>> setKrakenCredentials(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        log.info("Received request to set Kraken API credentials");
        
        try {
            String userId = httpRequest.getHeader("X-User-Email");
            if (userId == null || userId.isEmpty()) {
                log.warn("No user email provided in request header, falling back to authenticated user");
                // In a real implementation, this would come from the security context
                // For now, we'll use the email from the request parameter as a fallback
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
            log.info("Testing connection to Kraken API with provided credentials");
            boolean connectionSuccessful = krakenService.testConnection(apiKey, secretKey);
            
            if (!connectionSuccessful) {
                log.error("Connection test failed for user: {}", userId);
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Bad Request",
                    "message", "Connection test failed. Please check your API credentials."
                ));
            }
            
            // Save the credentials to Firestore
            log.info("Saving Kraken credentials for user: {}", userId);
            firestoreService.saveKrakenCredentials(userId, apiKey, secretKey);
            
            log.info("Kraken credentials saved successfully for user: {}", userId);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Kraken API credentials saved successfully"
            ));
        } catch (Exception e) {
            log.error("Error setting Kraken API credentials: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Internal Server Error",
                "message", "Error setting Kraken API credentials: " + e.getMessage()
            ));
        }
    }


    
    /**
     * Simple health check endpoint to verify the Kraken controller is accessible
     * @return Health status
     */
    @GetMapping("/health")
    @CrossOrigin(origins = {"http://localhost:3000", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<?> healthCheck(HttpServletRequest request) {
        log.info("Received health check request");
        
        try {
            // Get the authenticated user's email from the request
            String userId = request.getHeader("X-User-Email");
            if (userId == null || userId.isEmpty()) {
                userId = request.getParameter("email");
                
                if (userId == null || userId.isEmpty()) {
                    log.error("No user email provided for health check");
                    return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "User email is required",
                        "timestamp", new Date().toString()
                    ));
                }
            }
            
            log.info("Performing health check for user: {}", userId);
            
            // Get Kraken credentials from Firestore
            Map<String, String> credentials = firestoreService.getKrakenCredentials(userId);
            
            if (credentials == null || credentials.isEmpty() || 
                credentials.get("apiKey") == null || credentials.get("secretKey") == null) {
                log.error("Kraken API credentials not found or incomplete for user: {}", userId);
                return ResponseEntity.ok(Map.of(
                    "status", "error",
                    "message", "Kraken API credentials not found or incomplete",
                    "timestamp", new Date().toString(),
                    "credentialsFound", false
                ));
            }
            
            String apiKey = credentials.get("apiKey");
            String secretKey = credentials.get("secretKey");
            
            // Test the connection with the Kraken API
            boolean connectionSuccessful = krakenService.testConnection(apiKey, secretKey);
            
            Map<String, Object> result = new HashMap<>();
            result.put("timestamp", new Date().toString());
            result.put("credentialsFound", true);
            result.put("apiKeyPresent", apiKey != null && !apiKey.isEmpty());
            result.put("secretKeyPresent", secretKey != null && !secretKey.isEmpty());
            
            if (connectionSuccessful) {
                result.put("status", "healthy");
                result.put("message", "Kraken API connection successful");
                result.put("connectionTest", "passed");
            } else {
                result.put("status", "unhealthy");
                result.put("message", "Kraken API connection failed");
                result.put("connectionTest", "failed");
            }
            
            log.info("Health check completed for user: {}", userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error during health check: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error during health check: " + e.getMessage(),
                "timestamp", new Date().toString()
            ));
        }
    }
}
