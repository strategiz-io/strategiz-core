package io.strategiz.auth.repository;

import io.strategiz.auth.model.Session;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Repository interface for Firebase-based session management
 * Follows Spring Data repository pattern
 */
public interface FirebaseSessionRepository {

    /**
     * Saves a session to Firestore
     *
     * @param session The session to save
     * @return The saved session
     */
    CompletableFuture<Session> save(Session session);

    /**
     * Finds a session by its ID
     *
     * @param sessionId The session ID
     * @return Optional containing the session if found
     */
    CompletableFuture<Optional<Session>> findById(String sessionId);

    /**
     * Finds all sessions for a user
     *
     * @param userId The user ID
     * @return List of sessions
     */
    CompletableFuture<List<Session>> findByUserId(String userId);

    /**
     * Deletes a session
     *
     * @param sessionId The session ID
     * @return True if deleted, false otherwise
     */
    CompletableFuture<Boolean> deleteById(String sessionId);

    /**
     * Deletes all sessions for a user
     *
     * @param userId The user ID
     * @return Number of sessions deleted
     */
    CompletableFuture<Integer> deleteByUserId(String userId);

    /**
     * Deletes all expired sessions
     *
     * @param currentTimeSeconds Current time in seconds
     * @return Number of sessions deleted
     */
    CompletableFuture<Integer> deleteExpiredSessions(long currentTimeSeconds);
}
