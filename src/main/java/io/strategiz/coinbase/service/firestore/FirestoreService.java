package io.strategiz.coinbase.service.firestore;

import com.google.firebase.cloud.FirestoreClient;
import com.google.cloud.firestore.DocumentSnapshot;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for interacting with Firestore database for Coinbase credentials
 */
@Service("coinbaseFirestoreService")
public class FirestoreService {

    /**
     * Get Coinbase credentials for a user
     * 
     * @param userId User ID
     * @return Map of credentials (apiKey, secretKey)
     */
    public Map<String, String> getCoinbaseCredentials(String userId) {
        try {
            DocumentSnapshot document = FirestoreClient.getFirestore()
                .collection("users")
                .document(userId)
                .collection("credentials")
                .document("coinbase")
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
     * Save Coinbase credentials for a user
     * 
     * @param userId User ID
     * @param apiKey API key
     * @param secretKey Secret key
     */
    public void saveCoinbaseCredentials(String userId, String apiKey, String secretKey) {
        Map<String, Object> credentials = new HashMap<>();
        credentials.put("apiKey", apiKey);
        credentials.put("secretKey", secretKey);
        credentials.put("updatedAt", System.currentTimeMillis());
        
        FirestoreClient.getFirestore()
            .collection("users")
            .document(userId)
            .collection("credentials")
            .document("coinbase")
            .set(credentials);
    }
}
