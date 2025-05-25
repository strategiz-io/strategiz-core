package strategiz.service.exchange.binanceus.util;

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
 * Helper class for Binance US configuration management
 */
@Component
public class BinanceUSConfigHelper {
    
    private static final Logger log = LoggerFactory.getLogger(BinanceUSConfigHelper.class);
    
    /**
     * Check if Binance US API keys are configured for a user
     * 
     * @param email User email
     * @return Map containing configuration status and details
     */
    public Map<String, Object> checkBinanceUSConfig(String email) {
        log.info("Checking Binance US configuration for user: {}", email);
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
            
            // Check for Binance US configuration in the api_credentials subcollection (new structure)
            DocumentSnapshot apiCredentialsDoc = firestore.collection("users")
                    .document(userId)
                    .collection("api_credentials")
                    .document("binanceus")
                    .get()
                    .get();
            
            boolean hasApiKeys = false;
            
            if (apiCredentialsDoc.exists()) {
                Map<String, Object> apiCredentials = apiCredentialsDoc.getData();
                if (apiCredentials != null && 
                    apiCredentials.containsKey("apiKey") && 
                    (apiCredentials.containsKey("privateKey") || apiCredentials.containsKey("secretKey"))) {
                    hasApiKeys = true;
                    result.put("apiCredentialsConfig", apiCredentials);
                    result.put("apiCredentialsConfigComplete", true);
                }
            }
            
            // Check preferences.apiKeys.binanceus (old structure)
            if (userData.containsKey("preferences")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> preferences = (Map<String, Object>) userData.get("preferences");
                
                if (preferences != null && preferences.containsKey("apiKeys")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> apiKeys = (Map<String, Object>) preferences.get("apiKeys");
                    
                    if (apiKeys != null && apiKeys.containsKey("binanceus")) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> binanceusConfig = (Map<String, Object>) apiKeys.get("binanceus");
                        
                        if (binanceusConfig != null && 
                            binanceusConfig.containsKey("apiKey") && 
                            (binanceusConfig.containsKey("privateKey") || binanceusConfig.containsKey("secretKey"))) {
                            hasApiKeys = true;
                            result.put("preferencesConfig", binanceusConfig);
                            result.put("preferencesConfigComplete", true);
                        }
                    }
                }
            }
            
            result.put("hasApiKeys", hasApiKeys);
            
            return result;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error checking Binance US configuration for user: {}", email, e);
            result.put("status", "error");
            result.put("message", "Error checking configuration: " + e.getMessage());
            return result;
        }
    }
    
    /**
     * Add Binance US configuration for a user
     * 
     * @param email User email
     * @param apiKey Binance US API key
     * @param privateKey Binance US private key (secret key)
     * @return Map containing operation status and details
     */
    public Map<String, Object> addBinanceUSConfig(String email, String apiKey, String privateKey) {
        log.info("Adding Binance US configuration for user: {}", email);
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
                    .document("binanceus")
                    .set(apiCredentials)
                    .get();
            
            log.info("Saved Binance US API credentials to api_credentials subcollection for user: {}", email);
            
            result.put("status", "success");
            result.put("message", "Binance US configuration added successfully");
            result.put("email", email);
            result.put("documentId", userId);
            
            return result;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error adding Binance US configuration for user: {}", email, e);
            result.put("status", "error");
            result.put("message", "Error adding configuration: " + e.getMessage());
            return result;
        }
    }
}
