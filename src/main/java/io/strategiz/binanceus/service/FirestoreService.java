package io.strategiz.binanceus.service;

import com.google.firebase.cloud.FirestoreClient;
import com.google.cloud.firestore.DocumentSnapshot;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for interacting with Firestore database
 */
@Service
public class FirestoreService {

    /**
     * Get exchange credentials for a user
     * 
     * @param userId User ID
     * @param exchange Exchange name
     * @return Map of credentials (apiKey, secretKey)
     */
    public Map<String, String> getExchangeCredentials(String userId, String exchange) {
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
    }
}
