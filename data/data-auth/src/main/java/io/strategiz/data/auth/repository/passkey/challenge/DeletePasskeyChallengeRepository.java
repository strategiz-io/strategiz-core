package io.strategiz.data.auth.repository.passkey.challenge;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import io.strategiz.data.base.document.DocumentStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.concurrent.ExecutionException;

/**
 * Repository for deleting passkey challenges
 */
@Repository
public class DeletePasskeyChallengeRepository {

    private static final Logger log = LoggerFactory.getLogger(DeletePasskeyChallengeRepository.class);
    
    private static final String COLLECTION_PASSKEY_CHALLENGES = "passkey_challenges";
    private static final String FIELD_USER_ID = "userId";
    private static final String FIELD_EXPIRES_AT = "expiresAt";
    
    private final DocumentStorage documentStorage;
    
    public DeletePasskeyChallengeRepository(DocumentStorage documentStorage) {
        this.documentStorage = documentStorage;
    }

    /**
     * Delete a challenge by ID
     * 
     * @param challengeId Challenge ID
     * @return true if deleted, false if not found
     */
    public boolean deleteById(String challengeId) {
        try {
            Firestore firestore = documentStorage.getDocumentDb();
            
            firestore
                .collection(COLLECTION_PASSKEY_CHALLENGES)
                .document(challengeId)
                .delete()
                .get();
            
            log.debug("Deleted passkey challenge with ID: {}", challengeId);
            return true;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error deleting passkey challenge with ID {}: {}", challengeId, e.getMessage(), e);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Delete all challenges for a user
     * 
     * @param userId User ID
     * @return Number of challenges deleted
     */
    public int deleteAllByUserId(String userId) {
        try {
            Firestore firestore = documentStorage.getDocumentDb();
            
            QuerySnapshot querySnapshot = firestore
                .collection(COLLECTION_PASSKEY_CHALLENGES)
                .whereEqualTo(FIELD_USER_ID, userId)
                .get()
                .get();
            
            if (querySnapshot.isEmpty()) {
                return 0;
            }
            
            int deleteCount = 0;
            for (QueryDocumentSnapshot document : querySnapshot.getDocuments()) {
                document.getReference().delete();
                log.debug("Deleted passkey challenge: {}", document.getId());
                deleteCount++;
            }
            
            log.info("Deleted {} passkey challenges for user ID: {}", deleteCount, userId);
            return deleteCount;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error deleting passkey challenges for user {}: {}", userId, e.getMessage(), e);
            Thread.currentThread().interrupt();
            return 0;
        }
    }

    /**
     * Delete all expired challenges
     * 
     * @return Number of expired challenges deleted
     */
    public int deleteExpired() {
        try {
            Firestore firestore = documentStorage.getDocumentDb();
            
            QuerySnapshot querySnapshot = firestore
                .collection(COLLECTION_PASSKEY_CHALLENGES)
                .whereLessThan(FIELD_EXPIRES_AT, Instant.now())
                .get()
                .get();
            
            if (querySnapshot.isEmpty()) {
                return 0;
            }
            
            int deleteCount = 0;
            for (QueryDocumentSnapshot document : querySnapshot.getDocuments()) {
                document.getReference().delete();
                log.debug("Deleted expired passkey challenge: {}", document.getId());
                deleteCount++;
            }
            
            log.info("Deleted {} expired passkey challenges", deleteCount);
            return deleteCount;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error deleting expired passkey challenges: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
            return 0;
        }
    }
}
