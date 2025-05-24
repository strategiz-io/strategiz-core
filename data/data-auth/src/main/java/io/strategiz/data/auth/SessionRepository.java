package io.strategiz.data.auth;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Session data access operations
 */
public interface SessionRepository {

    /**
     * Find a session by ID
     *
     * @param sessionId the session ID
     * @return optional session
     */
    Optional<Session> findById(String sessionId);
    
    /**
     * Find a session by token
     *
     * @param token the session token
     * @return optional session
     */
    Optional<Session> findByToken(String token);
    
    /**
     * Find all sessions for a user
     *
     * @param userId the user ID
     * @return list of sessions
     */
    List<Session> findAllByUserId(String userId);
    
    /**
     * Save a session
     *
     * @param session the session to save
     * @return the saved session with ID
     */
    Session save(Session session);
    
    /**
     * Delete a session by ID
     *
     * @param sessionId the session ID
     * @return true if deleted, false otherwise
     */
    boolean deleteById(String sessionId);
    
    /**
     * Delete all sessions for a user
     *
     * @param userId the user ID
     * @return true if deleted, false otherwise
     */
    boolean deleteAllByUserId(String userId);
    
    /**
     * Delete expired sessions
     *
     * @return number of deleted sessions
     */
    int deleteExpiredSessions();
}
