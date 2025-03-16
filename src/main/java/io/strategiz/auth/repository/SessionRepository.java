package io.strategiz.auth.repository;

import io.strategiz.auth.model.Session;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Repository for managing sessions
 */
@Repository
public interface SessionRepository {
    /**
     * Saves a session
     * @param session The session to save
     * @return A CompletableFuture that completes when the operation is done
     */
    CompletableFuture<Void> save(Session session);

    /**
     * Finds a session by its ID
     * @param id The session ID
     * @return A CompletableFuture that completes with the optional session
     */
    CompletableFuture<Optional<Session>> findById(String id);

    /**
     * Deletes a session by its ID
     * @param id The session ID
     * @return A CompletableFuture that completes when the operation is done
     */
    CompletableFuture<Void> deleteById(String id);
    
    /**
     * Finds all sessions for a user
     * @param userId The user ID
     * @return A CompletableFuture that completes with a list of sessions
     */
    CompletableFuture<java.util.List<Session>> findByUserId(String userId);
}
