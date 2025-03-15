package io.strategiz.kraken.service;

import com.google.firebase.cloud.FirestoreClient;
import com.google.cloud.firestore.DocumentSnapshot;
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
            DocumentSnapshot document = FirestoreClient.getFirestore()
                .collection("users")
                .document(userId)
                .collection("credentials")
                .document("kraken")
                .get()
                .get();
            
            if (!document.exists()) {
                return null;
            }
            
            Map<String, String> credentials = new HashMap<>();
            credentials.put("apiKey", document.getString("apiKey"));
            credentials.put("secretKey", document.getString("secretKey"));
            
            return credentials;
        } catch (Exception e) {
            log.error("Error retrieving Kraken credentials: {}", e.getMessage());
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
            Map<String, Object> credentials = new HashMap<>();
            credentials.put("apiKey", apiKey);
            credentials.put("secretKey", secretKey);
            credentials.put("updatedAt", System.currentTimeMillis());
            
            FirestoreClient.getFirestore()
                .collection("users")
                .document(userId)
                .collection("credentials")
                .document("kraken")
                .set(credentials);
        } catch (Exception e) {
            log.error("Error saving Kraken credentials: {}", e.getMessage());
        }
    }
}
