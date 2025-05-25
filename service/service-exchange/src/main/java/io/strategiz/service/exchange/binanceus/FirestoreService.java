package strategiz.service.exchange.binanceus;

import com.google.firebase.cloud.FirestoreClient;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.FirebaseApp;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for interacting with Firestore database for Binance US credentials
 */
@Service
@Slf4j
public class FirestoreService {

    /**
     * Get Binance US credentials for a user
     * 
     * @param userId User ID
     * @return Map of credentials (apiKey, secretKey)
     */
    public Map<String, String> getBinanceUSCredentials(String userId) {
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("Firebase not initialized. Cannot retrieve Binance US credentials.");
            return null;
        }
        
        try {
            log.info("Getting Binance US credentials for user: {}", userId);
            
            // Check 1: Look in the new api_credentials subcollection (new structure)
            log.info("Checking api_credentials subcollection for Binance US credentials");
            QuerySnapshot querySnapshot = FirestoreClient.getFirestore()
                .collection("users")
                .document(userId)
                .collection("api_credentials")
                .whereEqualTo("provider", "binanceus")
                .get()
                .get();
            
            if (!querySnapshot.isEmpty()) {
                DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                
                if (document.exists()) {
                    Map<String, String> credentials = new HashMap<>();
                    credentials.put("apiKey", document.getString("apiKey"));
                    credentials.put("secretKey", document.getString("secretKey"));
                    
                    log.info("Found Binance US credentials in api_credentials subcollection for user: {}", userId);
                    log.info("API key found (first 5 chars): {}", 
                        credentials.get("apiKey") != null ? 
                        credentials.get("apiKey").substring(0, Math.min(credentials.get("apiKey").length(), 5)) + "..." : "null");
                    log.info("Secret key found (length): {}", 
                        credentials.get("secretKey") != null ? 
                        credentials.get("secretKey").length() + " chars" : "null");
                    
                    return credentials;
                }
            } else {
                log.info("No Binance US credentials found in api_credentials subcollection");
            }
            
            // Check 2: Look in the legacy credentials subcollection
            log.info("Checking legacy credentials subcollection for Binance US credentials");
            DocumentSnapshot legacyDocument = FirestoreClient.getFirestore()
                .collection("users")
                .document(userId)
                .collection("credentials")
                .document("binanceus")
                .get()
                .get();
            
            if (legacyDocument.exists()) {
                Map<String, String> credentials = new HashMap<>();
                credentials.put("apiKey", legacyDocument.getString("apiKey"));
                credentials.put("secretKey", legacyDocument.getString("secretKey"));
                
                log.info("Found Binance US credentials in legacy credentials subcollection for user: {}", userId);
                log.info("API key found (first 5 chars): {}", 
                    credentials.get("apiKey") != null ? 
                    credentials.get("apiKey").substring(0, Math.min(credentials.get("apiKey").length(), 5)) + "..." : "null");
                log.info("Secret key found (length): {}", 
                    credentials.get("secretKey") != null ? 
                    credentials.get("secretKey").length() + " chars" : "null");
                
                // Migrate to new structure
                migrateToNewStructure(userId, legacyDocument.getString("apiKey"), legacyDocument.getString("secretKey"));
                
                return credentials;
            } else {
                log.info("No Binance US credentials found in legacy credentials subcollection");
            }
            
            // Check 3: Look in the user document preferences
            log.info("Checking user document preferences for Binance US credentials");
            DocumentSnapshot userDoc = FirestoreClient.getFirestore()
                .collection("users")
                .document(userId)
                .get()
                .get();
            
            if (userDoc.exists()) {
                Map<String, Object> userData = userDoc.getData();
                
                if (userData != null && userData.containsKey("binanceusConfig")) {
                    @SuppressWarnings("unchecked") // This cast is necessary as Firestore returns Object
                    Map<String, String> binanceusConfig = (Map<String, String>) userData.get("binanceusConfig");
                    
                    if (binanceusConfig != null && binanceusConfig.containsKey("apiKey") && binanceusConfig.containsKey("secretKey")) {
                        Map<String, String> credentials = new HashMap<>();
                        credentials.put("apiKey", binanceusConfig.get("apiKey"));
                        credentials.put("secretKey", binanceusConfig.get("secretKey"));
                        
                        log.info("Found Binance US credentials in user document preferences for user: {}", userId);
                        log.info("API key found (first 5 chars): {}", 
                            credentials.get("apiKey") != null ? 
                            credentials.get("apiKey").substring(0, Math.min(credentials.get("apiKey").length(), 5)) + "..." : "null");
                        log.info("Secret key found (length): {}", 
                            credentials.get("secretKey") != null ? 
                            credentials.get("secretKey").length() + " chars" : "null");
                        
                        // Migrate to new structure
                        migrateToNewStructure(userId, binanceusConfig.get("apiKey"), binanceusConfig.get("secretKey"));
                        
                        return credentials;
                    }
                }
                
                log.info("No Binance US credentials found in user document preferences");
            }
            
            // Check 4: Look in the binanceus_config collection
            log.info("Checking binanceus_config collection for Binance US credentials");
            DocumentSnapshot configDoc = FirestoreClient.getFirestore()
                .collection("binanceus_config")
                .document(userId)
                .get()
                .get();
            
            if (configDoc.exists()) {
                Map<String, String> credentials = new HashMap<>();
                credentials.put("apiKey", configDoc.getString("apiKey"));
                credentials.put("secretKey", configDoc.getString("secretKey"));
                
                log.info("Found Binance US credentials in binanceus_config collection for user: {}", userId);
                log.info("API key found (first 5 chars): {}", 
                    credentials.get("apiKey") != null ? 
                    credentials.get("apiKey").substring(0, Math.min(credentials.get("apiKey").length(), 5)) + "..." : "null");
                log.info("Secret key found (length): {}", 
                    credentials.get("secretKey") != null ? 
                    credentials.get("secretKey").length() + " chars" : "null");
                
                // Migrate to new structure
                migrateToNewStructure(userId, configDoc.getString("apiKey"), configDoc.getString("secretKey"));
                
                return credentials;
            } else {
                log.info("No Binance US credentials found in binanceus_config collection");
            }
            
            log.warn("No Binance US credentials found for user: {}", userId);
            return null;
        } catch (Exception e) {
            log.error("Error getting Binance US credentials: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Save Binance US credentials for a user
     * 
     * @param userId User ID
     * @param apiKey API key
     * @param secretKey Secret key
     */
    public void saveBinanceUSCredentials(String userId, String apiKey, String secretKey) {
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("Firebase not initialized. Cannot save Binance US credentials.");
            return;
        }
        
        try {
            log.info("Saving Binance US credentials for user: {}", userId);
            
            // Save to the new api_credentials subcollection
            Map<String, Object> credentials = new HashMap<>();
            credentials.put("provider", "binanceus");
            credentials.put("apiKey", apiKey);
            credentials.put("secretKey", secretKey);
            credentials.put("createdAt", System.currentTimeMillis());
            credentials.put("updatedAt", System.currentTimeMillis());
            
            FirestoreClient.getFirestore()
                .collection("users")
                .document(userId)
                .collection("api_credentials")
                .document("binanceus")
                .set(credentials)
                .get();
            
            log.info("Saved Binance US credentials to api_credentials subcollection for user: {}", userId);
        } catch (Exception e) {
            log.error("Error saving Binance US credentials: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Migrate credentials to the new structure
     * 
     * @param userId User ID
     * @param apiKey API key
     * @param secretKey Secret key
     */
    private void migrateToNewStructure(String userId, String apiKey, String secretKey) {
        try {
            log.info("Migrating Binance US credentials to new structure for user: {}", userId);
            
            // Save to the new api_credentials subcollection
            saveBinanceUSCredentials(userId, apiKey, secretKey);
            
            log.info("Successfully migrated Binance US credentials to new structure for user: {}", userId);
        } catch (Exception e) {
            log.error("Error migrating Binance US credentials to new structure: {}", e.getMessage(), e);
        }
    }
}
