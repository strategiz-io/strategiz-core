package io.strategiz.data.auth.repository.passkey.challenge;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import io.strategiz.data.auth.model.passkey.PasskeyChallenge;
import io.strategiz.data.base.document.DocumentStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Repository for reading passkey challenges
 */
@Repository
public class ReadPasskeyChallengeRepository {

    private static final Logger log = LoggerFactory.getLogger(ReadPasskeyChallengeRepository.class);
    
    private static final String COLLECTION_PASSKEY_CHALLENGES = "passkey_challenges";
    private static final String FIELD_USER_ID = "userId";
    private static final String FIELD_CHALLENGE = "challenge";
    
    private final DocumentStorage documentStorage;
    
    public ReadPasskeyChallengeRepository(DocumentStorage documentStorage) {
        this.documentStorage = documentStorage;
    }

    /**
     * Find a challenge by its ID
     * 
     * @param challengeId Challenge ID
     * @return Optional containing the challenge if found
     */
    public Optional<PasskeyChallenge> findById(String challengeId) {
        try {
            Firestore firestore = documentStorage.getDocumentDb();
            
            DocumentSnapshot document = firestore
                .collection(COLLECTION_PASSKEY_CHALLENGES)
                .document(challengeId)
                .get()
                .get();
            
            if (!document.exists()) {
                return Optional.empty();
            }
            
            PasskeyChallenge challenge = document.toObject(PasskeyChallenge.class);
            if (challenge != null) {
                challenge.setId(document.getId());
                return Optional.of(challenge);
            }
            
            return Optional.empty();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error finding passkey challenge by ID {}: {}", challengeId, e.getMessage(), e);
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    /**
     * Find challenges by user ID
     * 
     * @param userId User ID
     * @return List of challenges for the user
     */
    public List<PasskeyChallenge> findByUserId(String userId) {
        try {
            Firestore firestore = documentStorage.getDocumentDb();
            
            QuerySnapshot query = firestore
                .collection(COLLECTION_PASSKEY_CHALLENGES)
                .whereEqualTo(FIELD_USER_ID, userId)
                .get()
                .get();
            
            if (query.isEmpty()) {
                return Collections.emptyList();
            }
            
            List<PasskeyChallenge> challenges = new ArrayList<>();
            for (DocumentSnapshot doc : query.getDocuments()) {
                PasskeyChallenge challenge = doc.toObject(PasskeyChallenge.class);
                if (challenge != null) {
                    challenge.setId(doc.getId());
                    challenges.add(challenge);
                }
            }
            
            return challenges;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error finding passkey challenges for user {}: {}", userId, e.getMessage(), e);
            Thread.currentThread().interrupt();
            return Collections.emptyList();
        }
    }

    /**
     * Find a challenge by its challenge string
     * 
     * @param challenge Challenge string
     * @return Optional containing the challenge if found
     */
    public Optional<PasskeyChallenge> findByChallenge(String challenge) {
        try {
            Firestore firestore = documentStorage.getDocumentDb();
            
            QuerySnapshot query = firestore
                .collection(COLLECTION_PASSKEY_CHALLENGES)
                .whereEqualTo(FIELD_CHALLENGE, challenge)
                .get()
                .get();
            
            if (query.isEmpty()) {
                return Optional.empty();
            }
            
            DocumentSnapshot doc = query.getDocuments().get(0);
            PasskeyChallenge passkeyChallenge = doc.toObject(PasskeyChallenge.class);
            if (passkeyChallenge != null) {
                passkeyChallenge.setId(doc.getId());
                return Optional.of(passkeyChallenge);
            }
            
            return Optional.empty();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error finding passkey challenge by challenge string: {}", challenge, e);
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }
}
