package io.strategiz.auth.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import io.strategiz.auth.model.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Firebase implementation of the FirebaseSessionRepository interface
 */
@Repository
public class FirebaseSessionRepositoryImpl implements FirebaseSessionRepository {
    private static final Logger logger = LoggerFactory.getLogger(FirebaseSessionRepositoryImpl.class);
    private static final String SESSIONS_COLLECTION = "sessions";

    @Autowired
    private Firestore firestore;

    @Override
    public CompletableFuture<Session> save(Session session) {
        CompletableFuture<Session> future = new CompletableFuture<>();
        
        DocumentReference docRef;
        if (session.getId() == null || session.getId().isEmpty()) {
            docRef = firestore.collection(SESSIONS_COLLECTION).document();
            session.setId(docRef.getId());
        } else {
            docRef = firestore.collection(SESSIONS_COLLECTION).document(session.getId());
        }
        
        ApiFuture<WriteResult> writeResultFuture = docRef.set(session.toMap());
        writeResultFuture.addListener(() -> {
            logger.info("Saved session {}", session.getId());
            future.complete(session);
        }, Runnable::run);
        
        return future;
    }

    @Override
    public CompletableFuture<Optional<Session>> findById(String sessionId) {
        CompletableFuture<Optional<Session>> future = new CompletableFuture<>();
        
        ApiFuture<DocumentSnapshot> docFuture = firestore.collection(SESSIONS_COLLECTION).document(sessionId).get();
        docFuture.addListener(() -> {
            try {
                DocumentSnapshot doc = docFuture.get();
                if (doc.exists()) {
                    Session session = Session.fromMap(doc.getId(), doc.getData());
                    future.complete(Optional.of(session));
                } else {
                    future.complete(Optional.empty());
                }
            } catch (Exception e) {
                logger.error("Error finding session {}: {}", sessionId, e.getMessage());
                future.completeExceptionally(e);
            }
        }, Runnable::run);
        
        return future;
    }

    @Override
    public CompletableFuture<List<Session>> findByUserId(String userId) {
        CompletableFuture<List<Session>> future = new CompletableFuture<>();
        
        ApiFuture<QuerySnapshot> queryFuture = firestore.collection(SESSIONS_COLLECTION)
            .whereEqualTo("userId", userId)
            .get();
            
        queryFuture.addListener(() -> {
            try {
                QuerySnapshot querySnapshot = queryFuture.get();
                
                List<Session> sessions = querySnapshot.getDocuments().stream()
                    .map(doc -> Session.fromMap(doc.getId(), doc.getData()))
                    .collect(Collectors.toList());
                
                future.complete(sessions);
            } catch (Exception e) {
                logger.error("Error finding sessions for user {}: {}", userId, e.getMessage());
                future.completeExceptionally(e);
            }
        }, Runnable::run);
        
        return future;
    }

    @Override
    public CompletableFuture<Boolean> deleteById(String sessionId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        ApiFuture<WriteResult> deleteFuture = firestore.collection(SESSIONS_COLLECTION).document(sessionId).delete();
        deleteFuture.addListener(() -> {
            logger.info("Deleted session {}", sessionId);
            future.complete(true);
        }, Runnable::run);
        
        return future;
    }

    @Override
    public CompletableFuture<Integer> deleteByUserId(String userId) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        
        ApiFuture<QuerySnapshot> queryFuture = firestore.collection(SESSIONS_COLLECTION)
            .whereEqualTo("userId", userId)
            .get();
            
        queryFuture.addListener(() -> {
            try {
                QuerySnapshot querySnapshot = queryFuture.get();
                
                List<DocumentReference> docs = querySnapshot.getDocuments().stream()
                    .map(DocumentSnapshot::getReference)
                    .collect(Collectors.toList());
                
                int count = docs.size();
                
                // Delete each document
                WriteBatch batch = firestore.batch();
                for (DocumentReference docRef : docs) {
                    batch.delete(docRef);
                }
                
                batch.commit().get();
                logger.info("Deleted {} sessions for user {}", count, userId);
                future.complete(count);
            } catch (Exception e) {
                logger.error("Error deleting sessions for user {}: {}", userId, e.getMessage());
                future.completeExceptionally(e);
            }
        }, Runnable::run);
        
        return future;
    }

    @Override
    public CompletableFuture<Integer> deleteExpiredSessions(long currentTimeSeconds) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        
        ApiFuture<QuerySnapshot> queryFuture = firestore.collection(SESSIONS_COLLECTION)
            .whereLessThan("expiresAt", currentTimeSeconds)
            .get();
            
        queryFuture.addListener(() -> {
            try {
                QuerySnapshot querySnapshot = queryFuture.get();
                
                List<DocumentReference> docs = querySnapshot.getDocuments().stream()
                    .map(DocumentSnapshot::getReference)
                    .collect(Collectors.toList());
                
                int count = docs.size();
                
                // Delete each document
                WriteBatch batch = firestore.batch();
                for (DocumentReference docRef : docs) {
                    batch.delete(docRef);
                }
                
                batch.commit().get();
                logger.info("Deleted {} expired sessions", count);
                future.complete(count);
            } catch (Exception e) {
                logger.error("Error deleting expired sessions: {}", e.getMessage());
                future.completeExceptionally(e);
            }
        }, Runnable::run);
        
        return future;
    }
}
