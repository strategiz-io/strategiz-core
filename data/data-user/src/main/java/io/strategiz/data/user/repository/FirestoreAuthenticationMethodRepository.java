package io.strategiz.data.user.repository;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import io.strategiz.data.user.model.OAuthAuthenticationMethod;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Firestore implementation of the AuthenticationMethodRepository interface
 */
@Repository
public class FirestoreAuthenticationMethodRepository implements AuthenticationMethodRepository {

    private static final String AUTH_METHODS_COLLECTION = "authentication_methods";
    
    private final Firestore firestore;
    
    public FirestoreAuthenticationMethodRepository(Firestore firestore) {
        this.firestore = firestore;
    }
    
    /**
     * Find OAuth authentication methods by provider and user ID
     * 
     * @param provider OAuth provider (e.g., "google", "facebook")
     * @param uid Provider-specific user ID
     * @return List of matching OAuth authentication methods
     */
    @Override
    public List<OAuthAuthenticationMethod> findByProviderAndUid(String provider, String uid) {
        List<OAuthAuthenticationMethod> result = new ArrayList<>();
        
        try {
            // Query authentication methods collection for matching provider and UID
            QuerySnapshot querySnapshot = firestore.collection(AUTH_METHODS_COLLECTION)
                    .whereEqualTo("provider", provider)
                    .whereEqualTo("uid", uid)
                    .get()
                    .get();
            
            for (QueryDocumentSnapshot document : querySnapshot.getDocuments()) {
                OAuthAuthenticationMethod method = document.toObject(OAuthAuthenticationMethod.class);
                // Ensure the ID is set from the document ID
                method.setId(document.getId());
                result.add(method);
            }
        } catch (InterruptedException | ExecutionException e) {
            // Log error and return empty list
            System.err.println("Error finding OAuth methods: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Save an OAuth authentication method
     * 
     * @param oAuthMethod The authentication method to save
     * @return The saved authentication method
     */
    @Override
    public OAuthAuthenticationMethod save(OAuthAuthenticationMethod oAuthMethod) {
        try {
            DocumentReference docRef;
            
            if (oAuthMethod.getId() != null && !oAuthMethod.getId().isEmpty()) {
                // Update existing authentication method
                docRef = firestore.collection(AUTH_METHODS_COLLECTION).document(oAuthMethod.getId());
            } else {
                // Create new authentication method with auto-generated ID
                docRef = firestore.collection(AUTH_METHODS_COLLECTION).document();
                oAuthMethod.setId(docRef.getId());
            }
            
            // Save the OAuth method to Firestore
            docRef.set(oAuthMethod).get();
            
            // Retrieve the saved document to return
            DocumentSnapshot savedDoc = docRef.get().get();
            return savedDoc.toObject(OAuthAuthenticationMethod.class);
            
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error saving OAuth method: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Find OAuth authentication methods by user ID, provider, and provider ID
     * 
     * @param userId The user ID in the system
     * @param provider OAuth provider (e.g., "google", "facebook")
     * @param providerId Provider-specific user ID
     * @return List of matching OAuth authentication methods
     */
    @Override
    public List<OAuthAuthenticationMethod> findByUserIdAndProviderAndProviderId(String userId, String provider, String providerId) {
        List<OAuthAuthenticationMethod> result = new ArrayList<>();
        
        try {
            // Query authentication methods collection for matching user ID, provider and providerId (uid)
            QuerySnapshot querySnapshot = firestore.collection(AUTH_METHODS_COLLECTION)
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("provider", provider)
                    .whereEqualTo("uid", providerId)
                    .get()
                    .get();
            
            for (QueryDocumentSnapshot document : querySnapshot.getDocuments()) {
                OAuthAuthenticationMethod method = document.toObject(OAuthAuthenticationMethod.class);
                // Ensure the ID is set from the document ID
                method.setId(document.getId());
                result.add(method);
            }
        } catch (InterruptedException | ExecutionException e) {
            // Log error and return empty list
            System.err.println("Error finding OAuth methods by user ID and provider: " + e.getMessage());
        }
        
        return result;
    }
}
