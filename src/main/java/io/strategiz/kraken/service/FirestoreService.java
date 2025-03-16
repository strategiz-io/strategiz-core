package io.strategiz.kraken.service;

import com.google.firebase.cloud.FirestoreClient;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.FirebaseApp;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for interacting with Firestore database for Kraken credentials
 */
@Service("krakenFirestoreService")
@Slf4j
public class FirestoreService {

    /**
     * Get Kraken credentials for a user
     * 
     * @param userId User ID
     * @return Map of credentials (apiKey, secretKey)
     */
    public Map<String, String> getKrakenCredentials(String userId) {
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("Firebase not initialized. Cannot retrieve Kraken credentials.");
            return null;
        }
        
        try {
            log.info("Getting Kraken credentials for user: {}", userId);
            
            // Check 1: Look in the new api_credentials subcollection (new structure)
            log.info("Checking api_credentials subcollection for Kraken credentials");
            QuerySnapshot querySnapshot = FirestoreClient.getFirestore()
                .collection("users")
                .document(userId)
                .collection("api_credentials")
                .whereEqualTo("provider", "kraken")
                .get()
                .get();
            
            if (!querySnapshot.isEmpty()) {
                DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                
                if (document.exists()) {
                    Map<String, String> credentials = new HashMap<>();
                    credentials.put("apiKey", document.getString("apiKey"));
                    credentials.put("secretKey", document.getString("secretKey"));
                    
                    log.info("Found Kraken credentials in api_credentials subcollection for user: {}", userId);
                    log.info("API key found (first 5 chars): {}", 
                        credentials.get("apiKey") != null ? 
                        credentials.get("apiKey").substring(0, Math.min(credentials.get("apiKey").length(), 5)) + "..." : "null");
                    log.info("Secret key found (length): {}", 
                        credentials.get("secretKey") != null ? 
                        credentials.get("secretKey").length() + " chars" : "null");
                    
                    return credentials;
                }
            } else {
                log.info("No Kraken credentials found in api_credentials subcollection");
            }
            
            // Check 2: Look in the legacy credentials subcollection
            log.info("Checking legacy credentials subcollection for Kraken credentials");
            DocumentSnapshot legacyDocument = FirestoreClient.getFirestore()
                .collection("users")
                .document(userId)
                .collection("credentials")
                .document("kraken")
                .get()
                .get();
            
            if (legacyDocument.exists()) {
                Map<String, String> credentials = new HashMap<>();
                credentials.put("apiKey", legacyDocument.getString("apiKey"));
                credentials.put("secretKey", legacyDocument.getString("secretKey"));
                
                log.info("Found Kraken credentials in legacy credentials subcollection for user: {}", userId);
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
                log.info("No Kraken credentials found in legacy credentials subcollection");
            }
            
            // Check 3: Look in the user document preferences
            log.info("Checking user document preferences for Kraken credentials");
            DocumentSnapshot userDoc = FirestoreClient.getFirestore()
                .collection("users")
                .document(userId)
                .get()
                .get();
            
            if (userDoc.exists() && userDoc.get("preferences.apiKeys.kraken") != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> preferences = (Map<String, Object>) userDoc.get("preferences");
                
                if (preferences != null && preferences.containsKey("apiKeys")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> apiKeys = (Map<String, Object>) preferences.get("apiKeys");
                    
                    if (apiKeys != null && apiKeys.containsKey("kraken")) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> krakenKeys = (Map<String, Object>) apiKeys.get("kraken");
                        
                        if (krakenKeys != null) {
                            Map<String, String> credentials = new HashMap<>();
                            credentials.put("apiKey", (String) krakenKeys.get("apiKey"));
                            credentials.put("secretKey", (String) krakenKeys.get("privateKey"));
                            
                            log.info("Found Kraken credentials in user preferences for user: {}", userId);
                            log.info("API key found (first 5 chars): {}", 
                                credentials.get("apiKey") != null ? 
                                credentials.get("apiKey").substring(0, Math.min(credentials.get("apiKey").length(), 5)) + "..." : "null");
                            log.info("Secret key found (length): {}", 
                                credentials.get("secretKey") != null ? 
                                credentials.get("secretKey").length() + " chars" : "null");
                            
                            // Migrate to new structure
                            migrateToNewStructure(userId, (String) krakenKeys.get("apiKey"), (String) krakenKeys.get("privateKey"));
                            
                            return credentials;
                        }
                    }
                }
            } else {
                log.info("No Kraken credentials found in user document preferences");
            }
            
