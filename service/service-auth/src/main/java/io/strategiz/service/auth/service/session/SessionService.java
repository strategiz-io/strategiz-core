package io.strategiz.service.auth.service.session;

import io.strategiz.business.tokenauth.SessionAuthBusiness;
import io.strategiz.business.tokenauth.model.SessionValidationResult;
import io.strategiz.data.session.entity.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.time.Instant;

/**
 * Service implementation for session management operations
 * Adapts SessionAuthBusiness for HTTP session management
 */
@Service("sessionService")
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);
    private static final String AUTH_COOKIE_NAME = "strategiz-token";

    private final SessionAuthBusiness sessionBusiness;

    @Autowired
    public SessionService(SessionAuthBusiness sessionBusiness) {
        this.sessionBusiness = sessionBusiness;
        log.info("SessionService initialized with SessionAuthBusiness");
    }

    /**
     * Create a new user session after successful authentication
     */
    public UserSession createSession(String userId, String userEmail, 
                                   HttpServletRequest request, 
                                   String acr, String aal, List<String> amr) {
        log.info("Creating session for user: {} with ACR: {}, AAL: {}", userId, acr, aal);
        
        // Get device and IP info from request
        String deviceId = request.getHeader("X-Device-Id");
        String ipAddress = request.getRemoteAddr();
        
        // Create token pair using SessionAuthBusiness
        SessionAuthBusiness.TokenPair tokenPair = sessionBusiness.createAuthenticationTokenPair(
            userId, amr, acr, deviceId, ipAddress
        );
        
        // Create a UserSession object for compatibility
        UserSession session = new UserSession();
        session.setUserId(userId);
        session.setUserEmail(userEmail);
        session.setIpAddress(ipAddress);
        session.setDeviceFingerprint(deviceId);
        session.setAcr(acr);
        session.setAal(aal);
        session.setAmr(amr);
        // Note: We don't have access to session ID in the token-based approach
        
        return session;
    }

    /**
     * Validate current session from HTTP request
     */
    public Optional<SessionValidationResult> validateSession(HttpServletRequest request) {
        log.debug("Validating session from HTTP request");
        
        // Extract token from request
        String token = extractTokenFromRequest(request);
        if (token == null) {
            return Optional.empty();
        }
        
        // Validate token
        Optional<String> userIdOpt = sessionBusiness.validateSession(token);
        if (userIdOpt.isEmpty()) {
            return Optional.empty();
        }
        
        // Create validation result with required parameters (AAL removed)
        SessionValidationResult result = new SessionValidationResult(
            userIdOpt.get(),    // userId
            null,               // userEmail - not available from token validation
            null,               // sessionId - not available in token-based approach
            "1",                // acr - default to basic authentication
            null,               // amr - not available from token validation
            "live",             // tradingMode - default to live
            Instant.now(),      // lastAccessedAt
            Instant.now().plusSeconds(3600), // expiresAt - assume 1 hour
            true                // valid
        );
        
        return Optional.of(result);
    }

    /**
     * Validate session by session ID - not supported in token-based approach
     */
    public Optional<SessionValidationResult> validateSessionById(String sessionId) {
        log.debug("Validating session by ID: {} - not supported in token-based approach", sessionId);
        return Optional.empty();
    }

    /**
     * Refresh current session's expiration time
     */
    public boolean refreshSession(HttpServletRequest request) {
        log.debug("Refreshing session from HTTP request");
        
        // Extract and validate token
        String token = extractTokenFromRequest(request);
        if (token == null) {
            return false;
        }
        
        // Token-based sessions don't support refresh in the current implementation
        // Would need to issue a new token
        return sessionBusiness.validateSession(token).isPresent();
    }

    /**
     * Terminate current session (logout)
     */
    public boolean terminateSession(HttpServletRequest request, HttpServletResponse response) {
        log.info("Terminating current session");
        
        // For token-based sessions, we just clear the cookie
        Cookie cookie = new Cookie(AUTH_COOKIE_NAME, "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
        
        return true;
    }

    /**
     * Terminate a specific session by ID - not supported in token-based approach
     */
    public boolean terminateSessionById(String sessionId, String reason) {
        log.info("Terminating session {} for reason: {} - not supported in token-based approach", sessionId, reason);
        return false;
    }

    /**
     * Terminate all sessions for a user - not supported in token-based approach
     */
    public int terminateAllUserSessions(String userId, String reason) {
        log.info("Terminating all sessions for user: {} for reason: {} - not supported in token-based approach", userId, reason);
        return 0;
    }

    /**
     * Get all active sessions for a user - returns empty list for token-based approach
     */
    public List<UserSession> getUserActiveSessions(String userId) {
        log.debug("Getting active sessions for user: {} - not supported in token-based approach", userId);
        return new ArrayList<>();
    }

    /**
     * Count active sessions for a user
     */
    public long countUserActiveSessions(String userId) {
        log.debug("Counting active sessions for user: {} - not supported in token-based approach", userId);
        return 0;
    }

    /**
     * Clean up expired sessions (system maintenance)
     */
    public int cleanupExpiredSessions() {
        log.info("Starting cleanup of expired sessions");
        sessionBusiness.cleanupExpiredTokens();
        return 0; // cleanupExpiredTokens returns void, so return 0 for compatibility
    }

    /**
     * Check for suspicious activity for a user
     */
    public boolean detectSuspiciousActivity(String userId) {
        log.debug("Checking for suspicious activity for user: {} - not implemented", userId);
        return false;
    }

    /**
     * Validate session and enforce concurrent session limits
     */
    public boolean enforceConcurrentSessionLimit(String userId, int maxConcurrentSessions) {
        log.debug("Enforcing concurrent session limit for user: {} (max: {}) - not supported in token-based approach", userId, maxConcurrentSessions);
        return true;
    }
    
    /**
     * Extract token from request (cookie or header)
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        // Check Authorization header first
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        
        // Check cookies
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (AUTH_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        
        return null;
    }
}