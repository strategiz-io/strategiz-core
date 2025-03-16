package io.strategiz.auth.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import io.strategiz.auth.model.Session;
import io.strategiz.auth.repository.FirebaseSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Service for managing user sessions
 */
@Service
public class SessionService {
    private static final Logger logger = LoggerFactory.getLogger(SessionService.class);
    private static final long SESSION_EXPIRY_SECONDS = 7 * 24 * 60 * 60; // 7 days

    @Autowired
    private FirebaseSessionRepository sessionRepository;

    /**
     * Creates a new session for a user
     *
     * @param userId The user ID
     * @param token The Firebase ID token
     * @return The session ID
     */
    public String createSession(String userId, String token) {
        try {
            // Verify the token first
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
            
            if (!decodedToken.getUid().equals(userId)) {
                logger.error("Token user ID does not match provided user ID");
                return null;
            }
            
            String sessionId = UUID.randomUUID().toString();
            
            Session session = Session.builder()
                    .id(sessionId)
                    .userId(userId)
                    .token(token)
                    .createdAt(Instant.now().getEpochSecond())
                    .expiresAt(Instant.now().getEpochSecond() + SESSION_EXPIRY_SECONDS)
                    .lastAccessedAt(Instant.now().getEpochSecond())
                    .build();
            
            sessionRepository.save(session).get(); // Wait for completion
            
            logger.info("Created new session {} for user {}", sessionId, userId);
            
            return sessionId;
        } catch (FirebaseAuthException | InterruptedException | ExecutionException e) {
            logger.error("Failed to create session for user {}: {}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * Validates a session and updates its last accessed time
     *
     * @param sessionId The session ID
     * @return The user ID if the session is valid, null otherwise
     */
    public String validateSession(String sessionId) {
        try {
            Optional<Session> optionalSession = sessionRepository.findById(sessionId).get();
            
            if (optionalSession.isEmpty()) {
                logger.warn("Session {} does not exist", sessionId);
                return null;
            }
            
            Session session = optionalSession.get();
            
            if (session.isExpired()) {
                logger.warn("Session {} has expired", sessionId);
                // Clean up expired session
                sessionRepository.deleteById(sessionId).get();
                return null;
            }
            
            // Update last accessed time
            session.updateLastAccessedTime();
            sessionRepository.save(session).get();
            
            String userId = session.getUserId();
            logger.info("Validated session {} for user {}", sessionId, userId);
            
            return userId;
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to validate session {}: {}", sessionId, e.getMessage());
            return null;
        }
    }

    /**
     * Refreshes a session with a new token
     *
     * @param sessionId The session ID
     * @param newToken The new Firebase ID token
     * @return True if the session was refreshed, false otherwise
     */
    public boolean refreshSession(String sessionId, String newToken) {
        try {
            Optional<Session> optionalSession = sessionRepository.findById(sessionId).get();
            
            if (optionalSession.isEmpty()) {
                logger.warn("Session {} does not exist for refresh", sessionId);
                return false;
            }
            
            Session session = optionalSession.get();
            String userId = session.getUserId();
            
            // Verify the new token
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(newToken);
            
            if (!decodedToken.getUid().equals(userId)) {
                logger.error("New token user ID does not match session user ID");
                return false;
            }
            
            // Update session
            session.setToken(newToken);
            session.updateLastAccessedTime();
            session.extendExpiry(SESSION_EXPIRY_SECONDS);
            
            sessionRepository.save(session).get();
            
            logger.info("Refreshed session {} for user {}", sessionId, userId);
            
            return true;
        } catch (FirebaseAuthException | InterruptedException | ExecutionException e) {
            logger.error("Failed to refresh session {}: {}", sessionId, e.getMessage());
            return false;
        }
    }

    /**
     * Invalidates a session
     *
     * @param sessionId The session ID
     * @return True if the session was invalidated, false otherwise
     */
    public boolean invalidateSession(String sessionId) {
        try {
            boolean deleted = sessionRepository.deleteById(sessionId).get();
            
            if (deleted) {
                logger.info("Invalidated session {}", sessionId);
            } else {
                logger.warn("Session {} does not exist for invalidation", sessionId);
            }
            
            return deleted;
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to invalidate session {}: {}", sessionId, e.getMessage());
            return false;
        }
    }

    /**
     * Invalidates all sessions for a user
     *
     * @param userId The user ID
     * @return The number of sessions invalidated
     */
    public int invalidateAllUserSessions(String userId) {
        try {
            int count = sessionRepository.deleteByUserId(userId).get();
            logger.info("Invalidated {} sessions for user {}", count, userId);
            return count;
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to invalidate sessions for user {}: {}", userId, e.getMessage());
            return 0;
        }
    }
    
    /**
     * Scheduled task to clean up expired sessions
     * Runs every hour
     */
    @Scheduled(fixedRate = 60 * 60 * 1000) // Every hour
    public void cleanupExpiredSessions() {
        try {
            long currentTime = Instant.now().getEpochSecond();
            int count = sessionRepository.deleteExpiredSessions(currentTime).get();
            if (count > 0) {
                logger.info("Cleaned up {} expired sessions", count);
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to clean up expired sessions: {}", e.getMessage());
        }
    }
}
