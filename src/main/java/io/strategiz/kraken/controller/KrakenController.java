package io.strategiz.kraken.controller;

import io.strategiz.kraken.model.KrakenAccount;
import io.strategiz.kraken.service.FirestoreService;
import io.strategiz.kraken.service.KrakenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
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

    /**
     * Get raw account data from Kraken API using stored credentials
     * This endpoint returns the completely unmodified raw data from Kraken API
     * 
     * @return Raw account data from Kraken API
     */
    @GetMapping("/raw-account-data")
    @CrossOrigin(origins = {"http://localhost:3000", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<?> getRawAccountDataForUser(HttpServletRequest request) {
        log.info("Received request for raw Kraken account data");
        
        try {
            // Get the authenticated user's email from the request
            // This should be extracted from the authentication context in a production environment
            String userId = request.getHeader("X-User-Email");
            if (userId == null || userId.isEmpty()) {
                log.warn("No user email provided in request header, falling back to authenticated user");
                // In a real implementation, this would come from the security context
                // For now, we'll use the email from the request parameter as a fallback
                userId = request.getParameter("email");
                
                if (userId == null || userId.isEmpty()) {
                    log.error("No user email provided");
                    return ResponseEntity.badRequest().body(Map.of(
                        "error", "Bad Request",
                        "message", "User email is required"
                    ));
                }
            }
            
            log.info("Getting raw account data for user: {}", userId);
            
            // Get Kraken credentials from Firestore
            log.info("Getting Kraken credentials for user: {}", userId);
            Map<String, String> credentials = firestoreService.getKrakenCredentials(userId);
            
            if (credentials == null || credentials.isEmpty() || 
                credentials.get("apiKey") == null || credentials.get("secretKey") == null) {
                log.error("Kraken API credentials not found or incomplete for user: {}", userId);
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Bad Request",
                    "message", "Kraken API credentials not found or incomplete"
                ));
            }
            
            String apiKey = credentials.get("apiKey");
            String secretKey = credentials.get("secretKey");
            
            // Get account data from Kraken API
            log.info("Getting account data from Kraken API for user: {}", userId);
            KrakenAccount account = krakenService.getAccount(apiKey, secretKey);
            
            if (account == null) {
                log.error("Failed to get account data from Kraken API for user: {}", userId);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Internal Server Error",
                    "message", "Failed to get account data from Kraken API"
                ));
            }
            
            if (account.getError() != null && account.getError().length > 0) {
                log.error("Kraken API returned error: {}", String.join(", ", account.getError()));
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Bad Request",
                    "message", "Kraken API returned error: " + String.join(", ", account.getError())
                ));
            }
            
            log.info("Successfully retrieved account data from Kraken API for user: {}", userId);
            return ResponseEntity.ok(account);
        } catch (Exception e) {
            log.error("Unexpected error in getRawAccountDataForUser: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Internal Server Error",
                "message", "Unexpected error: " + e.getMessage(),
                "exceptionType", e.getClass().getSimpleName(),
                "stackTrace", e.getStackTrace()[0].toString()
            ));
        }
    }

    /**
     * Test the Kraken API connection directly
     * This endpoint attempts to connect to the Kraken API and returns detailed diagnostic information
     * This endpoint does not require authentication
     * 
     * @return Diagnostic information about the Kraken API connection
     */
    @GetMapping("/test-api-connection")
    @CrossOrigin(origins = {"http://localhost:3000", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Map<String, Object>> testApiConnection() {
        log.info("Received request to test Kraken API connection directly");
        
        Map<String, Object> result = new HashMap<>();
        result.put("timestamp", System.currentTimeMillis());
        result.put("controller", "KrakenController");
        
        try {
            // Create a simple RestTemplate with timeouts
            RestTemplate testRestTemplate = new RestTemplate();
            HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
            factory.setConnectTimeout(10000); // 10 seconds
            factory.setReadTimeout(30000);    // 30 seconds
            testRestTemplate.setRequestFactory(factory);
            
            // Test connection to Kraken API public endpoint (no authentication required)
            log.info("Testing connection to Kraken API public endpoint");
            String krakenPublicUrl = "https://api.kraken.com/0/public/Time";
            
            long startTime = System.currentTimeMillis();
            ResponseEntity<?> response = testRestTemplate.getForEntity(krakenPublicUrl, Object.class);
            long endTime = System.currentTimeMillis();
            
            result.put("status", "success");
            result.put("responseTime", endTime - startTime);
            result.put("statusCode", response.getStatusCode().toString());
            result.put("responseBody", response.getBody());
            
            log.info("Successfully connected to Kraken API. Response time: {}ms", endTime - startTime);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error testing connection to Kraken API: {}", e.getMessage(), e);
            
            result.put("status", "error");
            result.put("error", e.getClass().getSimpleName());
            result.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
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
     * Get API keys for a user
     * This endpoint returns the API keys stored in Firestore
     * Note: This endpoint masks the secret key for security reasons
     * 
     * @return API keys
     */
    @GetMapping("/api-keys")
    @CrossOrigin(origins = {"http://localhost:3000", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<?> getApiKeys(HttpServletRequest request) {
        log.info("Received request for Kraken API keys");
        
        try {
            // Get the authenticated user's email from the request
            // This should be extracted from the authentication context in a production environment
            String userId = request.getHeader("X-User-Email");
            if (userId == null || userId.isEmpty()) {
                log.warn("No user email provided in request header, falling back to authenticated user");
                // In a real implementation, this would come from the security context
                // For now, we'll use the email from the request parameter as a fallback
                userId = request.getParameter("email");
                
                if (userId == null || userId.isEmpty()) {
                    log.error("No user email provided");
                    return ResponseEntity.badRequest().body(Map.of(
                        "error", "Bad Request",
                        "message", "User email is required"
                    ));
                }
            }
            
            log.info("Getting Kraken API keys for user: {}", userId);
            
            // Get Kraken credentials from Firestore
            Map<String, String> credentials = firestoreService.getKrakenCredentials(userId);
            
            if (credentials == null || credentials.isEmpty() || 
                credentials.get("apiKey") == null || credentials.get("secretKey") == null) {
                log.error("Kraken API credentials not found or incomplete for user: {}", userId);
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Bad Request",
                    "message", "Kraken API credentials not found or incomplete"
                ));
            }
            
            // Mask the secret key for security
            String apiKey = credentials.get("apiKey");
            String secretKey = credentials.get("secretKey");
            String maskedSecretKey = maskSecretKey(secretKey);
            
            Map<String, Object> result = new HashMap<>();
            result.put("apiKey", apiKey);
            result.put("secretKey", maskedSecretKey);
            result.put("source", "Firestore");
            result.put("userId", userId);
            
            log.info("Returning Kraken API keys for user: {}", userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error getting Kraken API keys: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Internal Server Error",
                "message", "Error getting Kraken API keys: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Mask a secret key for security
     * @param secretKey Secret key to mask
     * @return Masked secret key
     */
    private String maskSecretKey(String secretKey) {
        if (secretKey == null || secretKey.length() <= 8) {
            return "********";
        }
        
        // Show first 4 and last 4 characters, mask the rest
        String firstFour = secretKey.substring(0, 4);
        String lastFour = secretKey.substring(secretKey.length() - 4);
        String masked = "*".repeat(secretKey.length() - 8);
        
        return firstFour + masked + lastFour;
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
