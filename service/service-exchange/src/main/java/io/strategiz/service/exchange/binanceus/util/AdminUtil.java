package io.strategiz.service.exchange.binanceus.util;

import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.cloud.FirestoreClient;
import com.google.cloud.firestore.DocumentSnapshot;

/**
 * Utility class for admin-related functionality
 */
public class AdminUtil {

    /**
     * Check if a user has admin privileges
     * 
     * @param token Firebase token of the user
     * @return true if the user is an admin, false otherwise
     */
    public static boolean isAdmin(FirebaseToken token) {
        try {
            String userId = token.getUid();
            
            DocumentSnapshot userDoc = FirestoreClient.getFirestore()
                .collection("users")
                .document(userId)
                .get()
                .get();
            
            return userDoc.exists() && Boolean.TRUE.equals(userDoc.getBoolean("isAdmin"));
        } catch (Exception e) {
            return false;
        }
    }
}
