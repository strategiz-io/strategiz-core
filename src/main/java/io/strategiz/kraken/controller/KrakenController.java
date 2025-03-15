package io.strategiz.kraken.controller;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import io.strategiz.kraken.model.KrakenAccount;
import io.strategiz.kraken.service.FirestoreService;
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
     * @param authHeader Authorization header containing Firebase ID token
     * @return Raw account data from Kraken API
     */
    @GetMapping("/raw-data/user")
    @CrossOrigin(origins = {"http://localhost:3000", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Object> getRawAccountDataForUser(@RequestHeader("Authorization") String authHeader) {
        try {
            log.info("Received request for raw Kraken data using stored credentials");
            
            // Verify the Firebase ID token
            String idToken = authHeader.replace("Bearer ", "");
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String userId = decodedToken.getUid();
            
            log.info("Authenticated user: {}", userId);
            
            // Retrieve the user's Kraken credentials from Firestore
            Map<String, String> credentials = firestoreService.getKrakenCredentials(userId);
            
            if (credentials == null || credentials.get("apiKey") == null || credentials.get("secretKey") == null) {
                log.warn("No Kraken credentials found for user: {}", userId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "No Kraken credentials found. Please configure your Kraken API keys in settings."
                ));
            }
            
            String apiKey = credentials.get("apiKey");
            String secretKey = credentials.get("secretKey");
            
            log.info("Retrieved Kraken credentials for user: {}", userId);
            
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
