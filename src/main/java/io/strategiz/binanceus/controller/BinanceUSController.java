package io.strategiz.binanceus.controller;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.cloud.FirestoreClient;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.DocumentReference;
import io.strategiz.framework.rest.controller.BaseServiceRestController;
import io.strategiz.binanceus.model.Account;
import io.strategiz.binanceus.model.Balance;
import io.strategiz.binanceus.model.request.BalanceRequest;
import io.strategiz.binanceus.model.response.BalanceResponse;
import io.strategiz.binanceus.model.response.ExchangeInfoResponse;
import io.strategiz.binanceus.model.response.StatusResponse;
import io.strategiz.binanceus.model.response.TickerPricesResponse;
import io.strategiz.binanceus.service.BinanceUSService;
import io.strategiz.binanceus.service.FirestoreService;
import io.strategiz.binanceus.util.AdminUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;

/**
 * Controller for Binance US API integration
 * Extends BaseServiceRestController from our custom framework
 */
@Slf4j
@RestController
@RequestMapping("/api/binanceus")
public class BinanceUSController extends BaseServiceRestController {

    @Autowired
    private BinanceUSService binanceUSService;

    @Autowired
    private FirestoreService firestoreService;

    @Autowired
    @Qualifier("binanceRestTemplate")
    private RestTemplate binanceRestTemplate;

