package io.strategiz.service.auth.service.session;

import io.strategiz.business.session.service.SessionBusiness;
import io.strategiz.business.session.model.SessionValidationResult;
import io.strategiz.data.session.entity.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Optional;

/**
 * Service implementation for session management operations
 * Orchestrates session-related business logic using SessionBusiness
 */
@Service("sessionService")
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    private final SessionBusiness sessionBusiness;

    @Autowired
    public SessionService(SessionBusiness sessionBusiness) {
        this.sessionBusiness = sessionBusiness;
        log.info("SessionService initialized with business-session architecture");
    }

    /**
     * Create a new user session after successful authentication
     */
    public UserSession createSession(String userId, String userEmail, 
                                   HttpServletRequest request, 
                                   String acr, String aal, List<String> amr) {
        log.info("Creating session for user: {} with ACR: {}, AAL: {}", userId, acr, aal);
        return sessionBusiness.createSession(userId, userEmail, request, acr, aal, amr);
    }

    /**
     * Validate current session from HTTP request
     */
    public Optional<SessionValidationResult> validateSession(HttpServletRequest request) {
        log.debug("Validating session from HTTP request");
        return sessionBusiness.validateSession(request);
    }

    /**
     * Validate session by session ID
     */
    public Optional<SessionValidationResult> validateSessionById(String sessionId) {
        log.debug("Validating session by ID: {}", sessionId);
        return sessionBusiness.validateSessionById(sessionId);
    }

    /**
     * Refresh current session's expiration time
     */
    public boolean refreshSession(HttpServletRequest request) {
        log.debug("Refreshing session from HTTP request");
        return sessionBusiness.refreshSession(request);
    }

    /**
     * Terminate current session (logout)
     */
    public boolean terminateSession(HttpServletRequest request, HttpServletResponse response) {
        log.info("Terminating current session");
        return sessionBusiness.terminateSession(request, response);
    }

    /**
     * Terminate a specific session by ID
     */
    public boolean terminateSessionById(String sessionId, String reason) {
        log.info("Terminating session {} for reason: {}", sessionId, reason);
        return sessionBusiness.terminateSessionById(sessionId, reason);
    }

    /**
     * Terminate all sessions for a user
     */
    public int terminateAllUserSessions(String userId, String reason) {
        log.info("Terminating all sessions for user: {} for reason: {}", userId, reason);
        return sessionBusiness.terminateAllUserSessions(userId, reason);
    }

    /**
     * Get all active sessions for a user
     */
    public List<UserSession> getUserActiveSessions(String userId) {
        log.debug("Getting active sessions for user: {}", userId);
        return sessionBusiness.getUserActiveSessions(userId);
    }

    /**
     * Count active sessions for a user
     */
    public long countUserActiveSessions(String userId) {
        log.debug("Counting active sessions for user: {}", userId);
        return sessionBusiness.countUserActiveSessions(userId);
    }

    /**
     * Clean up expired sessions (system maintenance)
     */
    public int cleanupExpiredSessions() {
        log.info("Starting cleanup of expired sessions");
        return sessionBusiness.cleanupExpiredSessions();
    }

    /**
     * Check for suspicious activity for a user
     */
    public boolean detectSuspiciousActivity(String userId) {
        log.debug("Checking for suspicious activity for user: {}", userId);
        return sessionBusiness.detectSuspiciousActivity(userId);
    }

    /**
     * Validate session and enforce concurrent session limits
     */
    public boolean enforceConcurrentSessionLimit(String userId, int maxConcurrentSessions) {
        log.debug("Enforcing concurrent session limit for user: {} (max: {})", userId, maxConcurrentSessions);
        return sessionBusiness.enforceConcurrentSessionLimit(userId, maxConcurrentSessions);
    }
}