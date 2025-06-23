package io.strategiz.data.auth.repository.passkey.challenge;

import com.google.cloud.firestore.Firestore;
import io.strategiz.data.auth.model.passkey.PasskeyChallenge;
import io.strategiz.data.base.document.DocumentStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.concurrent.ExecutionException;

/**
 * Repository for creating passkey challenges
 */
@Repository
public class CreatePasskeyChallengeRepository {

    private static final Logger log = LoggerFactory.getLogger(CreatePasskeyChallengeRepository.class);
    
    private static final String COLLECTION_PASSKEY_CHALLENGES = "passkey_challenges";
    
    private final DocumentStorage documentStorage;
    
    public CreatePasskeyChallengeRepository(DocumentStorage documentStorage) {
        this.documentStorage = documentStorage;
    }
    
    /**
     * Create a new passkey challenge
     * 
     * @param challenge Passkey challenge to save
     * @return Saved passkey challenge with generated ID
     */
    public PasskeyChallenge create(PasskeyChallenge challenge) {
        try {
            Firestore firestore = documentStorage.getDocumentDb();
            
            // New challenge
            var docRef = firestore
                .collection(COLLECTION_PASSKEY_CHALLENGES)
                .document();
            challenge.setId(docRef.getId());
            docRef.set(challenge).get();
            
            log.debug("Created new passkey challenge with ID: {}", challenge.getId());
            return challenge;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error creating passkey challenge: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to create passkey challenge", e);
        }
    }
}
