package io.strategiz.data.auth;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import io.strategiz.data.base.document.DocumentStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Firestore implementation of the SessionRepository interface
 */
@Repository
public class FirestoreSessionRepository implements SessionRepository {

    private static final Logger log = LoggerFactory.getLogger(FirestoreSessionRepository.class);
    private static final String COLLECTION_PATH = "sessions";

    private final DocumentStorageService documentStorage;

    @Autowired
    public FirestoreSessionRepository(DocumentStorageService documentStorage) {
        this.documentStorage = documentStorage;
    }

    @Override
    public Optional<Session> findById(String sessionId) {
        try {
            Firestore firestore = documentStorage.getDocumentDb();
            DocumentSnapshot document = firestore.collection(COLLECTION_PATH)
                .document(sessionId)
                .get()
                .get();
            
            if (!document.exists()) {
                return Optional.empty();
            }
            
            Session session = document.toObject(Session.class);
            if (session == null) {
                return Optional.empty();
            }
            
            session.setId(document.getId());
            return Optional.of(session);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error finding session by id: {}", sessionId, e);
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    @Override
    public Optional<Session> findByToken(String token) {
        try {
            Firestore firestore = documentStorage.getDocumentDb();
            QuerySnapshot querySnapshot = firestore.collection(COLLECTION_PATH)
                .whereEqualTo("token", token)
                .get()
                .get();
            
            if (querySnapshot.isEmpty()) {
                return Optional.empty();
            }
            
            DocumentSnapshot document = querySnapshot.getDocuments().get(0);
            Session session = document.toObject(Session.class);
            if (session == null) {
                return Optional.empty();
            }
            
            session.setId(document.getId());
            return Optional.of(session);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error finding session by token", e);
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    @Override
    public List<Session> findAllByUserId(String userId) {
        try {
            List<Session> sessions = new ArrayList<>();
            Firestore firestore = documentStorage.getDocumentDb();
            
            QuerySnapshot querySnapshot = firestore.collection(COLLECTION_PATH)
                .whereEqualTo("userId", userId)
                .get()
                .get();
            
            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                Session session = document.toObject(Session.class);
                if (session != null) {
                    session.setId(document.getId());
                    sessions.add(session);
                }
            }
            
            return sessions;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error finding sessions for user: {}", userId, e);
            Thread.currentThread().interrupt();
            return List.of();
        }
    }

    @Override
    public Session save(Session session) {
        try {
            Firestore firestore = documentStorage.getDocumentDb();
            DocumentReference docRef;
            
            if (session.getId() != null && !session.getId().isEmpty()) {
                // Update existing session
                docRef = firestore.collection(COLLECTION_PATH)
                    .document(session.getId());
            } else {
                // Create new session
                docRef = firestore.collection(COLLECTION_PATH)
                    .document();
                session.setId(docRef.getId());
            }
            
            // Save to Firestore
            ApiFuture<WriteResult> result = docRef.set(session);
            result.get(); // Wait for write to complete
            
            return session;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error saving session: {}", session.getId(), e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to save session", e);
        }
    }

    @Override
    public boolean deleteById(String sessionId) {
        try {
            Firestore firestore = documentStorage.getDocumentDb();
            ApiFuture<WriteResult> result = firestore.collection(COLLECTION_PATH)
                .document(sessionId)
                .delete();
            
            result.get(); // Wait for delete to complete
            return true;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error deleting session: {}", sessionId, e);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public boolean deleteAllByUserId(String userId) {
        try {
            Firestore firestore = documentStorage.getDocumentDb();
            
            // Get all sessions for the user
            QuerySnapshot querySnapshot = firestore.collection(COLLECTION_PATH)
                .whereEqualTo("userId", userId)
                .get()
                .get();
            
            // Delete each session
            WriteBatch batch = firestore.batch();
            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                batch.delete(document.getReference());
            }
            
            // Commit the batch
            batch.commit().get();
            
            return true;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error deleting sessions for user: {}", userId, e);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public int deleteExpiredSessions() {
        try {
            Firestore firestore = documentStorage.getDocumentDb();
            long now = Instant.now().getEpochSecond();
            
            // Get all expired sessions
            QuerySnapshot querySnapshot = firestore.collection(COLLECTION_PATH)
                .whereLessThan("expiresAt", now)
                .get()
                .get();
            
            int count = querySnapshot.size();
            
            // Delete each expired session
            WriteBatch batch = firestore.batch();
            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                batch.delete(document.getReference());
            }
            
            // Commit the batch
            batch.commit().get();
            
            return count;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error deleting expired sessions", e);
            Thread.currentThread().interrupt();
            return 0;
        }
    }
}
