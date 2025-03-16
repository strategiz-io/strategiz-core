package io.strategiz.auth.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import io.strategiz.auth.model.PasskeyCredential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Firestore implementation of the PasskeyCredentialRepository
 */
@Repository
public class FirestorePasskeyCredentialRepository implements PasskeyCredentialRepository {
    private static final Logger logger = LoggerFactory.getLogger(FirestorePasskeyCredentialRepository.class);
    private static final String COLLECTION_NAME = "passkey_credentials";

    private final Firestore firestore;

    public FirestorePasskeyCredentialRepository() {
        this.firestore = FirestoreClient.getFirestore();
    }

    @Override
    public CompletableFuture<Void> save(PasskeyCredential credential) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        try {
            DocumentReference docRef;
            if (credential.getId() != null && !credential.getId().isEmpty()) {
                docRef = firestore.collection(COLLECTION_NAME).document(credential.getId());
            } else {
                docRef = firestore.collection(COLLECTION_NAME).document();
                credential.setId(docRef.getId());
            }
            
            ApiFuture<WriteResult> result = docRef.set(credential);
            
            result.addListener(() -> {
                try {
                    result.get();
                    logger.info("Saved passkey credential: {}", credential.getId());
                    future.complete(null);
                } catch (Exception e) {
                    logger.error("Failed to save passkey credential: {}", credential.getId(), e);
                    future.completeExceptionally(e);
                }
            }, Runnable::run);
        } catch (Exception e) {
            logger.error("Error saving passkey credential", e);
            future.completeExceptionally(e);
        }
        
        return future;
    }

    @Override
    public CompletableFuture<Optional<PasskeyCredential>> findById(String id) {
        CompletableFuture<Optional<PasskeyCredential>> future = new CompletableFuture<>();
        
        try {
            DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(id);
            ApiFuture<DocumentSnapshot> documentSnapshot = docRef.get();
            
            documentSnapshot.addListener(() -> {
                try {
                    DocumentSnapshot snapshot = documentSnapshot.get();
                    if (snapshot.exists()) {
                        PasskeyCredential credential = snapshot.toObject(PasskeyCredential.class);
                        logger.info("Found passkey credential: {}", id);
                        future.complete(Optional.ofNullable(credential));
                    } else {
                        logger.info("Passkey credential not found: {}", id);
                        future.complete(Optional.empty());
                    }
                } catch (Exception e) {
                    logger.error("Failed to get passkey credential: {}", id, e);
                    future.completeExceptionally(e);
                }
            }, Runnable::run);
        } catch (Exception e) {
            logger.error("Error finding passkey credential by ID", e);
            future.completeExceptionally(e);
        }
        
        return future;
    }

    @Override
    public CompletableFuture<List<PasskeyCredential>> findByUserId(String userId) {
        CompletableFuture<List<PasskeyCredential>> future = new CompletableFuture<>();
        
        try {
            Query query = firestore.collection(COLLECTION_NAME).whereEqualTo("userId", userId);
            ApiFuture<QuerySnapshot> querySnapshot = query.get();
            
            querySnapshot.addListener(() -> {
                try {
                    List<PasskeyCredential> credentials = new ArrayList<>();
                    QuerySnapshot snapshot = querySnapshot.get();
                    
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        PasskeyCredential credential = document.toObject(PasskeyCredential.class);
                        if (credential != null) {
                            credentials.add(credential);
                        }
                    }
                    
                    logger.info("Found {} passkey credentials for user: {}", credentials.size(), userId);
                    future.complete(credentials);
                } catch (Exception e) {
                    logger.error("Failed to get passkey credentials for user: {}", userId, e);
                    future.completeExceptionally(e);
                }
            }, Runnable::run);
        } catch (Exception e) {
            logger.error("Error finding passkey credentials by user ID", e);
            future.completeExceptionally(e);
        }
        
        return future;
    }

    @Override
    public CompletableFuture<Void> deleteById(String id) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        try {
            DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(id);
            ApiFuture<WriteResult> result = docRef.delete();
            
            result.addListener(() -> {
                try {
                    result.get();
                    logger.info("Deleted passkey credential: {}", id);
                    future.complete(null);
                } catch (Exception e) {
                    logger.error("Failed to delete passkey credential: {}", id, e);
                    future.completeExceptionally(e);
                }
            }, Runnable::run);
        } catch (Exception e) {
            logger.error("Error deleting passkey credential", e);
            future.completeExceptionally(e);
        }
        
        return future;
    }
}
