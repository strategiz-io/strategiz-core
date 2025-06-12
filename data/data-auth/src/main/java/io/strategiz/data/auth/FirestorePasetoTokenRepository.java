package io.strategiz.data.auth;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import io.strategiz.data.base.FirestoreOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Firestore implementation of PasetoTokenRepository
 */
@Repository
public class FirestorePasetoTokenRepository implements PasetoTokenRepository {
    private static final Logger log = LoggerFactory.getLogger(FirestorePasetoTokenRepository.class);
    private static final String COLLECTION_NAME = "paseto_tokens";
    
    private final FirestoreOperations firestoreOperations;
    private final CollectionReference tokensCollection;
    
    @Autowired
    public FirestorePasetoTokenRepository(Firestore firestore, FirestoreOperations firestoreOperations) {
        this.firestoreOperations = firestoreOperations;
        this.tokensCollection = firestore.collection(COLLECTION_NAME);
    }
    
    @Override
    public PasetoToken save(PasetoToken token) {
        if (token.getId() == null || token.getId().isEmpty()) {
            token.setId(UUID.randomUUID().toString());
        }
        
        firestoreOperations.set(tokensCollection.document(token.getId()), token);
        log.debug("Saved PASETO token: {}", token.getId());
        return token;
    }
    
    @Override
    public Optional<PasetoToken> findById(String id) {
        try {
            DocumentSnapshot document = tokensCollection.document(id).get().get();
            
            if (document.exists()) {
                PasetoToken token = document.toObject(PasetoToken.class);
                return Optional.ofNullable(token);
            }
            
            return Optional.empty();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error finding PASETO token by ID: {}", id, e);
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }
    
    @Override
    public Optional<PasetoToken> findByTokenValue(String tokenValue) {
        try {
            QuerySnapshot querySnapshot = tokensCollection
                    .whereEqualTo("tokenValue", tokenValue)
                    .limit(1)
                    .get()
                    .get();
            
            if (!querySnapshot.isEmpty()) {
                PasetoToken token = querySnapshot.getDocuments().get(0).toObject(PasetoToken.class);
                return Optional.ofNullable(token);
            }
            
            return Optional.empty();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error finding PASETO token by token value", e);
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }
    
    @Override
    public List<PasetoToken> findAllByUserId(String userId) {
        try {
            QuerySnapshot querySnapshot = tokensCollection
                    .whereEqualTo("userId", userId)
                    .get()
                    .get();
            
            List<PasetoToken> tokens = new ArrayList<>();
            for (QueryDocumentSnapshot document : querySnapshot.getDocuments()) {
                tokens.add(document.toObject(PasetoToken.class));
            }
            
            return tokens;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error finding PASETO tokens by user ID: {}", userId, e);
            Thread.currentThread().interrupt();
            return List.of();
        }
    }
    
    @Override
    public List<PasetoToken> findActiveTokensByUserId(String userId) {
        try {
            long now = Instant.now().getEpochSecond();
            
            QuerySnapshot querySnapshot = tokensCollection
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("revoked", false)
                    .whereGreaterThan("expiresAt", now)
                    .get()
                    .get();
            
            List<PasetoToken> tokens = new ArrayList<>();
            for (QueryDocumentSnapshot document : querySnapshot.getDocuments()) {
                tokens.add(document.toObject(PasetoToken.class));
            }
            
            return tokens;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error finding active PASETO tokens by user ID: {}", userId, e);
            Thread.currentThread().interrupt();
            return List.of();
        }
    }
    
    @Override
    public boolean deleteById(String id) {
        try {
            firestoreOperations.delete(tokensCollection.document(id));
            log.debug("Deleted PASETO token: {}", id);
            return true;
        } catch (Exception e) {
            log.error("Error deleting PASETO token: {}", id, e);
            return false;
        }
    }
    
    @Override
    public int deleteAllByUserId(String userId) {
        try {
            QuerySnapshot querySnapshot = tokensCollection
                    .whereEqualTo("userId", userId)
                    .get()
                    .get();
            
            int count = 0;
            for (QueryDocumentSnapshot document : querySnapshot.getDocuments()) {
                firestoreOperations.delete(document.getReference());
                count++;
            }
            
            log.debug("Deleted {} PASETO tokens for user: {}", count, userId);
            return count;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error deleting PASETO tokens for user: {}", userId, e);
            Thread.currentThread().interrupt();
            return 0;
        }
    }
    
    @Override
    public int deleteExpiredTokens(long currentTime) {
        try {
            // Get all expired tokens
            QuerySnapshot querySnapshot = tokensCollection
                    .whereLessThan("expiresAt", currentTime)
                    .get()
                    .get();
            
            int count = 0;
            for (QueryDocumentSnapshot document : querySnapshot.getDocuments()) {
                firestoreOperations.delete(document.getReference());
                count++;
            }
            
            log.debug("Deleted {} expired PASETO tokens", count);
            return count;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error deleting expired PASETO tokens", e);
            Thread.currentThread().interrupt();
            return 0;
        }
    }
}
