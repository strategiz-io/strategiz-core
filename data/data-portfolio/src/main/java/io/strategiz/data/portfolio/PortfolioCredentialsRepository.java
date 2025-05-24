package io.strategiz.data.portfolio;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Repository for accessing portfolio and brokerage credentials
 */
@Repository
public class PortfolioCredentialsRepository {
    
    private static final Logger log = LoggerFactory.getLogger(PortfolioCredentialsRepository.class);
    
    /**
     * Get brokerage credentials for a user from the api_credentials subcollection
     * 
     * @param userId User ID (email)
     * @param provider Provider name (e.g., "robinhood", "coinbase")
     * @return Map with credentials, or null if not found
     */
    public Map<String, String> getBrokerageCredentials(String userId, String provider) {
        if (com.google.firebase.FirebaseApp.getApps().isEmpty()) {
            log.warn("Firebase not initialized. Cannot retrieve {} credentials.", provider);
            return null;
        }
        
        try {
            log.info("Getting {} credentials for user: {}", provider, userId);
            Firestore db = FirestoreClient.getFirestore();
            CollectionReference apiCredentialsRef = db.collection("users").document(userId).collection("api_credentials");
            Query query = apiCredentialsRef.whereEqualTo("provider", provider);
            ApiFuture<QuerySnapshot> querySnapshot = query.get();
            List<QueryDocumentSnapshot> documents = querySnapshot.get().getDocuments();
            
            if (!documents.isEmpty()) {
                DocumentSnapshot doc = documents.get(0);
                Map<String, Object> data = doc.getData();
                if (data != null) {
                    Map<String, String> credentials = new HashMap<>();
                    // Extract string values from the data map
                    for (Map.Entry<String, Object> entry : data.entrySet()) {
                        if (entry.getValue() instanceof String) {
                            credentials.put(entry.getKey(), (String) entry.getValue());
                        }
                    }
                    return credentials;
                }
            }
            
            log.warn("No {} credentials found for user: {}", provider, userId);
            return null;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error retrieving {} credentials for user: {}", provider, userId, e);
            Thread.currentThread().interrupt();
            return null;
        }
    }
}
