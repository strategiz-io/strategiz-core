package io.strategiz.binanceus.service;

import com.google.firebase.cloud.FirestoreClient;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.firebase.FirebaseApp;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for interacting with Firestore database
 */
@Service("binanceUsFirestoreService")
@Slf4j
public class FirestoreService {

    /**
     * Get exchange credentials for a user
     * 
     * @param userId User ID
     * @param exchange Exchange name
     * @return Map of credentials (apiKey, secretKey)
     */
    public Map<String, String> getExchangeCredentials(String userId, String exchange) {
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("Firebase not initialized. Cannot retrieve {} credentials.", exchange);
            return null;
        }
        
        try {
            DocumentSnapshot document = FirestoreClient.getFirestore()
                .collection("users")
                .document(userId)
                .collection("credentials")
                .document(exchange)
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
            log.error("Error retrieving {} credentials: {}", exchange, e.getMessage());
            return null;
        }
    }
    
    /**
     * Save exchange credentials for a user
     * 
     * @param userId User ID
     * @param exchange Exchange name
     * @param apiKey API key
     * @param secretKey Secret key
     */
    public void saveExchangeCredentials(String userId, String exchange, String apiKey, String secretKey) {
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("Firebase not initialized. Cannot save {} credentials.", exchange);
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
                .document(exchange)
                .set(credentials);
        } catch (Exception e) {
            log.error("Error saving {} credentials: {}", exchange, e.getMessage());
        }
    }
}
