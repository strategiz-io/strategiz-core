package io.strategiz.data.session.repository;

import io.strategiz.data.session.entity.UserSessionEntity;
import org.springframework.data.repository.Repository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data repository interface for managing user sessions
 * Uses Spring Data method naming conventions where possible
 */
@Component
public interface SessionRepository extends Repository<UserSessionEntity, String> {

    /**
     * Save a session (create or update)
     * @param session The session to save
     * @param userId Who is saving it
     * @return The saved session
     */
    UserSessionEntity save(UserSessionEntity session, String userId);

    /**
     * Find a session by session ID
     * @param sessionId The session identifier
     * @return Optional containing the session if found
     */
    Optional<UserSessionEntity> findById(String sessionId);

    /**
     * Find all active sessions for a user - Spring Data naming convention
     * @param userId The user's ID
     * @param active Whether the session is active
     * @return List of active sessions for the user
     */
    List<UserSessionEntity> findByUserIdAndActive(String userId, boolean active);

    /**
     * Find sessions by user ID and device fingerprint - Spring Data naming convention
     * @param userId The user's ID
     * @param deviceFingerprint The device fingerprint
     * @return List of sessions matching the criteria
     */
    List<UserSessionEntity> findByUserIdAndDeviceFingerprint(String userId, String deviceFingerprint);

    /**
     * Find sessions by IP address - Spring Data naming convention
     * @param ipAddress The IP address
     * @return List of sessions from this IP
     */
    List<UserSessionEntity> findByIpAddress(String ipAddress);

    /**
     * Find sessions that expire before a certain time - Spring Data naming convention
     * @param expiresAt Sessions that expire before this time
     * @return List of expired sessions
     */
    List<UserSessionEntity> findByExpiresAtBefore(Instant expiresAt);

    /**
     * Count active sessions for a user - Spring Data naming convention
     * @param userId The user's ID
     * @param active Whether the session is active
     * @return Number of active sessions for the user
     */
    long countByUserIdAndActive(String userId, boolean active);

    /**
     * Find active sessions by session ID - custom query method
     * @param sessionId The session identifier
     * @param active Whether the session is active
     * @return Optional containing the session if found and active
     */
    Optional<UserSessionEntity> findBySessionIdAndActive(String sessionId, boolean active);

    /**
     * Delete a session by ID - Spring Data naming convention
     * @param sessionId The session ID to delete
     */
    void deleteById(String sessionId);

    /**
     * Check if a session exists by ID - Spring Data naming convention
     * @param sessionId The session ID to check
     * @return True if session exists
     */
    boolean existsById(String sessionId);

    // Custom methods that don't follow Spring Data naming conventions
    // These will need custom implementation

    /**
     * Terminate all sessions for a user (custom business logic)
     * @param userId The user's ID
     * @param reason The reason for termination
     * @return Number of sessions terminated
     */
    int terminateAllSessionsForUser(String userId, String reason);

    /**
     * Clean up expired sessions (custom business logic)
     * @param beforeTime Remove sessions that expired before this time
     * @return Number of sessions cleaned up
     */
    int cleanupExpiredSessions(Instant beforeTime);
}