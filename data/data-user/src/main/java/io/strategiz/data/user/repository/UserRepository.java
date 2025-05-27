package io.strategiz.data.user.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.SetOptions;
import com.google.cloud.firestore.WriteResult;
import io.strategiz.data.user.model.Credentials;
import io.strategiz.data.user.model.Provider;
import io.strategiz.data.user.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Repository for accessing and manipulating user data, provider settings, and API credentials
 * in Firestore.
 */
@Repository
public class UserRepository {

    private static final String USERS_COLLECTION = "users";
    private static final String PROVIDERS_COLLECTION = "providers";
    private static final String CREDENTIALS_COLLECTION = "credentials";
    private static final String DEFAULT_CREDENTIALS_ID = "default";
    
    private final Firestore firestore;

    @Autowired
    public UserRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    /**
     * Retrieves a user by ID.
     *
     * @param userId The user ID
     * @return An Optional containing the user if found
     */
    public Optional<User> getUserById(String userId) {
        try {
            DocumentReference docRef = firestore.collection(USERS_COLLECTION).document(userId);
            ApiFuture<DocumentSnapshot> future = docRef.get();
            DocumentSnapshot document = future.get();
            
            if (document.exists()) {
                User user = document.toObject(User.class);
                user.setId(document.getId());
                return Optional.of(user);
            } else {
                return Optional.empty();
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error retrieving user: " + e.getMessage(), e);
        }
    }

    /**
     * Updates the account mode for a user.
     *
     * @param userId The user ID
     * @param accountMode The account mode ("PAPER" or "LIVE")
     * @return true if successful
     */
    public boolean updateAccountMode(String userId, String accountMode) {
        try {
            DocumentReference docRef = firestore.collection(USERS_COLLECTION).document(userId);
            ApiFuture<WriteResult> future = docRef.update("accountMode", accountMode);
            future.get();
            return true;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error updating account mode: " + e.getMessage(), e);
        }
    }

    /**
     * Gets provider configuration for a specific user and provider.
     *
     * @param userId The user ID
     * @param providerId The provider ID
     * @return An Optional containing the provider if found
     */
    public Optional<Provider> getProvider(String userId, String providerId) {
        try {
            DocumentReference docRef = firestore.collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(PROVIDERS_COLLECTION)
                    .document(providerId);
                    
            ApiFuture<DocumentSnapshot> future = docRef.get();
            DocumentSnapshot document = future.get();
            
            if (document.exists()) {
                Provider provider = document.toObject(Provider.class);
                provider.setId(document.getId());
                return Optional.of(provider);
            } else {
                return Optional.empty();
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error retrieving provider: " + e.getMessage(), e);
        }
    }

    /**
     * Saves or updates provider configuration for a user.
     *
     * @param userId The user ID
     * @param providerId The provider ID
     * @param provider The provider configuration to save
     * @return true if successful
     */
    public boolean saveProvider(String userId, String providerId, Provider provider) {
        try {
            DocumentReference docRef = firestore.collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(PROVIDERS_COLLECTION)
                    .document(providerId);
                    
            provider.setId(providerId);
            ApiFuture<WriteResult> future = docRef.set(provider, SetOptions.merge());
            future.get();
            return true;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error saving provider: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes provider configuration for a user.
     *
     * @param userId The user ID
     * @param providerId The provider ID
     * @return true if successful
     */
    public boolean deleteProvider(String userId, String providerId) {
        try {
            // First delete any credentials documents under this provider
            deleteCredentials(userId, providerId);
            
            // Then delete the provider document itself
            DocumentReference docRef = firestore.collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(PROVIDERS_COLLECTION)
                    .document(providerId);
                    
            ApiFuture<WriteResult> future = docRef.delete();
            future.get();
            return true;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error deleting provider: " + e.getMessage(), e);
        }
    }
    
    /**
     * Gets API credentials for a specific user and provider.
     *
     * @param userId The user ID
     * @param providerId The provider ID
     * @return An Optional containing the credentials if found
     */
    public Optional<Credentials> getCredentials(String userId, String providerId) {
        try {
            DocumentReference docRef = firestore.collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(PROVIDERS_COLLECTION)
                    .document(providerId)
                    .collection(CREDENTIALS_COLLECTION)
                    .document(DEFAULT_CREDENTIALS_ID);
                    
            ApiFuture<DocumentSnapshot> future = docRef.get();
            DocumentSnapshot document = future.get();
            
            if (document.exists()) {
                Credentials credentials = document.toObject(Credentials.class);
                credentials.setId(document.getId());
                return Optional.of(credentials);
            } else {
                return Optional.empty();
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error retrieving credentials: " + e.getMessage(), e);
        }
    }
    
    /**
     * Saves or updates API credentials for a user's provider.
     *
     * @param userId The user ID
     * @param providerId The provider ID
     * @param credentials The credentials to save
     * @return true if successful
     */
    public boolean saveCredentials(String userId, String providerId, Credentials credentials) {
        try {
            DocumentReference docRef = firestore.collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(PROVIDERS_COLLECTION)
                    .document(providerId)
                    .collection(CREDENTIALS_COLLECTION)
                    .document(DEFAULT_CREDENTIALS_ID);
                    
            credentials.setId(DEFAULT_CREDENTIALS_ID);
            ApiFuture<WriteResult> future = docRef.set(credentials, SetOptions.merge());
            future.get();
            return true;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error saving credentials: " + e.getMessage(), e);
        }
    }
    
    /**
     * Deletes API credentials for a user's provider.
     *
     * @param userId The user ID
     * @param providerId The provider ID
     * @return true if successful
     */
    public boolean deleteCredentials(String userId, String providerId) {
        try {
            DocumentReference docRef = firestore.collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(PROVIDERS_COLLECTION)
                    .document(providerId)
                    .collection(CREDENTIALS_COLLECTION)
                    .document(DEFAULT_CREDENTIALS_ID);
                    
            ApiFuture<WriteResult> future = docRef.delete();
            future.get();
            return true;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error deleting credentials: " + e.getMessage(), e);
        }
    }
}