    @PostMapping("/test-connection")
    public ResponseEntity<StatusResponse> testConnection(@RequestHeader("Authorization") String authHeader, @RequestBody Map<String, String> credentials) {
        try {
            String idToken = authHeader.replace("Bearer ", "");
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String userId = decodedToken.getUid();

            String apiKey = credentials.get("apiKey");
            String secretKey = credentials.get("secretKey");

            if (apiKey == null || secretKey == null) {
                return ResponseEntity.badRequest().body(StatusResponse.error("API key and secret key are required"));
            }

            Map<String, Object> result = binanceUSService.testConnection(apiKey, secretKey);
            if (result != null && "ok".equals(result.get("status"))) {
                // Save credentials to Firestore if test is successful
                firestoreService.saveExchangeCredentials(userId, "binanceus", apiKey, secretKey);
                return ResponseEntity.ok(StatusResponse.success("Connection successful"));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(StatusResponse.error("Invalid API credentials"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(StatusResponse.error("Error testing connection: " + e.getMessage()));
        }
    }

    @GetMapping("/exchange-info")
    public ResponseEntity<ExchangeInfoResponse> getExchangeInfo() {
        try {
            Object exchangeInfo = binanceUSService.getExchangeInfo();
            ExchangeInfoResponse response = new ExchangeInfoResponse();
            response.setExchangeInfo(exchangeInfo);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/ticker/prices")
    public ResponseEntity<TickerPricesResponse> getTickerPrices() {
        try {
            TickerPricesResponse response = new TickerPricesResponse();
            response.setTickerPrices(binanceUSService.getTickerPrices());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/balance/{userId}")
    public ResponseEntity<BalanceResponse> getBalanceByUserId(@PathVariable String userId, @RequestHeader("Authorization") String authHeader) {
        try {
            log.info("Received request for Binance US balance for user: {}", userId);
            
            String idToken = authHeader.replace("Bearer ", "");
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String requestingUserId = decodedToken.getUid();
            String requestingEmail = decodedToken.getEmail();

            log.info("Request from user: {}, email: {}", requestingUserId, requestingEmail);

            // Check if the requesting user is the same as the requested user or is an admin
            boolean isRequestingUser = requestingEmail != null && requestingEmail.equals(userId);
            boolean isAdmin = AdminUtil.isAdmin(decodedToken);
            
            if (!isRequestingUser && !isAdmin) {
                log.warn("Unauthorized access attempt: User {} attempted to access data for {}", requestingEmail, userId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
            }

            // Get Firestore instance
            Firestore firestore = FirestoreClient.getFirestore();
            
            // First try to get user document by ID
            DocumentSnapshot userDoc = null;
            try {
                userDoc = firestore.collection("users").document(userId).get().get();
            } catch (Exception e) {
                log.warn("Error fetching user document by ID: {}", e.getMessage());
            }
            
            // If user document not found by ID, try to query by email
            if (userDoc == null || !userDoc.exists()) {
                log.info("User document not found by ID, trying to query by email...");
                try {
                    // Query users collection where email field equals userId (which might be an email)
                    QuerySnapshot querySnapshot = firestore.collection("users")
                            .whereEqualTo("email", userId)
                            .limit(1)
                            .get()
                            .get();
                    
                    if (!querySnapshot.isEmpty()) {
                        userDoc = querySnapshot.getDocuments().get(0);
                        log.info("Found user document by email query");
                    } else {
                        log.warn("User document not found by email query for: {}", userId);
                    }
                } catch (Exception e) {
                    log.warn("Error querying user document by email: {}", e.getMessage());
                }
            }
            
            if (userDoc == null || !userDoc.exists()) {
                log.warn("User document not found for: {}", userId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
            
            // Extract user data and API keys using the same flexible approach
            Map<String, Object> userData = userDoc.getData();
            if (userData == null) {
                log.warn("User data is null for: {}", userId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
            
            // Now try to find the Binance US API keys in the user document
            // We'll check multiple possible locations
            Map<String, String> binanceusConfig = null;
            
            // Dump the entire user data structure for debugging
            log.info("User document data structure: {}", userData != null ? userData.keySet() : "null");
            
            // Check path: preferences.apiKeys.binanceus
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> preferences = (Map<String, Object>) userData.get("preferences");
                log.info("Preferences found: {}", preferences != null ? preferences.keySet() : "null");
                
                if (preferences != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> apiKeys = (Map<String, Object>) preferences.get("apiKeys");
                    log.info("API Keys found: {}", apiKeys != null ? apiKeys.keySet() : "null");
                    
                    if (apiKeys != null) {
                        @SuppressWarnings("unchecked")
                        Map<String, String> binanceusKeys = (Map<String, String>) apiKeys.get("binanceus");
                        log.info("Binance US keys found at preferences.apiKeys.binanceus: {}", 
                            binanceusKeys != null ? binanceusKeys.keySet() : "null");
                        
                        if (binanceusKeys != null) {
                            log.info("Found Binance US API keys at path: preferences.apiKeys.binanceus");
                            binanceusConfig = binanceusKeys;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Error checking preferences.apiKeys.binanceus path: {}", e.getMessage());
            }
            
            // If not found, check path: binanceusConfig
            if (binanceusConfig == null) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, String> binanceusKeys = (Map<String, String>) userData.get("binanceusConfig");
                    if (binanceusKeys != null) {
                        log.info("Found Binance US API keys at path: binanceusConfig");
                        binanceusConfig = binanceusKeys;
                    }
                } catch (Exception e) {
                    log.warn("Error checking binanceusConfig path: {}", e.getMessage());
                }
            }
            
            // If still not found, check path: apiKeys.binanceus
            if (binanceusConfig == null) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> apiKeys = (Map<String, Object>) userData.get("apiKeys");
                    if (apiKeys != null) {
                        @SuppressWarnings("unchecked")
                        Map<String, String> binanceusKeys = (Map<String, String>) apiKeys.get("binanceus");
                        if (binanceusKeys != null) {
                            log.info("Found Binance US API keys at path: apiKeys.binanceus");
                            binanceusConfig = binanceusKeys;
                        }
                    }
                } catch (Exception e) {
                    log.warn("Error checking apiKeys.binanceus path: {}", e.getMessage());
                }
            }
            
            // If still not found, check if the API keys are directly at the root level
            if (binanceusConfig == null) {
                try {
                    String apiKey = (String) userData.get("apiKey");
                    String secretKey = (String) userData.get("secretKey");
                    
                    if (apiKey != null && secretKey != null) {
                        log.info("Found Binance US API keys at root level");
                        binanceusConfig = new HashMap<>();
                        binanceusConfig.put("apiKey", apiKey);
                        binanceusConfig.put("secretKey", secretKey);
                    }
                } catch (Exception e) {
                    log.warn("Error checking root level API keys: {}", e.getMessage());
                }
            }
            
            if (binanceusConfig == null) {
                log.warn("Binance US API keys not configured for user: {}", userId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }
            
            String apiKey = binanceusConfig.get("apiKey");
            String secretKey = binanceusConfig.get("secretKey");
            
            // Get balance from Binance US API
            BalanceResponse response = new BalanceResponse();
            Account rawAccount = binanceUSService.getAccount(apiKey, secretKey);
            response.setRawAccountData(rawAccount); // Store the complete raw data
            List<Balance> balances = binanceUSService.getAccountBalance(apiKey, secretKey);
            response.setPositions(balances);
            response.setTotalUsdValue(binanceUSService.calculateTotalUsdValue(balances));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/balance")
    public ResponseEntity<BalanceResponse> getBalance(@RequestBody BalanceRequest request) {
        try {
            String apiKey = request.getApiKey();
            String secretKey = request.getSecretKey();

            if (apiKey == null || secretKey == null) {
                return ResponseEntity.badRequest().body(null);
            }

            BalanceResponse response = new BalanceResponse();
            Account rawAccount = binanceUSService.getAccount(apiKey, secretKey);
            response.setRawAccountData(rawAccount); // Store the complete raw data
            List<Balance> balances = binanceUSService.getAccountBalance(apiKey, secretKey);
            response.setPositions(balances);
            response.setTotalUsdValue(binanceUSService.calculateTotalUsdValue(balances));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * Get raw account data from Binance US API
     * This endpoint returns the completely unmodified raw data from Binance US API
     * 
     * @param request Request containing user ID
     * @return Raw account data from Binance US API
     */
    @PostMapping("/raw-data")
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://strategiz.io"}, allowedHeaders = "*", methods = {RequestMethod.POST, RequestMethod.OPTIONS})
    public ResponseEntity<Object> getRawAccountData(@RequestBody Map<String, String> request) {
        log.info("Received request for raw Binance US data: {}", request);
        
        try {
            // Extract user ID from the request (email address)
            String userId = request.get("userId");
            
            log.info("Extracted userId (email): {}", userId);
            
            if (userId == null) {
                log.warn("Missing user ID in request");
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "User ID is required"
                ));
            }
            
            log.info("Fetching Binance US API keys from Firestore for user: {}", userId);
            
            // Use the helper method to get Binance US API keys from Firestore
            Map<String, String> binanceusConfig = getBinanceUSApiKeysFromFirestore(userId);
            
            if (binanceusConfig == null || binanceusConfig.get("apiKey") == null || binanceusConfig.get("secretKey") == null) {
                log.warn("Binance US API keys not configured for user: {}", userId);
                
                // Check what's missing specifically to provide a more helpful error message
                String errorDetail;
                if (binanceusConfig == null) {
                    errorDetail = "No Binance US configuration found";
                } else if (binanceusConfig.get("apiKey") == null) {
                    errorDetail = "API key is missing";
                } else {
                    errorDetail = "Secret key is missing";
                }
                
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", "error",
                    "message", "Binance US API keys not configured: " + errorDetail,
                    "userEmail", userId
                ));
            }
            
            String apiKey = binanceusConfig.get("apiKey");
            String secretKey = binanceusConfig.get("secretKey");
            
            log.info("Successfully retrieved Binance US API keys from Firebase for user: {}", userId);
            log.info("Fetching raw account data from Binance US API with key: {}", apiKey.substring(0, Math.min(5, apiKey.length())) + "...");
            
            try {
                // First try to get ticker prices to test connectivity
                try {
                    log.info("Testing connection with ticker prices endpoint");
                    binanceUSService.getTickerPrices();
                    log.info("Successfully connected to Binance US API");
                } catch (Exception e) {
                    log.error("Error connecting to Binance US API: {}", e.getMessage(), e);
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Error connecting to Binance US API: " + e.getMessage());
                    errorResponse.put("error", e.getClass().getSimpleName());
                    
                    if (e instanceof ResourceAccessException) {
                        log.error("Network error connecting to Binance US API", e);
                        errorResponse.put("detail", "Network error: Unable to connect to Binance US API");
                        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
                    } else if (e instanceof HttpClientErrorException) {
                        HttpClientErrorException clientEx = (HttpClientErrorException) e;
                        log.error("Client error from Binance US API: {} - {}", clientEx.getStatusCode(), clientEx.getResponseBodyAsString());
                        errorResponse.put("detail", "Client error: " + clientEx.getStatusCode() + " - " + clientEx.getResponseBodyAsString());
                        return ResponseEntity.status(clientEx.getStatusCode()).body(errorResponse);
                    } else if (e instanceof HttpServerErrorException) {
                        HttpServerErrorException serverEx = (HttpServerErrorException) e;
                        log.error("Server error from Binance US API: {} - {}", serverEx.getStatusCode(), serverEx.getResponseBodyAsString());
                        errorResponse.put("detail", "Server error: " + serverEx.getStatusCode() + " - " + serverEx.getResponseBodyAsString());
                        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorResponse);
                    }
                    
                    return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorResponse);
                }
                
                try {
                    // Get the complete unmodified raw data from the Binance US API
                    log.info("Fetching account data with provided credentials");
                    Object rawAccount = binanceUSService.signedRequest(
                        HttpMethod.GET, 
                        "/api/v3/account", 
                        new HashMap<>(), 
                        apiKey, 
                        secretKey, 
                        Object.class
                    );
                    
                    log.info("Successfully retrieved raw account data from Binance US API");
                    
                    // Return the completely unmodified raw data directly as Object
                    // This ensures the frontend gets exactly what comes from the Binance US API
                    // without any transformations or mappings
                    return ResponseEntity.ok(rawAccount);
                } catch (Exception e) {
                    log.error("Error from Binance US API: {}", e.getMessage(), e);
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Error from Binance US API: " + e.getMessage());
                    errorResponse.put("error", e.getClass().getSimpleName());
                    
                    if (e instanceof HttpClientErrorException) {
                        HttpClientErrorException clientEx = (HttpClientErrorException) e;
                        log.error("Client error from Binance US API: {} - {}", clientEx.getStatusCode(), clientEx.getResponseBodyAsString());
                        errorResponse.put("detail", "Client error: " + clientEx.getStatusCode() + " - " + clientEx.getResponseBodyAsString());
                        return ResponseEntity.status(clientEx.getStatusCode()).body(errorResponse);
                    }
                    
                    return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorResponse);
                }
            } catch (Exception e) {
                log.error("Unexpected error processing request: {}", e.getMessage(), e);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "Unexpected error: " + e.getMessage());
                errorResponse.put("error", e.getClass().getSimpleName());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        } catch (Exception e) {
            log.error("Unexpected error processing request: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Unexpected error: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Admin endpoint to get completely unmodified raw data directly from the Binance US API
     * This endpoint bypasses all transformations and returns the exact JSON response from the API
     * 
     * @param token Firebase ID token for authentication
     * @return Raw JSON response from Binance US API
     */
    @GetMapping("/admin/raw-data")
    public ResponseEntity<Object> getAdminRawAccountData(@RequestHeader("Authorization") String authHeader) {
        try {
            log.info("Received request for raw Binance US account data for admin");
            
            // Extract token from Authorization header
            String token = authHeader.replace("Bearer ", "");
            if (token == null || token.isEmpty()) {
                log.error("No token provided in Authorization header");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "No token provided"));
            }
            
            // Verify the Firebase token
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
            String userId = decodedToken.getUid();
            String email = decodedToken.getEmail();
            
            log.info("Verified Firebase token for user: {} ({})", email, userId);
            
            // Get Binance US credentials from Firestore
            // Using the new structure with api_credentials subcollection
            Map<String, String> credentials = getBinanceUSApiKeysFromFirestore(userId);
            
            if (credentials == null || credentials.isEmpty() || 
                !credentials.containsKey("apiKey") || !credentials.containsKey("secretKey")) {
                log.error("Binance US API credentials not found for user: {}", userId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Binance US API credentials not configured"));
            }
            
            String apiKey = credentials.get("apiKey");
            String secretKey = credentials.get("secretKey");
            
            log.info("Retrieved Binance US API credentials for user: {}", userId);
            log.info("API Key: {}...", apiKey.substring(0, Math.min(5, apiKey.length())));
            
            // Get raw account data from Binance US API
            String rawData = binanceUSService.getRawAccountData(apiKey, secretKey);
            
            // Parse the raw data to ensure it's valid JSON
            ObjectMapper mapper = new ObjectMapper();
            JsonNode parsedResponse = mapper.readTree(rawData);
            
            log.info("Successfully retrieved raw Binance US account data");
            
            // Return the completely unmodified raw data
            return ResponseEntity.ok().body(Map.of(
                "parsedResponse", parsedResponse,
                "rawResponse", rawData
            ));
        } catch (FirebaseAuthException e) {
            log.error("Firebase authentication error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Invalid Firebase token: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting raw Binance US account data: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Error getting raw Binance US account data: " + e.getMessage()));
        }
    }
    
    /**
     * Get Binance US API keys from Firestore using the new api_credentials subcollection structure
     * 
     * @param userId Firebase user ID
     * @return Map containing apiKey and secretKey
     */
    private Map<String, String> getBinanceUSApiKeysFromFirestore(String userId) {
        log.info("Getting Binance US API keys from Firestore for user: {}", userId);
        Map<String, String> apiCredentials = new HashMap<>();
        
        try {
            // Get Firestore instance
            Firestore firestore = FirestoreClient.getFirestore();
            
            // First try to get user document by ID
            DocumentReference userDocRef = firestore.collection("users").document(userId);
            DocumentSnapshot userDoc = userDocRef.get().get();
            
            // If user document not found by ID, try to query by email
            if (!userDoc.exists()) {
                log.info("User document not found by ID, trying to query by email...");
                try {
                    // Query users collection where email field equals userId (which might be an email)
                    QuerySnapshot querySnapshot = firestore.collection("users")
                            .whereEqualTo("email", userId)
                            .limit(1)
                            .get()
                            .get();
                    
                    if (!querySnapshot.isEmpty()) {
                        userDocRef = querySnapshot.getDocuments().get(0).getReference();
                        userDoc = userDocRef.get().get();
                        log.info("Found user document by email query");
                    } else {
                        log.warn("User document not found by email query for: {}", userId);
                        return null;
                    }
                } catch (Exception e) {
                    log.warn("Error querying user document by email: {}", e.getMessage());
                    return null;
                }
            }
            
            // Check 1: Look in the new api_credentials subcollection (new structure)
            DocumentReference apiCredentialsDocRef = userDocRef.collection("api_credentials").document("binanceus");
            DocumentSnapshot apiCredentialsSnapshot = apiCredentialsDocRef.get().get();
            
            if (apiCredentialsSnapshot.exists()) {
                apiCredentials.put("apiKey", apiCredentialsSnapshot.getString("apiKey"));
                apiCredentials.put("secretKey", apiCredentialsSnapshot.getString("secretKey"));
                
                log.info("Found Binance US credentials in api_credentials subcollection for user: {}", userId);
                return apiCredentials;
            }
            
            // Check 2: Look in the legacy locations
            Map<String, Object> userData = userDoc.getData();
            if (userData == null) {
                log.warn("User data is null for user: {}", userId);
                return null;
            }
            
            // Check path: preferences.apiKeys.binanceus
            try {
                Object preferencesObj = userData.get("preferences");
                if (preferencesObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> preferences = (Map<String, Object>) preferencesObj;
                    Object apiKeysObj = preferences.get("apiKeys");
                    if (apiKeysObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> apiKeys = (Map<String, Object>) apiKeysObj;
                        Object binanceusKeysObj = apiKeys.get("binanceus");
                        if (binanceusKeysObj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, String> binanceusKeys = (Map<String, String>) binanceusKeysObj;
                            
                            if (binanceusKeys != null) {
                                log.info("Found Binance US API keys at path: preferences.apiKeys.binanceus");
                                
                                // Migrate to new structure
                                migrateToNewStructure(userDocRef, binanceusKeys.get("apiKey"), binanceusKeys.get("secretKey") != null ? 
                                    binanceusKeys.get("secretKey") : binanceusKeys.get("privateKey"));
                                
                                return binanceusKeys;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Error checking preferences.apiKeys.binanceus path: {}", e.getMessage());
            }
            
            // If not found, check path: binanceusConfig
            try {
                @SuppressWarnings("unchecked")
                Map<String, String> binanceusKeys = (Map<String, String>) userData.get("binanceusConfig");
                if (binanceusKeys != null) {
                    log.info("Found Binance US API keys at path: binanceusConfig");
                    
                    // Migrate to new structure
                    migrateToNewStructure(userDocRef, binanceusKeys.get("apiKey"), binanceusKeys.get("secretKey"));
                    
                    return binanceusKeys;
                }
            } catch (Exception e) {
                log.warn("Error checking binanceusConfig path: {}", e.getMessage());
            }
            
            log.warn("No Binance US API keys found for user: {}", userId);
            return null;
        } catch (Exception e) {
            log.error("Error retrieving Binance US API keys: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Migrate Binance US API keys to the new structure
     * 
     * @param userDocRef User document reference
     * @param apiKey API key
     * @param secretKey Secret key
     */
    private void migrateToNewStructure(DocumentReference userDocRef, String apiKey, String secretKey) {
        try {
            if (apiKey == null || secretKey == null) {
                log.warn("Cannot migrate null API keys to new structure");
                return;
            }
            
            // Save to the new api_credentials subcollection
            Map<String, Object> credentials = new HashMap<>();
            credentials.put("provider", "binanceus");
            credentials.put("apiKey", apiKey);
            credentials.put("secretKey", secretKey);
            credentials.put("updatedAt", System.currentTimeMillis());
            
            userDocRef.collection("api_credentials").document("binanceus").set(credentials);
            
            log.info("Migrated Binance US API keys to new api_credentials subcollection structure");
        } catch (Exception e) {
            log.error("Error migrating Binance US API keys to new structure: {}", e.getMessage(), e);
        }
    }
}
