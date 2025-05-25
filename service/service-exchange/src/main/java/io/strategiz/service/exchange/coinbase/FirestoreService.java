package strategiz.service.exchange.coinbase;

import com.google.firebase.cloud.FirestoreClient;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.FirebaseApp;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for interacting with Firestore database for Coinbase credentials
 * This service implements the new Firebase database structure for storing API credentials
 * as mentioned in the project requirements.
 */
@Service("coinbaseFirestoreService")
@Slf4j
public class FirestoreService {

    /**
     * Get Coinbase credentials for a user
     * 
     * @param userId User ID
     * @return Map of credentials (apiKey, privateKey)
     */
    public Map<String, String> getCoinbaseCredentials(String userId) {
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("Firebase not initialized. Cannot retrieve Coinbase credentials.");
            return null;
        }
        
        try {
            log.info("Getting Coinbase credentials for user: {}", userId);
            
            // Check 1: Look in the new api_credentials subcollection (new structure)
            log.info("Checking api_credentials subcollection for Coinbase credentials");
            QuerySnapshot querySnapshot = FirestoreClient.getFirestore()
                .collection("users")
                .document(userId)
                .collection("api_credentials")
                .whereEqualTo("provider", "coinbase")
                .get()
                .get();
            
            if (!querySnapshot.isEmpty()) {
                DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                
                if (document.exists()) {
                    Map<String, String> credentials = new HashMap<>();
                    credentials.put("apiKey", document.getString("apiKey"));
                    credentials.put("privateKey", document.getString("privateKey"));
                    
                    log.info("Found Coinbase credentials in api_credentials subcollection for user: {}", userId);
                    log.info("API key found (first 5 chars): {}", 
                        credentials.get("apiKey") != null ? 
                        credentials.get("apiKey").substring(0, Math.min(credentials.get("apiKey").length(), 5)) + "..." : "null");
                    log.info("Private key found (length): {}", 
                        credentials.get("privateKey") != null ? 
                        credentials.get("privateKey").length() + " chars" : "null");
                    
                    return credentials;
                }
            } else {
                log.info("No Coinbase credentials found in api_credentials subcollection");
            }
            
            // Check 2: Look in the legacy credentials subcollection
            log.info("Checking legacy credentials subcollection for Coinbase credentials");
            DocumentSnapshot legacyDocument = FirestoreClient.getFirestore()
                .collection("users")
                .document(userId)
                .collection("credentials")
                .document("coinbase")
                .get()
                .get();
            
            if (legacyDocument.exists()) {
                Map<String, String> credentials = new HashMap<>();
                credentials.put("apiKey", legacyDocument.getString("apiKey"));
                credentials.put("privateKey", legacyDocument.getString("privateKey"));
                
                log.info("Found Coinbase credentials in legacy credentials subcollection for user: {}", userId);
                log.info("API key found (first 5 chars): {}", 
                    credentials.get("apiKey") != null ? 
                    credentials.get("apiKey").substring(0, Math.min(credentials.get("apiKey").length(), 5)) + "..." : "null");
                log.info("Private key found (length): {}", 
                    credentials.get("privateKey") != null ? 
                    credentials.get("privateKey").length() + " chars" : "null");
                
                // Migrate to new structure
                migrateToNewStructure(userId, legacyDocument.getString("apiKey"), legacyDocument.getString("privateKey"));
                
                return credentials;
            } else {
                log.info("No Coinbase credentials found in legacy credentials subcollection");
            }
            
            // Check 3: Look in the user document preferences
            log.info("Checking user document preferences for Coinbase credentials");
            DocumentSnapshot userDoc = FirestoreClient.getFirestore()
                .collection("users")
                .document(userId)
                .get()
                .get();
            
            if (userDoc.exists()) {
                Map<String, Object> userData = userDoc.getData();
                
                if (userData != null && userData.containsKey("coinbaseConfig")) {
                    @SuppressWarnings("unchecked") // This cast is necessary as Firestore returns Object
                    Map<String, String> coinbaseConfig = (Map<String, String>) userData.get("coinbaseConfig");
                    
                    if (coinbaseConfig != null && coinbaseConfig.containsKey("apiKey") && coinbaseConfig.containsKey("privateKey")) {
                        Map<String, String> credentials = new HashMap<>();
                        credentials.put("apiKey", coinbaseConfig.get("apiKey"));
                        credentials.put("privateKey", coinbaseConfig.get("privateKey"));
                        
                        log.info("Found Coinbase credentials in user document preferences for user: {}", userId);
                        log.info("API key found (first 5 chars): {}", 
                            credentials.get("apiKey") != null ? 
                            credentials.get("apiKey").substring(0, Math.min(credentials.get("apiKey").length(), 5)) + "..." : "null");
                        log.info("Private key found (length): {}", 
                            credentials.get("privateKey") != null ? 
                            credentials.get("privateKey").length() + " chars" : "null");
                        
                        // Migrate to new structure
                        migrateToNewStructure(userId, coinbaseConfig.get("apiKey"), coinbaseConfig.get("privateKey"));
                        
                        return credentials;
                    }
                }
                
                log.info("No Coinbase credentials found in user document preferences");
            }
            
            log.warn("No Coinbase credentials found for user: {}", userId);
            return null;
        } catch (Exception e) {
            log.error("Error getting Coinbase credentials: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Save Coinbase credentials for a user
     * 
     * @param userId User ID
     * @param apiKey API key
     * @param privateKey Private key
     */
    public void saveCoinbaseCredentials(String userId, String apiKey, String privateKey) {
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("Firebase not initialized. Cannot save Coinbase credentials.");
            return;
        }
        
        try {
            log.info("Saving Coinbase credentials for user: {}", userId);
            
            // Save to the new api_credentials subcollection
            Map<String, Object> credentials = new HashMap<>();
            credentials.put("provider", "coinbase");
            credentials.put("apiKey", apiKey);
            credentials.put("privateKey", privateKey);
            credentials.put("createdAt", System.currentTimeMillis());
            credentials.put("updatedAt", System.currentTimeMillis());
            
            FirestoreClient.getFirestore()
                .collection("users")
                .document(userId)
                .collection("api_credentials")
                .document("coinbase")
                .set(credentials)
                .get();
            
            log.info("Saved Coinbase credentials to api_credentials subcollection for user: {}", userId);
        } catch (Exception e) {
            log.error("Error saving Coinbase credentials: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Migrate credentials to the new structure
     * 
     * @param userId User ID
     * @param apiKey API key
     * @param privateKey Private key
     */
    private void migrateToNewStructure(String userId, String apiKey, String privateKey) {
        try {
            log.info("Migrating Coinbase credentials to new structure for user: {}", userId);
            
            // Save to the new api_credentials subcollection
            saveCoinbaseCredentials(userId, apiKey, privateKey);
            
            log.info("Successfully migrated Coinbase credentials to new structure for user: {}", userId);
        } catch (Exception e) {
            log.error("Error migrating Coinbase credentials to new structure: {}", e.getMessage(), e);
        }
    }
}
