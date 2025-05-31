package io.strategiz.data.auth;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import io.strategiz.data.base.document.DocumentStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Firestore implementation of the PasskeyCredentialRepository interface
 */
@Repository
public class FirestorePasskeyCredentialRepository implements PasskeyCredentialRepository {

    private static final Logger log = LoggerFactory.getLogger(FirestorePasskeyCredentialRepository.class);
    private static final String COLLECTION_PATH = "passkey_credentials";

    private final DocumentStorageService documentStorage;

    @Autowired
    public FirestorePasskeyCredentialRepository(DocumentStorageService documentStorage) {
        this.documentStorage = documentStorage;
    }

    @Override
    public Optional<PasskeyCredential> findById(String id) {
        try {
            Firestore firestore = documentStorage.getDocumentDb();
            DocumentSnapshot document = firestore.collection(COLLECTION_PATH)
                .document(id)
                .get()
                .get();
            
            if (!document.exists()) {
                return Optional.empty();
            }
            
            PasskeyCredential credential = document.toObject(PasskeyCredential.class);
            if (credential == null) {
                return Optional.empty();
            }
            
            credential.setId(document.getId());
            return Optional.of(credential);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error finding passkey credential by id: {}", id, e);
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    @Override
    public Optional<PasskeyCredential> findByCredentialId(String credentialId) {
        try {
            Firestore firestore = documentStorage.getDocumentDb();
            QuerySnapshot querySnapshot = firestore.collection(COLLECTION_PATH)
                .whereEqualTo("credentialId", credentialId)
                .get()
                .get();
            
            if (querySnapshot.isEmpty()) {
                return Optional.empty();
            }
            
            DocumentSnapshot document = querySnapshot.getDocuments().get(0);
            PasskeyCredential credential = document.toObject(PasskeyCredential.class);
            if (credential == null) {
                return Optional.empty();
            }
            
            credential.setId(document.getId());
            return Optional.of(credential);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error finding passkey credential by credential ID: {}", credentialId, e);
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    @Override
    public Optional<PasskeyCredential> findByCredentialIdAndUserId(String credentialId, String userId) {
        try {
            Firestore firestore = documentStorage.getDocumentDb();
            QuerySnapshot querySnapshot = firestore.collection(COLLECTION_PATH)
                .whereEqualTo("credentialId", credentialId)
                .whereEqualTo("userId", userId)
                .get()
                .get();
            
            if (querySnapshot.isEmpty()) {
                return Optional.empty();
            }
            
            DocumentSnapshot document = querySnapshot.getDocuments().get(0);
            PasskeyCredential credential = document.toObject(PasskeyCredential.class);
            if (credential == null) {
                return Optional.empty();
            }
            
            credential.setId(document.getId());
            return Optional.of(credential);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error finding passkey credential by credential ID and user ID: {}, {}", credentialId, userId, e);
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    @Override
    public List<PasskeyCredential> findAllByUserId(String userId) {
        try {
            List<PasskeyCredential> credentials = new ArrayList<>();
            Firestore firestore = documentStorage.getDocumentDb();
            
            QuerySnapshot querySnapshot = firestore.collection(COLLECTION_PATH)
                .whereEqualTo("userId", userId)
                .get()
                .get();
            
            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                PasskeyCredential credential = document.toObject(PasskeyCredential.class);
                if (credential != null) {
                    credential.setId(document.getId());
                    credentials.add(credential);
                }
            }
            
            return credentials;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error finding passkey credentials for user: {}", userId, e);
            Thread.currentThread().interrupt();
            return List.of();
        }
    }

    @Override
    public PasskeyCredential save(PasskeyCredential passkeyCredential) {
        try {
            Firestore firestore = documentStorage.getDocumentDb();
            DocumentReference docRef;
            
            if (passkeyCredential.getId() != null && !passkeyCredential.getId().isEmpty()) {
                // Update existing credential
                docRef = firestore.collection(COLLECTION_PATH)
                    .document(passkeyCredential.getId());
            } else {
                // Create new credential
                docRef = firestore.collection(COLLECTION_PATH)
                    .document();
                passkeyCredential.setId(docRef.getId());
            }
            
            // Save to Firestore
            ApiFuture<WriteResult> result = docRef.set(passkeyCredential);
            result.get(); // Wait for write to complete
            
            return passkeyCredential;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error saving passkey credential: {}", passkeyCredential.getId(), e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to save passkey credential", e);
        }
    }

    @Override
    public boolean deleteById(String id) {
        try {
            Firestore firestore = documentStorage.getDocumentDb();
            ApiFuture<WriteResult> result = firestore.collection(COLLECTION_PATH)
                .document(id)
                .delete();
            
            result.get(); // Wait for delete to complete
            return true;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error deleting passkey credential: {}", id, e);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public int deleteAllByUserId(String userId) {
        try {
            Firestore firestore = documentStorage.getDocumentDb();
            
            // Get all credentials for the user
            QuerySnapshot querySnapshot = firestore.collection(COLLECTION_PATH)
                .whereEqualTo("userId", userId)
                .get()
                .get();
            
            int count = querySnapshot.size();
            
            // Delete each credential
            WriteBatch batch = firestore.batch();
            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                batch.delete(document.getReference());
            }
            
            // Commit the batch
            batch.commit().get();
            
            return count;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error deleting passkey credentials for user: {}", userId, e);
            Thread.currentThread().interrupt();
            return 0;
        }
    }
}
