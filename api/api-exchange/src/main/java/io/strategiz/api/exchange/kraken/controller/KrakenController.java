package io.strategiz.api.exchange.kraken.controller;

import io.strategiz.data.exchange.ExchangeCredentialsRepository;
import io.strategiz.service.exchange.kraken.KrakenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for Kraken exchange API operations
 * Handles regular user functionality for the Kraken integration
 */
@Slf4j
@RestController
@RequestMapping("/api/kraken")
@CrossOrigin(origins = {"http://localhost:3000", "https://strategiz.io"}, allowedHeaders = "*")
public class KrakenController {

    private final KrakenService krakenService;
    private final ExchangeCredentialsRepository credentialsRepository;

    @Autowired
    public KrakenController(KrakenService krakenService, ExchangeCredentialsRepository credentialsRepository) {
        this.krakenService = krakenService;
        this.credentialsRepository = credentialsRepository;
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
            
            // Save the credentials
            log.info("Saving Kraken credentials for user: {}", userId);
            boolean saved = credentialsRepository.saveExchangeCredentials(userId, "kraken", apiKey, secretKey);
            
            if (!saved) {
                log.error("Failed to save Kraken credentials for user: {}", userId);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Internal Server Error",
                    "message", "Failed to save Kraken API credentials"
                ));
            }
            
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
            
            // Get Kraken credentials from the repository
            Map<String, String> credentials = credentialsRepository.getExchangeCredentials(userId, "kraken");
            
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
            
            // Test connection with the credentials
            boolean connectionSuccessful = krakenService.testConnection(
                credentials.get("apiKey"), credentials.get("secretKey")
            );
            
            if (connectionSuccessful) {
                log.info("Kraken API health check successful for user: {}", userId);
                return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Kraken API is accessible and credentials are valid",
                    "timestamp", new Date().toString(),
                    "credentialsFound", true,
                    "connectionTest", "passed"
                ));
            } else {
                log.error("Kraken API connection test failed for user: {}", userId);
                return ResponseEntity.ok(Map.of(
                    "status", "error",
                    "message", "Kraken API credentials are invalid or API is unavailable",
                    "timestamp", new Date().toString(),
                    "credentialsFound", true,
                    "connectionTest", "failed"
                ));
            }
        } catch (Exception e) {
            log.error("Error performing Kraken API health check: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "error",
                "message", "Error performing Kraken API health check: " + e.getMessage(),
                "timestamp", new Date().toString()
            ));
        }
    }
}
