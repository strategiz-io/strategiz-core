package io.strategiz.service.auth.service.session;

import io.strategiz.business.tokenauth.SessionAuthBusiness;
import io.strategiz.business.tokenauth.model.SessionValidationResult;
import io.strategiz.data.session.entity.SessionEntity;
import io.strategiz.service.auth.util.CookieUtil;
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
    private static final String ACCESS_TOKEN_COOKIE = "strategiz-access-token";
    private static final String REFRESH_TOKEN_COOKIE = "strategiz-refresh-token";

    private final SessionAuthBusiness sessionBusiness;
    private final CookieUtil cookieUtil;

    @Autowired
    public SessionService(SessionAuthBusiness sessionBusiness, CookieUtil cookieUtil) {
        this.sessionBusiness = sessionBusiness;
        this.cookieUtil = cookieUtil;
        log.info("SessionService initialized with SessionAuthBusiness");
    }

    /**
     * Create a new user session after successful authentication
     */
    public SessionEntity createSession(String userId, String userEmail, 
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
        
        // Create a SessionEntity object for compatibility
        SessionEntity session = new SessionEntity(userId);
        session.setUserId(userId);
        session.setIpAddress(ipAddress);
        session.setDeviceId(deviceId);
        // Store ACR and AMR in claims
        java.util.Map<String, Object> claims = new java.util.HashMap<>();
        claims.put("acr", acr);
        claims.put("aal", aal);
        claims.put("amr", amr);
        claims.put("email", userEmail);
        session.setClaims(claims);
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

        // Use validateToken which returns full SessionValidationResult with email
        return sessionBusiness.validateToken(token);
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

        // First, try to get the session token and invalidate it in the business layer
        String token = extractTokenFromRequest(request);
        if (token != null) {
            try {
                // Revoke the token/session in the business layer (marks session as revoked in database)
                boolean revoked = sessionBusiness.revokeAuthentication(token);
                if (revoked) {
                    log.info("Session token revoked in business layer");
                } else {
                    log.warn("Session token not found for revocation, continuing with cookie clearing");
                }
            } catch (Exception e) {
                log.warn("Failed to revoke session in business layer: {}", e.getMessage());
                // Continue with cookie clearing even if token revocation fails
            }
        }

        // Clear all authentication cookies using CookieUtil
        cookieUtil.clearAuthCookies(response);
        log.info("All authentication cookies cleared");

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
     * Terminate all sessions for a user by revoking all tokens in the database
     */
    public int terminateAllUserSessions(String userId, String reason) {
        log.info("Terminating all sessions for user: {} for reason: {}", userId, reason);
        try {
            // Use the SessionAuthBusiness to revoke all user authentication
            int revokedCount = sessionBusiness.revokeAllUserAuthentication(userId, reason);
            log.info("Successfully revoked {} sessions for user: {}", revokedCount, userId);
            return revokedCount;
        } catch (Exception e) {
            log.error("Failed to revoke all sessions for user {}: {}", userId, e.getMessage());
            return 0;
        }
    }

    /**
     * Get all active sessions for a user - returns empty list for token-based approach
     */
    public List<SessionEntity> getUserActiveSessions(String userId) {
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
        
        // Check cookies for access token
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (ACCESS_TOKEN_COOKIE.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        
        return null;
    }
}