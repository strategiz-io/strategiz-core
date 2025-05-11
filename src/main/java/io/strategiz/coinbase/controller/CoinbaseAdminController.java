package io.strategiz.coinbase.controller;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.HashMap;

// Never use mock data - always use real API data
// Important: This class must always use real API data, no mocks

import io.strategiz.coinbase.client.exception.CoinbaseApiException;
import io.strategiz.coinbase.service.CoinbaseService;
import io.strategiz.coinbase.service.firestore.FirestoreService;
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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.google.firebase.cloud.FirestoreClient;
import com.google.cloud.firestore.QuerySnapshot;
import org.slf4j.MDC;

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
    
    @Autowired
    private io.strategiz.coinbase.service.CoinbaseCloudService coinbaseCloudService;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    /**
     * Test the JWT token generation and authentication with Coinbase API
     * This endpoint uses the updated JJWT implementation to generate a JWT token
     * and attempts to make a simple API call to verify the token works correctly
     */
    @GetMapping("/test-jwt-token")
    public ResponseEntity<Map<String, Object>> testJwtToken() {
        Map<String, Object> result = new HashMap<>();
        try {
            // Get user's credentials from Firestore
            String userId = "test"; // Using test user for diagnostic
            Map<String, String> credentials = firestoreService.getCoinbaseCredentials(userId);
            
            if (credentials == null || !credentials.containsKey("privateKey") || !credentials.containsKey("apiKey")) {
                result.put("success", false);
                result.put("error", "No Coinbase credentials found for user");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
            }
            
            String apiKey = credentials.get("apiKey");
            String privateKeyPem = credentials.get("privateKey");
            
            // Record basic credential information
            result.put("apiKeyFound", true);
            result.put("privateKeyFound", true);
            
            // 1. Test JWT token generation
            long startTime = System.currentTimeMillis();
            String jwtToken = coinbaseCloudService.generateJwtTokenForDiagnostic(apiKey, privateKeyPem);
            long endTime = System.currentTimeMillis();
            
            result.put("jwtGenerationSuccess", jwtToken != null && !jwtToken.isEmpty());
            result.put("jwtGenerationTime", (endTime - startTime) + "ms");
            result.put("jwtTokenLength", jwtToken != null ? jwtToken.length() : 0);
            result.put("jwtTokenParts", jwtToken != null ? jwtToken.split("\\.",-1).length : 0); // -1 to include empty segments
            
            // Don't expose the full token in the response for security
            if (jwtToken != null && jwtToken.length() > 20) {
                result.put("jwtTokenPreview", jwtToken.substring(0, 10) + "..." + 
                         jwtToken.substring(jwtToken.length() - 10));
            }
            
            // 2. Test API authentication with the generated token
            try {
                // Make a simple API call to test the token
                Map<String, Object> apiResponse = coinbaseCloudService.testApiAuthentication(apiKey, jwtToken);
                result.put("apiCallSuccess", true);
                result.put("apiResponse", apiResponse);
            } catch (Exception e) {
                result.put("apiCallSuccess", false);
                result.put("apiCallError", e.getMessage());
                // Add the stack trace for better debugging
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                result.put("stackTrace", sw.toString());
            }
            
            result.put("success", true);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "Error testing JWT token: " + e.getMessage());
            // Add the stack trace for better debugging
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            result.put("stackTrace", sw.toString());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
    
    /**
     * Diagnostic endpoint to check key format issues with real credentials
     * 
     * @param email User email for retrieving credentials
     * @return Diagnostic information about the key format
     */
    @GetMapping("/diagnose-key")
    public ResponseEntity<Map<String, Object>> diagnoseKeyFormat(@RequestParam String email) {
        log.info("Running key format diagnostics for user: {}", email);
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", new Date().toString());
        response.put("email", email);
        
        try {
            // Get user's real API credentials from Firestore
            Map<String, String> credentials = firestoreService.getCoinbaseCredentials(email);
            
            if (credentials == null || credentials.isEmpty()) {
                response.put("status", "error");
                response.put("message", "No Coinbase credentials found for user");
                return ResponseEntity.ok(response);
            }
            
            String apiKey = credentials.get("apiKey");
            String privateKey = credentials.get("privateKey");
            
            // Add basic diagnostics without exposing the full key
            response.put("apiKeyPresent", apiKey != null && !apiKey.isEmpty());
            response.put("privateKeyPresent", privateKey != null && !privateKey.isEmpty());
            
            if (privateKey != null && !privateKey.isEmpty()) {
                response.put("privateKeyLength", privateKey.length());
                response.put("containsBeginECTag", privateKey.contains("BEGIN EC PRIVATE KEY"));
                response.put("containsBeginPrivateKeyTag", privateKey.contains("BEGIN PRIVATE KEY"));
                response.put("containsDashes", privateKey.contains("-"));
                response.put("containsEquals", privateKey.contains("="));
                response.put("containsNewlines", privateKey.contains("\n") || privateKey.contains("\\n"));
                
                // Add first and last few characters (safely redacted)
                if (privateKey.length() > 10) {
                    response.put("keyPrefix", privateKey.substring(0, Math.min(10, privateKey.length())));
                    response.put("keySuffix", privateKey.substring(Math.max(0, privateKey.length() - 10)));
                }
                
                // Try to configure with the key to see what happens
                try {
                    Map<String, String> configResult = coinbaseCloudService.configure(apiKey, privateKey);
                    response.put("configureResult", configResult);
                } catch (Exception ex) {
                    response.put("configureError", ex.getMessage());
                    response.put("configureErrorType", ex.getClass().getName());
                }
                
                // Sample output of a correctly formatted key for comparison
                response.put("correctFormatExample", "-----BEGIN EC PRIVATE KEY-----\n[base64-encoded-data]\n-----END EC PRIVATE KEY-----");
            }
            
            response.put("status", "success");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error during key format diagnostics: {}", e.getMessage(), e);
            response.put("status", "error");
            response.put("message", "Error during diagnostics: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
    
    /**
     * Health check endpoint for Coinbase API integration
     * This verifies that the API controller is responding and attempts to check connectivity to Coinbase
     * using real credentials (never mock data)
     * 
     * @param httpRequest HTTP request
     * @return Health status
     */
    @GetMapping("/health")
    @CrossOrigin(origins = {"http://localhost:3000", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Map<String, Object>> healthCheck(HttpServletRequest httpRequest) {
        log.info("Received health check request");
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("controller", "CoinbaseAdminController");
        response.put("timestamp", new Date().toString());
        
        try {
            String userId = httpRequest.getHeader("X-User-Email");
            if (userId == null || userId.isEmpty()) {
                userId = httpRequest.getParameter("email");
            }
            
            if (userId != null && !userId.isEmpty()) {
                // If we have a user ID, try to get their API credentials
                Map<String, String> credentials = firestoreService.getCoinbaseCredentials(userId);
                
                if (credentials != null && !credentials.isEmpty()) {
                    // Check if API credentials are valid by checking if they exist
                    String apiKey = credentials.get("apiKey");
                    String privateKey = credentials.get("privateKey");
                    String passphrase = credentials.get("passphrase");
                    
                    if (apiKey != null && !apiKey.isEmpty() && privateKey != null && !privateKey.isEmpty() && passphrase != null && !passphrase.isEmpty()) {
                        try {
                            boolean connected = coinbaseService.testConnection(apiKey, privateKey, passphrase);
                            response.put("configuredForUser", true);
                            response.put("userEmail", userId);
                            response.put("hasApiKey", true);
                            response.put("hasPrivateKey", true);
                            response.put("hasPassphrase", true);
                            response.put("coinbaseApiConnected", connected);
                        } catch (CoinbaseApiException e) {
                            log.error("Error testing Coinbase API connection: {}", e.getMessage(), e);
                            response.put("configuredForUser", true);
                            response.put("userEmail", userId);
                            response.put("hasApiKey", true);
                            response.put("hasPrivateKey", true);
                            response.put("hasPassphrase", true);
                            response.put("coinbaseApiConnected", false);
                            response.put("coinbaseApiError", e.getMessage());
                        }
                    } else {
                        response.put("configuredForUser", false);
                        response.put("userEmail", userId);
                    }
                } else {
                    response.put("configuredForUser", false);
                    response.put("userEmail", userId);
                }
            }
            
            // Check if Coinbase API is available by making a public endpoint call
            try {
                // Use a public endpoint that doesn't require authentication
                ResponseEntity<Object> publicApiResponse = restTemplate.getForEntity(
                    "https://api.coinbase.com/v2/currencies", Object.class);
                
                if (publicApiResponse.getStatusCode().is2xxSuccessful()) {
                    response.put("coinbaseApiAvailable", true);
                } else {
                    response.put("coinbaseApiAvailable", false);
                    response.put("coinbaseApiError", "Non-200 response: " + publicApiResponse.getStatusCode());
                }
            } catch (RestClientResponseException e) {
                response.put("coinbaseApiAvailable", false);
                response.put("coinbaseApiError", e.getMessage());
            } catch (Exception e) {
                response.put("coinbaseApiAvailable", false);
                response.put("coinbaseApiError", e.getMessage());
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error in health check: {}", e.getMessage(), e);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get raw account data from Coinbase API - dedicated endpoint for frontend
     * This endpoint returns the completely unmodified raw data from Coinbase API
     * 
     * @param httpRequest HTTP request
     * @return Raw account data from Coinbase API
     */
    @GetMapping({"/raw-account-data", "/raw-data"})
    @CrossOrigin(origins = {"http://localhost:3000", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Object> getRawAccountDataForFrontend(HttpServletRequest httpRequest) {
        String requestId = String.valueOf(System.currentTimeMillis());
        MDC.put("requestId", requestId);
        log.info("[{}] Received request to get raw Coinbase account data (frontend endpoint)", requestId);
        
        try {
            String userId = httpRequest.getHeader("X-User-Email");
            if (userId == null || userId.isEmpty()) {
                log.warn("[{}] No user email provided in request header, falling back to parameter", requestId);
                userId = httpRequest.getParameter("email");
                
                if (userId == null || userId.isEmpty()) {
                    log.error("[{}] No user email provided", requestId);
                    return ResponseEntity.badRequest().body(Map.of(
                        "error", "Not Found",
                        "message", "User email is required"
                    ));
                }
            }
            
            log.info("[{}] Getting raw Coinbase account data for user: {}", requestId, userId);
            
            // Get API credentials from Firestore
            Map<String, String> credentials = firestoreService.getCoinbaseCredentials(userId);
            
            if (credentials == null || credentials.isEmpty()) {
                log.warn("[{}] Coinbase API credentials not found for user: {}", requestId, userId);
                return ResponseEntity.ok(buildErrorResponse(
                    404, // Error code
                    "Credentials Not Found", // User-friendly message
                    "No Coinbase API credentials found for user: " + userId, // Developer message
                    Map.of("userEmail", userId), // Additional information
                    null, // No exception
                    userId
                ));
            }
            
            String apiKey = credentials.get("apiKey");
            String privateKey = credentials.get("privateKey");
            String passphrase = credentials.get("passphrase");
            
            // For Coinbase Cloud API, passphrase is not required
            // For Advanced Trade API (formerly Pro), passphrase is required
            boolean isCloudApi = true; // Default to Coinbase Cloud API
            
            if (passphrase == null || passphrase.isEmpty()) {
                log.info("No passphrase found for user: {}. Using Coinbase Cloud API mode.", userId);
                
                // Check that we have valid privateKey and apiKey for Cloud API
                if (apiKey == null || apiKey.isEmpty() || privateKey == null || privateKey.isEmpty()) {
                    log.error("Missing required Coinbase Cloud API credentials for user: {}", userId);
                    return ResponseEntity.ok(buildErrorResponse(
                        400, // Error code
                        "Missing Required Credentials", // User-friendly message
                        "Coinbase Cloud API requires valid API Key and Private Key", // Developer message
                        Map.of("credentialsFound", true), // More information
                        null, // No exception
                        userId
                    ));
                }
            } else {
                // If passphrase is present, we'll use Advanced Trade API
                // No need to set isCloudApi as we're using the presence of passphrase directly
            }
            
            // Get raw account data from the appropriate Coinbase API based on available credentials
            try {
                Object rawData;
                
                // If passphrase is empty, use Coinbase Cloud API via CoinbaseCloudService
                if (passphrase == null || passphrase.isEmpty()) {
                    log.info("[{}] Using Coinbase Cloud API to get raw account data for user: {}", requestId, userId);
                    rawData = coinbaseCloudService.getRawAccountData(apiKey, privateKey);
                    
                    // Check if the response from CoinbaseCloudService is an error response
                    if (rawData instanceof Map) {
                        Map<?, ?> responseMap = (Map<?, ?>) rawData;
                        if (responseMap.containsKey("error")) {
                            log.error("[{}] Coinbase Cloud API returned error: {}", requestId, responseMap);
                            
                                            // Cast the map to the correct type
                            Map<String, Object> typedResponseMap = new HashMap<>();
                            for (Map.Entry<?, ?> entry : responseMap.entrySet()) {
                                if (entry.getKey() != null) {
                                    typedResponseMap.put(entry.getKey().toString(), entry.getValue());
                                }
                            }
                            
                            // Use standardized error format
                            return ResponseEntity.ok(buildErrorResponse(
                                500, // Error code
                                "Coinbase API Error", // User-friendly message
                                "Error from Coinbase Cloud API: " + responseMap.get("message"), // Developer message
                                typedResponseMap, // More information
                                null, // No exception
                                userId
                            ));
                        }
                    }
                } else {
                    // Otherwise use Advanced Trade API via CoinbaseService
                    log.info("[{}] Using Coinbase Advanced Trade API to get raw account data for user: {}", requestId, userId);
                    rawData = coinbaseService.getRawAccountData(apiKey, privateKey, passphrase);
                }
                
                if (rawData == null) {
                    log.error("[{}] Failed to get raw account data from Coinbase API for user: {}", requestId, userId);
                    return ResponseEntity.ok(buildErrorResponse(
                        500, // Error code
                        "API Error", // User-friendly message
                        "Failed to get raw account data from Coinbase API", // Developer message
                        Map.of("credentialsFound", true), // More information
                        null, // No exception
                        userId
                    ));
                }
                
                log.info("[{}] Successfully got raw Coinbase account data for user: {}", requestId, userId);
                MDC.remove("requestId");
                return ResponseEntity.ok(rawData);
            } catch (CoinbaseApiException coinbaseError) {
                // Handle our specific Coinbase API exception which contains detailed error information
                log.error("[{}] Coinbase API error for user {}: {}", requestId, userId, coinbaseError.getMessage());
                String errorDetails = coinbaseError.getErrorDetails();
                
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Coinbase API Error");
                errorResponse.put("message", "Error calling Coinbase API: " + coinbaseError.getMessage());
                errorResponse.put("coinbaseErrorDetails", errorDetails);
                errorResponse.put("timestamp", new Date().toString());
                errorResponse.put("userEmail", userId);
                errorResponse.put("credentialsFound", true);
                errorResponse.put("errorType", coinbaseError.getClass().getSimpleName());
                
                // Extract raw error details and include them directly in the response
                String rawError = coinbaseError.getErrorDetails();
                if (rawError != null && !rawError.isEmpty()) {
                    // Parse the response body if possible to include in the response
                    errorResponse.put("rawApiError", rawError);
                }
                
                // Extract HTTP status code and response body if available
                Throwable cause = coinbaseError.getCause();
                if (cause instanceof RestClientResponseException) {
                    RestClientResponseException restError = (RestClientResponseException) cause;
                    errorResponse.put("statusCode", restError.getRawStatusCode());
                    errorResponse.put("responseBody", restError.getResponseBodyAsString());
                    errorResponse.put("apiErrorMessage", restError.getMessage());
                } else if (cause instanceof HttpStatusCodeException) {
                    HttpStatusCodeException httpError = (HttpStatusCodeException) cause;
                    errorResponse.put("statusCode", httpError.getRawStatusCode());
                    errorResponse.put("responseBody", httpError.getResponseBodyAsString());
                    errorResponse.put("apiErrorMessage", httpError.getMessage());
                }
                
                // Log the complete error response to help with debugging
                log.info("[{}] Returning detailed Coinbase error response: {}", requestId, errorResponse);
                
                MDC.remove("requestId");
                return ResponseEntity.ok(errorResponse);
            } catch (Exception apiError) {
                // Fallback for any other exceptions
                log.error("[{}] General error for user {}: {}", requestId, userId, apiError.getMessage(), apiError);
                
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Coinbase API Error");
                errorResponse.put("message", "Error calling Coinbase API: " + apiError.getMessage());
                errorResponse.put("timestamp", new Date().toString());
                errorResponse.put("userEmail", userId);
                errorResponse.put("credentialsFound", true);
                errorResponse.put("errorType", apiError.getClass().getSimpleName());
                
                // Log the complete error response to help with debugging
                log.info("[{}] Returning general error response: {}", requestId, errorResponse);
                
                MDC.remove("requestId");
                return ResponseEntity.ok(errorResponse);
            }
            
        } catch (Exception e) {
            log.error("[{}] Error getting raw Coinbase account data: {}", requestId, e.getMessage(), e);
            MDC.remove("requestId");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Internal Server Error",
                "message", "Error getting raw Coinbase account data: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Diagnostic endpoint to check Coinbase credential completeness
     * This endpoint helps diagnose issues with Coinbase credentials stored in Firestore
     * 
     * @param email User email for checking credentials
     * @return Diagnostic information about the credentials
     */
    @GetMapping("/check-credentials")
    public ResponseEntity<Map<String, Object>> checkCredentialCompleteness(@RequestParam String email) {
        log.info("Checking Coinbase credential completeness for user: {}", email);
        
        Map<String, Object> result = new HashMap<>();
        result.put("email", email);
        result.put("timestamp", new Date().toString());
        
        try {
            // Try to get credentials from FirestoreService
            Map<String, String> credentials = firestoreService.getCoinbaseCredentials(email);
            
            if (credentials == null) {
                result.put("status", "error");
                result.put("credentialsFound", false);
                result.put("message", "No credentials found in Firestore for this user");
                return ResponseEntity.ok(result);
            }
            
            // Check if the map is empty
            if (credentials.isEmpty()) {
                result.put("status", "error");
                result.put("credentialsFound", true);
                result.put("credentialsEmpty", true);
                result.put("message", "Credentials exist but are empty");
                return ResponseEntity.ok(result);
            }
            
            // Check each required field
            result.put("credentialsFound", true);
            result.put("credentialsEmpty", false);
            
            // Check API Key
            String apiKey = credentials.get("apiKey");
            boolean apiKeyPresent = apiKey != null && !apiKey.isEmpty();
            result.put("apiKeyPresent", apiKeyPresent);
            if (apiKeyPresent && apiKey.length() > 8) {
                result.put("apiKeyPreview", apiKey.substring(0, 4) + "..." + apiKey.substring(apiKey.length() - 4));
            }
            
            // Check Private Key
            String privateKey = credentials.get("privateKey");
            boolean privateKeyPresent = privateKey != null && !privateKey.isEmpty();
            result.put("privateKeyPresent", privateKeyPresent);
            if (privateKeyPresent) {
                result.put("privateKeyLength", privateKey.length());
                result.put("privateKeyContainsPEM", privateKey.contains("-----BEGIN") && privateKey.contains("-----END"));
            }
            
            // Check Passphrase
            String passphrase = credentials.get("passphrase");
            boolean passphrasePresent = passphrase != null && !passphrase.isEmpty();
            result.put("passphrasePresent", passphrasePresent);
            
            // Overall status
            boolean allFieldsPresent = apiKeyPresent && privateKeyPresent && passphrasePresent;
            result.put("status", allFieldsPresent ? "success" : "incomplete");
            result.put("allRequiredFieldsPresent", allFieldsPresent);
            
            if (!allFieldsPresent) {
                result.put("message", "Credentials are incomplete. Missing fields: " + 
                    (!apiKeyPresent ? "apiKey " : "") +
                    (!privateKeyPresent ? "privateKey " : "") +
                    (!passphrasePresent ? "passphrase" : ""));
            } else {
                result.put("message", "All required credential fields are present");
            }
            
            // Show all keys in the credential map to check for naming inconsistencies
            result.put("availableKeys", credentials.keySet());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error checking credential completeness: {}", e.getMessage(), e);
            result.put("status", "error");
            result.put("error", e.getMessage());
            result.put("errorType", e.getClass().getName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
    
    /**
     * Get Coinbase API keys
     * 
     * @param httpRequest HTTP request
     * @return API keys
     */
    @GetMapping("/api-keys")
    @CrossOrigin(origins = {"http://localhost:3000", "https://strategiz.io"}, 
                allowedHeaders = {"Content-Type", "Authorization", "X-User-Email", "X-Admin-Request"}, 
                allowCredentials = "true")
    public ResponseEntity<Map<String, Object>> getApiKeys(HttpServletRequest httpRequest) {
        log.info("Received request to get Coinbase API keys");
        log.info("Request headers: {}", getHeadersInfo(httpRequest));
        log.info("Request parameters: {}", httpRequest.getParameterMap());
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Get the authenticated user's email from the request
            String userId = httpRequest.getHeader("X-User-Email");
            
            // Check if this is an admin request from the admin-coinbase page
            String referer = httpRequest.getHeader("Referer");
            boolean isAdminRequest = referer != null && 
                (referer.contains("/admin-coinbase") || referer.contains("/admin/coinbase"));
            
            if (userId == null || userId.isEmpty()) {
                log.warn("No user email provided in request header, falling back to parameter");
                userId = httpRequest.getParameter("email");
                
                if (userId == null || userId.isEmpty()) {
                    // For admin requests, use a default test user if no email is provided
                    if (isAdminRequest) {
                        log.info("Admin request detected, using default test user email");
                        userId = "admin@strategiz.io";
                    } else {
                        log.error("No user email provided");
                        result.put("status", "error");
                        result.put("message", "User email is required");
                        result.put("code", 400);
                        return ResponseEntity.badRequest().body(result);
                    }
                }
            }
            
            log.info("Getting Coinbase API keys for user: {}", userId);
            result.put("userId", userId);
            
            // Get Coinbase credentials from Firestore
            Map<String, String> credentials = firestoreService.getCoinbaseCredentials(userId);
            
            if (credentials == null || credentials.isEmpty()) {
                log.error("Coinbase API credentials not found for user: {}", userId);
                result.put("status", "error");
                result.put("message", "Coinbase API credentials not found");
                result.put("details", "Please add your Coinbase API credentials in the settings page");
                result.put("code", 404);
                return ResponseEntity.ok(result);
            }
            
            // Extract credentials
            String apiKey = credentials.get("apiKey");
            String privateKey = credentials.get("privateKey");
            String passphrase = credentials.get("passphrase");
            
            // Validate credentials
            if (apiKey == null || apiKey.isEmpty()) {
                log.error("API key is missing for user: {}", userId);
                result.put("status", "error");
                result.put("message", "API key is missing");
                result.put("details", "Please add a valid Coinbase API key in the settings page");
                result.put("code", 400);
                return ResponseEntity.ok(result);
            }
            
            if (privateKey == null || privateKey.isEmpty()) {
                log.error("Private key is missing for user: {}", userId);
                result.put("status", "error");
                result.put("message", "Private key is missing");
                result.put("details", "Please add a valid Coinbase private key in the settings page");
                result.put("code", 400);
                return ResponseEntity.ok(result);
            }
            
            // Mask the private key for security
            String maskedPrivateKey;
            if (privateKey.length() <= 8) {
                maskedPrivateKey = "********";
            } else {
                // Show first 4 and last 4 characters, mask the rest
                String firstFour = privateKey.substring(0, 4);
                String lastFour = privateKey.substring(privateKey.length() - 4);
                String masked = "*".repeat(Math.min(20, privateKey.length() - 8)); // Limit mask length
                maskedPrivateKey = firstFour + masked + lastFour;
            }
            
            // Create response
            result.put("apiKey", apiKey);
            result.put("privateKey", maskedPrivateKey);
            if (passphrase != null && !passphrase.isEmpty()) {
                result.put("passphrase", "[PASSPHRASE REDACTED]");
                result.put("hasPassphrase", true);
            } else {
                result.put("hasPassphrase", false);
            }
            result.put("source", "Firestore");
            result.put("status", "success");
            result.put("code", 200);
            
            // Try to validate the credentials by making a test connection
            try {
                boolean connectionSuccess = coinbaseCloudService.testConnection(apiKey, privateKey);
                result.put("connectionTested", true);
                result.put("connectionSuccess", connectionSuccess);
                
                if (!connectionSuccess) {
                    log.warn("Coinbase API credentials found but connection test failed for user: {}", userId);
                    result.put("warning", "Credentials found but API connection test failed. Please verify your Coinbase API credentials.");
                }
            } catch (Exception e) {
                log.warn("Error testing Coinbase API connection: {}", e.getMessage());
                result.put("connectionTested", true);
                result.put("connectionSuccess", false);
                result.put("connectionError", e.getMessage());
                result.put("warning", "Credentials found but API connection test failed with error: " + e.getMessage());
            }
            
            log.info("Successfully retrieved Coinbase API keys for user: {}", userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error getting Coinbase API keys: {}", e.getMessage(), e);
            result.put("status", "error");
            result.put("message", "Error getting Coinbase API keys: " + e.getMessage());
            result.put("code", 500);
            result.put("errorType", e.getClass().getName());
            return ResponseEntity.ok(result);
        }
    }
    
    /**
     * Check API credentials in Firestore
     * This endpoint checks all possible locations where Coinbase API credentials might be stored
     * 
     * @param httpRequest HTTP request
     * @return Credential check result
     */
    @GetMapping("/api-credentials/check")
    @CrossOrigin(origins = {"http://localhost:3000", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Map<String, Object>> checkApiCredentials(HttpServletRequest httpRequest) {
        Map<String, Object> result = new HashMap<>();
        
        // Get userId from request header or parameter
        String userId = httpRequest.getHeader("X-User-Email");
        if (userId == null || userId.isEmpty()) {
            userId = httpRequest.getParameter("email");
        }
        
        if (userId == null || userId.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "User email is required"
            ));
        }
        
        result.put("userId", userId);
        
        try {
            boolean legacyPathFound = false;
            boolean providerQueryFound = false;
            boolean newPathFound = false;
            boolean legacyProviderQueryFound = false;
            
            // Try the new api_credentials subcollection with 'provider' query
            log.info("Debug: Checking api_credentials subcollection with provider=coinbase query");
            try {
                QuerySnapshot apiCredentialsQuery = FirestoreClient.getFirestore()
                    .collection("users")
                    .document(userId)
                    .collection("api_credentials")
                    .whereEqualTo("provider", "coinbase")
                    .get()
                    .get();
                providerQueryFound = !apiCredentialsQuery.isEmpty();
                result.put("providerQueryFound", providerQueryFound);
            } catch (Exception fireStoreEx) {
                log.error("Error checking api_credentials with provider query: {}", fireStoreEx.getMessage());
                result.put("apiCredentialsQueryError", fireStoreEx.getMessage());
            }
            
            // Try direct document 'coinbase' in api_credentials subcollection
            log.info("Debug: Checking api_credentials/coinbase document");
            try {
                newPathFound = FirestoreClient.getFirestore()
                    .collection("users")
                    .document(userId)
                    .collection("api_credentials")
                    .document("coinbase")
                    .get()
                    .get()
                    .exists();
                result.put("newPathFound", newPathFound);
            } catch (Exception fireStoreEx) {
                log.error("Error checking api_credentials/coinbase: {}", fireStoreEx.getMessage());
                result.put("newPathError", fireStoreEx.getMessage());
            }
            
            // Try legacy credentials subcollection with 'provider' query
            log.info("Debug: Checking legacy credentials subcollection with provider=coinbase query");
            try {
                QuerySnapshot legacyCredentialsQuery = FirestoreClient.getFirestore()
                    .collection("users")
                    .document(userId)
                    .collection("credentials")
                    .whereEqualTo("provider", "coinbase")
                    .get()
                    .get();
                legacyProviderQueryFound = !legacyCredentialsQuery.isEmpty();
                result.put("legacyProviderQueryFound", legacyProviderQueryFound);
            } catch (Exception fireStoreEx) {
                log.error("Error checking legacy credentials query: {}", fireStoreEx.getMessage());
                result.put("legacyQueryError", fireStoreEx.getMessage());
            }
            
            // Try direct document 'coinbase' in legacy credentials subcollection
            log.info("Debug: Checking credentials/coinbase document");
            try {
                legacyPathFound = FirestoreClient.getFirestore()
                    .collection("users")
                    .document(userId)
                    .collection("credentials")
                    .document("coinbase")
                    .get()
                    .get()
                    .exists();
                result.put("legacyPathFound", legacyPathFound);
            } catch (Exception fireStoreEx) {
                log.error("Error checking legacy credentials/coinbase: {}", fireStoreEx.getMessage());
                result.put("legacyPathError", fireStoreEx.getMessage());
            }
            
            // Get credentials using our FirestoreService
            try {
                Map<String, String> credentials = firestoreService.getCoinbaseCredentials(userId);
                boolean serviceFound = credentials != null && !credentials.isEmpty();
                result.put("serviceFound", serviceFound);
                
                if (serviceFound && credentials != null) { 
                    result.put("hasApiKey", credentials.get("apiKey") != null && !credentials.get("apiKey").isEmpty());
                    result.put("hasPrivateKey", credentials.get("privateKey") != null && !credentials.get("privateKey").isEmpty());
                    result.put("hasPassphrase", credentials.get("passphrase") != null && !credentials.get("passphrase").isEmpty());
                    
                    // Don't include the actual values for security reasons, just mask them to verify retrieval
                    if (credentials.containsKey("apiKey") && credentials.get("apiKey") != null) {
                        String apiKey = credentials.get("apiKey");
                        result.put("apiKeyPrefix", apiKey.substring(0, Math.min(apiKey.length(), 5)) + "...");
                    }
                }
            } catch (Exception serviceEx) {
                log.error("Error getting credentials from service: {}", serviceEx.getMessage());
                result.put("serviceError", serviceEx.getMessage());
            }
            
            log.info("Debug: Credential check result: {}", result);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Debug: Unexpected error checking credentials: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", e.getMessage(),
                "errorType", e.getClass().getName()
            ));
        }
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        log.error("Unhandled exception in CoinbaseAdminController: {}", e.getMessage(), e);
        return ResponseEntity.ok(buildErrorResponse(
            500, // Error code
            "Server Error", // User-friendly message
            "An unexpected error occurred: " + e.getMessage(), // Developer message
            null, // No additional information
            e, // Include the exception
            "unknown" // No user email available
        ));
    }
    
    /**
     * Helper method to build standardized error responses
     * 
     * @param code Error code (HTTP status code-like)
     * @param message User-friendly error message
     * @param developerMessage Technical message for developers (can include stack trace)
     * @param moreInformation Additional details about the error
     * @param exception The exception that occurred, if any
     * @param userId User email or ID
     * @return A standardized error response map
     */
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
    
    private Map<String, Object> buildErrorResponse(int code, String message, String developerMessage, 
                                              Map<String, Object> moreInformation, Exception exception, String userId) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", "error");
        errorResponse.put("code", code);
        errorResponse.put("message", message);
        errorResponse.put("developerMessage", developerMessage);
        
        if (moreInformation != null) {
            errorResponse.put("moreInformation", moreInformation);
        }
        
        if (exception != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            exception.printStackTrace(pw);
            errorResponse.put("exception", exception.getClass().getName());
            errorResponse.put("exceptionMessage", exception.getMessage());
            errorResponse.put("stackTrace", sw.toString());
        }
        
        if (userId != null && !userId.isEmpty()) {
            errorResponse.put("userId", userId);
        }
        
        return errorResponse;
    }
}
