package io.strategiz.data.auth.repository.passkey.challenge;

import com.google.cloud.firestore.Firestore;
import io.strategiz.data.auth.model.passkey.PasskeyChallenge;
import io.strategiz.data.base.document.DocumentStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.concurrent.ExecutionException;

/**
 * Repository for updating passkey challenges
 */
@Repository
public class UpdatePasskeyChallengeRepository {

    private static final Logger log = LoggerFactory.getLogger(UpdatePasskeyChallengeRepository.class);
    
    private static final String COLLECTION_PASSKEY_CHALLENGES = "passkey_challenges";
    
    private final DocumentStorage documentStorage;
    
    public UpdatePasskeyChallengeRepository(DocumentStorage documentStorage) {
        this.documentStorage = documentStorage;
    }
    
    /**
     * Update an existing passkey challenge
     * 
     * @param challenge Challenge to update
     * @return Updated challenge
     * @throws IllegalArgumentException if challenge ID is null or empty
     */
    public PasskeyChallenge update(PasskeyChallenge challenge) {
        if (challenge.getId() == null || challenge.getId().isEmpty()) {
            throw new IllegalArgumentException("Cannot update challenge without an ID");
        }
        
        try {
            Firestore firestore = documentStorage.getDocumentDb();
            
            // Update existing challenge
            firestore
                .collection(COLLECTION_PASSKEY_CHALLENGES)
                .document(challenge.getId())
                .set(challenge)
                .get();
            
            log.debug("Updated passkey challenge with ID: {}", challenge.getId());
            return challenge;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error updating passkey challenge: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to update passkey challenge", e);
        }
    }
}
