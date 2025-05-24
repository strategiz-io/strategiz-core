package io.strategiz.service.auth;

import io.strategiz.data.auth.Session;
import io.strategiz.data.auth.SessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for session management
 */
@Service
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);
    
    @Value("${session.expiry.seconds:86400}") // Default to 24 hours
    private long sessionExpirySeconds;
    
    private final SessionRepository sessionRepository;
    
    @Autowired
    public SessionService(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }
    
    /**
     * Create a new session for a user
     *
     * @param userId User ID
     * @return The created session
     */
    public Session createSession(String userId) {
        log.info("Creating new session for user: {}", userId);
        
        long now = Instant.now().getEpochSecond();
        
        Session session = Session.builder()
                .userId(userId)
                .token(generateSessionToken())
                .createdAt(now)
                .lastAccessedAt(now)
                .expiresAt(now + sessionExpirySeconds)
                .build();
        
        return sessionRepository.save(session);
    }
    
    /**
     * Validate a session by token
     *
     * @param token Session token
     * @return Optional containing the session if valid, empty otherwise
     */
    public Optional<Session> validateSession(String token) {
        if (token == null || token.isEmpty()) {
            log.warn("Token is null or empty");
            return Optional.empty();
        }
        
        log.debug("Validating session token");
        
        Optional<Session> sessionOpt = sessionRepository.findByToken(token);
        
        if (sessionOpt.isEmpty()) {
            log.debug("Session not found for token");
            return Optional.empty();
        }
        
        Session session = sessionOpt.get();
        
        if (session.isExpired()) {
            log.info("Session expired for user: {}", session.getUserId());
            sessionRepository.deleteById(session.getId());
            return Optional.empty();
        }
        
        // Update last accessed time and extend expiry
        session.updateLastAccessedTime();
        session.extendExpiry(sessionExpirySeconds);
        sessionRepository.save(session);
        
        return Optional.of(session);
    }
    
    /**
     * Get all sessions for a user
     *
     * @param userId User ID
     * @return List of sessions
     */
    public List<Session> getUserSessions(String userId) {
        log.info("Getting sessions for user: {}", userId);
        return sessionRepository.findAllByUserId(userId);
    }
    
    /**
     * Delete a session
     *
     * @param sessionId Session ID
     * @return true if deleted, false otherwise
     */
    public boolean deleteSession(String sessionId) {
        log.info("Deleting session: {}", sessionId);
        return sessionRepository.deleteById(sessionId);
    }
    
    /**
     * Delete all sessions for a user
     *
     * @param userId User ID
     * @return true if deleted, false otherwise
     */
    public boolean deleteUserSessions(String userId) {
        log.info("Deleting all sessions for user: {}", userId);
        return sessionRepository.deleteAllByUserId(userId);
    }
    
    /**
     * Generate a unique session token
     *
     * @return Session token
     */
    private String generateSessionToken() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Scheduled task to clean up expired sessions
     * Runs every hour
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    public void cleanupExpiredSessions() {
        log.info("Cleaning up expired sessions");
        int deleted = sessionRepository.deleteExpiredSessions();
        log.info("Deleted {} expired sessions", deleted);
    }
}
