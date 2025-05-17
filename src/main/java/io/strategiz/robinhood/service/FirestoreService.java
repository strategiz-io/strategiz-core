package io.strategiz.robinhood.service;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.cloud.FirestoreClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service("robinhoodFirestoreService")
public class FirestoreService {
    /**
     * Get Robinhood credentials for a user from the api_credentials subcollection
     * @param userId User ID (email)
     * @return Map with username and password, or null if not found
     */
    public Map<String, String> getRobinhoodCredentials(String userId) {
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("Firebase not initialized. Cannot retrieve Robinhood credentials.");
            return null;
        }
        try {
            log.info("Getting Robinhood credentials for user: {}", userId);
            Firestore db = FirestoreClient.getFirestore();
            CollectionReference apiCredentialsRef = db.collection("users").document(userId).collection("api_credentials");
            Query query = apiCredentialsRef.whereEqualTo("provider", "robinhood");
            ApiFuture<QuerySnapshot> querySnapshot = query.get();
            List<QueryDocumentSnapshot> documents = querySnapshot.get().getDocuments();
            if (!documents.isEmpty()) {
                DocumentSnapshot doc = documents.get(0);
                Map<String, Object> data = doc.getData();
                if (data != null && data.containsKey("username") && data.containsKey("password")) {
                    Map<String, String> credentials = new HashMap<>();
                    credentials.put("username", (String) data.get("username"));
                    credentials.put("password", (String) data.get("password"));
                    return credentials;
                }
            }
            log.warn("No Robinhood credentials found for user: {}", userId);
            return null;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error retrieving Robinhood credentials for user: {}", userId, e);
            return null;
        }
    }
}
