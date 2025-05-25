package io.strategiz.service.exchange.coinbase.util;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Helper class for Coinbase configuration management
 */
@Component
public class CoinbaseConfigHelper {
    
    private static final Logger log = LoggerFactory.getLogger(CoinbaseConfigHelper.class);
    
    /**
     * Check if Coinbase API keys are configured for a user
     * 
     * @param email User email
     * @return Map containing configuration status and details
     */
    public Map<String, Object> checkCoinbaseConfig(String email) {
        log.info("Checking Coinbase configuration for user: {}", email);
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Get Firestore instance
            Firestore firestore = FirestoreClient.getFirestore();
            
            // Query users collection where email field equals the provided email
            log.info("Querying users collection where email = {}", email);
            QuerySnapshot querySnapshot = firestore.collection("users")
                    .whereEqualTo("email", email)
                    .limit(1)
                    .get()
                    .get();
            
            log.info("Query results: isEmpty={}, size={}", querySnapshot.isEmpty(), querySnapshot.size());
            
            if (querySnapshot.isEmpty()) {
                result.put("status", "error");
                result.put("message", "User not found with email: " + email);
                return result;
            }
            
            DocumentSnapshot userDoc = querySnapshot.getDocuments().get(0);
            String userId = userDoc.getId();
            log.info("Found user document: id={}", userId);
            
            Map<String, Object> userData = userDoc.getData();
            if (userData == null) {
                result.put("status", "error");
                result.put("message", "User data is null for email: " + email);
                return result;
            }
            
            result.put("status", "success");
            result.put("email", email);
            result.put("documentId", userId);
            
            // Check for Coinbase configuration in the api_credentials subcollection (new structure)
            DocumentSnapshot apiCredentialsDoc = firestore.collection("users")
                    .document(userId)
                    .collection("api_credentials")
                    .document("coinbase")
                    .get()
                    .get();
            
            boolean hasApiKeys = false;
            
            if (apiCredentialsDoc.exists()) {
                Map<String, Object> apiCredentials = apiCredentialsDoc.getData();
                if (apiCredentials != null && 
                    apiCredentials.containsKey("apiKey") && 
                    apiCredentials.containsKey("privateKey")) {
                    hasApiKeys = true;
                    result.put("apiCredentialsConfig", apiCredentials);
                    result.put("apiCredentialsConfigComplete", true);
                }
            }
            
            // Check preferences.apiKeys.coinbase (old structure)
            if (userData.containsKey("preferences")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> preferences = (Map<String, Object>) userData.get("preferences");
                
                if (preferences != null && preferences.containsKey("apiKeys")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> apiKeys = (Map<String, Object>) preferences.get("apiKeys");
                    
                    if (apiKeys != null && apiKeys.containsKey("coinbase")) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> coinbaseConfig = (Map<String, Object>) apiKeys.get("coinbase");
                        
                        if (coinbaseConfig != null && 
                            coinbaseConfig.containsKey("apiKey") && 
                            coinbaseConfig.containsKey("privateKey")) {
                            hasApiKeys = true;
                            result.put("preferencesConfig", coinbaseConfig);
                            result.put("preferencesConfigComplete", true);
                        }
                    }
                }
            }
            
            result.put("hasApiKeys", hasApiKeys);
            
            return result;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error checking Coinbase configuration for user: {}", email, e);
            result.put("status", "error");
            result.put("message", "Error checking configuration: " + e.getMessage());
            return result;
        }
    }
    
    /**
     * Add Coinbase configuration for a user
     * 
     * @param email User email
     * @param apiKey Coinbase API key
     * @param privateKey Coinbase private key
     * @return Map containing operation status and details
     */
    public Map<String, Object> addCoinbaseConfig(String email, String apiKey, String privateKey) {
        log.info("Adding Coinbase configuration for user: {}", email);
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Get Firestore instance
            Firestore firestore = FirestoreClient.getFirestore();
            
            // Query users collection where email field equals the provided email
            log.info("Querying users collection where email = {}", email);
            QuerySnapshot querySnapshot = firestore.collection("users")
                    .whereEqualTo("email", email)
                    .limit(1)
                    .get()
                    .get();
            
            log.info("Query results: isEmpty={}, size={}", querySnapshot.isEmpty(), querySnapshot.size());
            
            if (querySnapshot.isEmpty()) {
                result.put("status", "error");
                result.put("message", "User not found with email: " + email);
                return result;
            }
            
            DocumentSnapshot userDoc = querySnapshot.getDocuments().get(0);
            String userId = userDoc.getId();
            log.info("Found user document: id={}", userId);
            
            // Store API credentials in the api_credentials subcollection (new structure)
            Map<String, Object> apiCredentials = new HashMap<>();
            apiCredentials.put("apiKey", apiKey);
            apiCredentials.put("privateKey", privateKey);
            apiCredentials.put("timestamp", System.currentTimeMillis());
            
            firestore.collection("users")
                    .document(userId)
                    .collection("api_credentials")
                    .document("coinbase")
                    .set(apiCredentials)
                    .get();
            
            log.info("Saved Coinbase API credentials to api_credentials subcollection for user: {}", email);
            
            result.put("status", "success");
            result.put("message", "Coinbase configuration added successfully");
            result.put("email", email);
            result.put("documentId", userId);
            
            return result;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error adding Coinbase configuration for user: {}", email, e);
            result.put("status", "error");
            result.put("message", "Error adding configuration: " + e.getMessage());
            return result;
        }
    }
}