            // Check 4: Look in the kraken_config collection
            log.info("Checking kraken_config collection for Kraken credentials");
            DocumentSnapshot configDoc = FirestoreClient.getFirestore()
                .collection("kraken_config")
                .document(userId)
                .get()
                .get();
            
            if (configDoc.exists()) {
                Map<String, String> credentials = new HashMap<>();
                credentials.put("apiKey", configDoc.getString("apiKey"));
                credentials.put("secretKey", configDoc.getString("secretKey"));
                
                log.info("Found Kraken credentials in kraken_config collection for user: {}", userId);
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
                log.info("No Kraken credentials found in kraken_config collection");
            }
            
            log.warn("No Kraken credentials found for user: {}", userId);
            return null;
        } catch (Exception e) {
            log.error("Error retrieving Kraken credentials: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Save Kraken credentials for a user
     * 
     * @param userId User ID
     * @param apiKey API key
     * @param secretKey Secret key
     */
    public void saveKrakenCredentials(String userId, String apiKey, String secretKey) {
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("Firebase not initialized. Cannot save Kraken credentials.");
            return;
        }
        
        try {
            // Save to the new api_credentials subcollection
            Map<String, Object> credentials = new HashMap<>();
            credentials.put("provider", "kraken");
            credentials.put("apiKey", apiKey);
            credentials.put("secretKey", secretKey);
            credentials.put("updatedAt", System.currentTimeMillis());
            
            // First check if a document already exists
            QuerySnapshot querySnapshot = FirestoreClient.getFirestore()
                .collection("users")
                .document(userId)
                .collection("api_credentials")
                .whereEqualTo("provider", "kraken")
                .get()
                .get();
            
            if (!querySnapshot.isEmpty()) {
                // Update existing document
                DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                FirestoreClient.getFirestore()
                    .collection("users")
                    .document(userId)
                    .collection("api_credentials")
                    .document(document.getId())
                    .set(credentials);
                
                log.info("Updated existing Kraken credentials in api_credentials subcollection for user: {}", userId);
            } else {
                // Create new document
                FirestoreClient.getFirestore()
                    .collection("users")
                    .document(userId)
                    .collection("api_credentials")
                    .add(credentials);
                
                log.info("Created new Kraken credentials in api_credentials subcollection for user: {}", userId);
            }
        } catch (Exception e) {
            log.error("Error saving Kraken credentials: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Migrate Kraken credentials to the new structure
     * 
     * @param userId User ID
     * @param apiKey API key
     * @param secretKey Secret key
     */
    private void migrateToNewStructure(String userId, String apiKey, String secretKey) {
        try {
            // Save to the new api_credentials subcollection
            Map<String, Object> credentials = new HashMap<>();
            credentials.put("provider", "kraken");
            credentials.put("apiKey", apiKey);
            credentials.put("secretKey", secretKey);
            credentials.put("updatedAt", System.currentTimeMillis());
            credentials.put("migratedAt", System.currentTimeMillis());
            
            // First check if a document already exists
            QuerySnapshot querySnapshot = FirestoreClient.getFirestore()
                .collection("users")
                .document(userId)
                .collection("api_credentials")
                .whereEqualTo("provider", "kraken")
                .get()
                .get();
            
            if (querySnapshot.isEmpty()) {
                // Create new document only if it doesn't exist
                FirestoreClient.getFirestore()
                    .collection("users")
                    .document(userId)
                    .collection("api_credentials")
                    .add(credentials);
                
                log.info("Migrated Kraken credentials to new structure for user: {}", userId);
            }
        } catch (Exception e) {
            log.error("Error migrating Kraken credentials to new structure: {}", e.getMessage(), e);
        }
    }
}
