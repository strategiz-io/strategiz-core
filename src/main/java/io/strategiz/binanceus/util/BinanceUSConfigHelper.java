package io.strategiz.binanceus.util;

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
                        
                        if (binanceusConfig != null) {
                            hasApiKeys = true;
                            result.put("preferencesBinanceusConfig", binanceusConfig);
                            result.put("preferencesBinanceusConfigComplete", 
                                binanceusConfig.containsKey("apiKey") && (binanceusConfig.containsKey("privateKey") || binanceusConfig.containsKey("secretKey")));
                            
                            // Migrate to new structure if not already there
                            if (!apiCredentialsDoc.exists() && 
                                binanceusConfig.containsKey("apiKey") && 
                                (binanceusConfig.containsKey("privateKey") || binanceusConfig.containsKey("secretKey"))) {
                                String oldApiKey = (String) binanceusConfig.get("apiKey");
                                String oldKey = (String) (binanceusConfig.containsKey("privateKey") ? 
                                    binanceusConfig.get("privateKey") : binanceusConfig.get("secretKey"));
                                
                                // Migrate keys to new structure
                                Map<String, Object> newCredentials = new HashMap<>();
                                newCredentials.put("apiKey", oldApiKey);
                                newCredentials.put("privateKey", oldKey);
                                newCredentials.put("secretKey", oldKey); // For backward compatibility
                                newCredentials.put("createdAt", System.currentTimeMillis());
                                newCredentials.put("updatedAt", System.currentTimeMillis());
                                newCredentials.put("source", "migrated_from_preferences");
                                
                                firestore.collection("users")
                                    .document(userId)
                                    .collection("api_credentials")
                                    .document("binanceus")
                                    .set(newCredentials)
                                    .get();
                                
                                log.info("Migrated API keys from preferences.apiKeys.binanceus to api_credentials subcollection for user: {}", email);
                                result.put("migrated", true);
                            }
                        }
                    }
                }
            }
            
            // Check root binanceusConfig (old structure)
            if (userData.containsKey("binanceusConfig")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> binanceusConfig = (Map<String, Object>) userData.get("binanceusConfig");
                
                if (binanceusConfig != null) {
                    hasApiKeys = true;
                    result.put("rootBinanceusConfig", binanceusConfig);
                    result.put("rootBinanceusConfigComplete", 
                        binanceusConfig.containsKey("apiKey") && (binanceusConfig.containsKey("privateKey") || binanceusConfig.containsKey("secretKey")));
                    
                    // Migrate to new structure if not already there
                    if (!apiCredentialsDoc.exists() && 
                        binanceusConfig.containsKey("apiKey") && 
                        (binanceusConfig.containsKey("privateKey") || binanceusConfig.containsKey("secretKey"))) {
                        String oldApiKey = (String) binanceusConfig.get("apiKey");
                        String oldKey = (String) (binanceusConfig.containsKey("privateKey") ? 
                            binanceusConfig.get("privateKey") : binanceusConfig.get("secretKey"));
                        
                        // Migrate keys to new structure
                        Map<String, Object> newCredentials = new HashMap<>();
                        newCredentials.put("apiKey", oldApiKey);
                        newCredentials.put("privateKey", oldKey);
                        newCredentials.put("secretKey", oldKey); // For backward compatibility
                        newCredentials.put("createdAt", System.currentTimeMillis());
                        newCredentials.put("updatedAt", System.currentTimeMillis());
                        newCredentials.put("source", "migrated_from_root_config");
                        
                        firestore.collection("users")
                            .document(userId)
                            .collection("api_credentials")
                            .document("binanceus")
                            .set(newCredentials)
                            .get();
                        
                        log.info("Migrated API keys from binanceusConfig to api_credentials subcollection for user: {}", email);
                        result.put("migrated", true);
                    }
                }
            }
            
            // Check root apiKeys.binanceus (old structure)
            if (userData.containsKey("apiKeys")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> apiKeys = (Map<String, Object>) userData.get("apiKeys");
                
                if (apiKeys != null && apiKeys.containsKey("binanceus")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> binanceusConfig = (Map<String, Object>) apiKeys.get("binanceus");
                    
                    if (binanceusConfig != null) {
                        hasApiKeys = true;
                        result.put("rootApiKeysBinanceusConfig", binanceusConfig);
                        result.put("rootApiKeysBinanceusConfigComplete", 
                            binanceusConfig.containsKey("apiKey") && (binanceusConfig.containsKey("privateKey") || binanceusConfig.containsKey("secretKey")));
                        
                        // Migrate to new structure if not already there
                        if (!apiCredentialsDoc.exists() && 
                            binanceusConfig.containsKey("apiKey") && 
                            (binanceusConfig.containsKey("privateKey") || binanceusConfig.containsKey("secretKey"))) {
                            String oldApiKey = (String) binanceusConfig.get("apiKey");
                            String oldKey = (String) (binanceusConfig.containsKey("privateKey") ? 
                                binanceusConfig.get("privateKey") : binanceusConfig.get("secretKey"));
                            
                            // Migrate keys to new structure
                            Map<String, Object> newCredentials = new HashMap<>();
                            newCredentials.put("apiKey", oldApiKey);
                            newCredentials.put("privateKey", oldKey);
                            newCredentials.put("secretKey", oldKey); // For backward compatibility
                            newCredentials.put("createdAt", System.currentTimeMillis());
                            newCredentials.put("updatedAt", System.currentTimeMillis());
                            newCredentials.put("source", "migrated_from_root_apiKeys");
                            
                            firestore.collection("users")
                                .document(userId)
                                .collection("api_credentials")
                                .document("binanceus")
                                .set(newCredentials)
                                .get();
                            
                            log.info("Migrated API keys from apiKeys.binanceus to api_credentials subcollection for user: {}", email);
                            result.put("migrated", true);
                        }
                    }
                }
            }
            
            result.put("hasBinanceusConfig", hasApiKeys);
            
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error checking Binance US configuration: {}", e.getMessage(), e);
            result.put("status", "error");
            result.put("message", "Error checking Binance US configuration: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Add Binance US configuration to a user document
     * 
     * @param email User email
     * @param apiKey Binance US API key
     * @param privateKey Binance US private key
     * @return Map containing operation status and details
     */
    public Map<String, Object> addBinanceUSConfig(String email, String apiKey, String privateKey) {
        log.info("Adding Binance US configuration for user: {}", email);
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Get Firestore instance
            Firestore firestore = FirestoreClient.getFirestore();
            
            // Query users collection where email field equals the provided email
            QuerySnapshot querySnapshot = firestore.collection("users")
                    .whereEqualTo("email", email)
                    .limit(1)
                    .get()
                    .get();
            
            if (querySnapshot.isEmpty()) {
                result.put("status", "error");
                result.put("message", "User not found with email: " + email);
                return result;
            }
            
            DocumentSnapshot userDoc = querySnapshot.getDocuments().get(0);
            String userId = userDoc.getId();
            
            // Create Binance US configuration map for the new structure
            Map<String, Object> binanceusConfig = new HashMap<>();
            binanceusConfig.put("apiKey", apiKey);
            binanceusConfig.put("privateKey", privateKey);
            binanceusConfig.put("secretKey", privateKey); // Keep for backward compatibility
            binanceusConfig.put("createdAt", System.currentTimeMillis());
            binanceusConfig.put("updatedAt", System.currentTimeMillis());
            binanceusConfig.put("source", "direct_config");
            
            // Store in the new api_credentials subcollection
            firestore.collection("users")
                .document(userId)
                .collection("api_credentials")
                .document("binanceus")
                .set(binanceusConfig)
                .get();
            
            // For backward compatibility, also update the old structure
            Map<String, Object> updates = new HashMap<>();
            Map<String, String> oldFormatConfig = new HashMap<>();
            oldFormatConfig.put("apiKey", apiKey);
            oldFormatConfig.put("privateKey", privateKey);
            oldFormatConfig.put("secretKey", privateKey); // Keep for backward compatibility
            updates.put("preferences.apiKeys.binanceus", oldFormatConfig);
            
            // Update the user document with old structure for backward compatibility
            firestore.collection("users").document(userId).update(updates).get();
            
            result.put("status", "success");
            result.put("message", "Binance US configuration added successfully");
            result.put("email", email);
            result.put("documentId", userId);
            
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error adding Binance US configuration: {}", e.getMessage(), e);
            result.put("status", "error");
            result.put("message", "Error adding Binance US configuration: " + e.getMessage());
        }
        
        return result;
    }
}
