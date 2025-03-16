package io.strategiz.auth.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import io.strategiz.auth.model.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Firestore implementation of the SessionRepository
 */
@Repository
public class FirestoreSessionRepository implements SessionRepository {
    private static final Logger logger = LoggerFactory.getLogger(FirestoreSessionRepository.class);
    private static final String COLLECTION_NAME = "sessions";

    private final Firestore firestore;

    @Autowired
    public FirestoreSessionRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    @Override
    public CompletableFuture<Void> save(Session session) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        try {
            DocumentReference docRef;
            if (session.getId() != null && !session.getId().isEmpty()) {
                docRef = firestore.collection(COLLECTION_NAME).document(session.getId());
            } else {
                docRef = firestore.collection(COLLECTION_NAME).document();
                session.setId(docRef.getId());
            }
            
            ApiFuture<WriteResult> result = docRef.set(session);
            
            result.addListener(() -> {
                try {
                    result.get();
                    logger.info("Saved session: {}", session.getId());
                    future.complete(null);
                } catch (Exception e) {
                    logger.error("Failed to save session: {}", session.getId(), e);
                    future.completeExceptionally(e);
                }
            }, Runnable::run);
        } catch (Exception e) {
            logger.error("Error saving session", e);
            future.completeExceptionally(e);
        }
        
        return future;
    }

    @Override
    public CompletableFuture<Optional<Session>> findById(String id) {
        CompletableFuture<Optional<Session>> future = new CompletableFuture<>();
        
        try {
            DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(id);
            ApiFuture<DocumentSnapshot> documentSnapshot = docRef.get();
            
            documentSnapshot.addListener(() -> {
                try {
                    DocumentSnapshot snapshot = documentSnapshot.get();
                    if (snapshot.exists()) {
                        Session session = snapshot.toObject(Session.class);
                        logger.info("Found session: {}", id);
                        future.complete(Optional.ofNullable(session));
                    } else {
                        logger.info("Session not found: {}", id);
                        future.complete(Optional.empty());
                    }
                } catch (Exception e) {
                    logger.error("Failed to get session: {}", id, e);
                    future.completeExceptionally(e);
                }
            }, Runnable::run);
        } catch (Exception e) {
            logger.error("Error finding session by ID", e);
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
                    logger.info("Deleted session: {}", id);
                    future.complete(null);
                } catch (Exception e) {
                    logger.error("Failed to delete session: {}", id, e);
                    future.completeExceptionally(e);
                }
            }, Runnable::run);
        } catch (Exception e) {
            logger.error("Error deleting session", e);
            future.completeExceptionally(e);
        }
        
        return future;
    }

    @Override
    public CompletableFuture<List<Session>> findByUserId(String userId) {
        CompletableFuture<List<Session>> future = new CompletableFuture<>();
        
        try {
            Query query = firestore.collection(COLLECTION_NAME).whereEqualTo("userId", userId);
            ApiFuture<QuerySnapshot> querySnapshot = query.get();
            
            querySnapshot.addListener(() -> {
                try {
                    List<Session> sessions = new ArrayList<>();
                    QuerySnapshot snapshot = querySnapshot.get();
                    
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        Session session = document.toObject(Session.class);
                        if (session != null) {
                            sessions.add(session);
                        }
                    }
                    
                    logger.info("Found {} sessions for user: {}", sessions.size(), userId);
                    future.complete(sessions);
                } catch (Exception e) {
                    logger.error("Failed to get sessions for user: {}", userId, e);
                    future.completeExceptionally(e);
                }
            }, Runnable::run);
        } catch (Exception e) {
            logger.error("Error finding sessions by user ID", e);
            future.completeExceptionally(e);
        }
        
        return future;
    }
}
