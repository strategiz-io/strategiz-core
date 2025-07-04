package io.strategiz.service.auth.service.session;

import io.strategiz.business.tokenauth.SessionAuthBusiness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service for session management that delegates to SessionAuthBusiness
 * This is a simplified proxy service to maintain compatibility during migration
 * from api-auth to service-auth.
 */
@Service
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);
    private final SessionAuthBusiness sessionAuthBusiness;
    
    public SessionService(SessionAuthBusiness sessionAuthBusiness) {
        this.sessionAuthBusiness = sessionAuthBusiness;
        log.info("Using SessionAuthBusiness for token management");
    }
    
    /**
     * Create a new session for a user
     * @param userId the user ID
     * @param deviceId Optional device ID
     * @param ipAddress Optional IP address
     * @return the access token
     */
    public String createSession(String userId, String deviceId, String ipAddress) {
        // Use ACR "2.1" (basic assurance) as default for generic session creation
        // Assume basic authentication method since context is unknown
        SessionAuthBusiness.TokenPair tokenPair = sessionAuthBusiness.createAuthenticationTokenPair(
            userId,
            List.of("password"), // Default to password auth for generic sessions
            "2.1", // ACR "2.1" - Basic assurance
            deviceId,
            ipAddress
        );
        return tokenPair.accessToken();
    }
    
    /**
     * Create a new session for a user with minimal info
     * @param userId the user ID
     * @return the access token
     */
    public String createSession(String userId) {
        // Use ACR "2.1" (basic assurance) as default for generic session creation
        SessionAuthBusiness.TokenPair tokenPair = sessionAuthBusiness.createAuthenticationTokenPair(
            userId,
            List.of("password"), // Default to password auth for generic sessions
            "2.1", // ACR "2.1" - Basic assurance
            null, // No device ID
            null  // No IP address
        );
        return tokenPair.accessToken();
    }

    /**
     * Validate a session token
     *
     * @param token Session token
     * @return true if token is valid, false otherwise
     */
    public boolean validateSession(String token) {
        if (token == null || token.isBlank()) {
            log.warn("Token is null or empty");
            return false;
        }
        log.debug("Validating session token");
        
        try {
            return sessionAuthBusiness.validateSession(token).isPresent();
        } catch (Exception e) {
            log.warn("Error validating token: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get user ID from token
     *
     * @param token Session token
     * @return User ID if token is valid, empty otherwise
     */
    public Optional<String> getUserIdFromToken(String token) {
        if (token == null || token.isBlank()) {
            log.warn("Token is null or empty");
            return Optional.empty();
        }
        
        try {
            return sessionAuthBusiness.validateSession(token);
        } catch (Exception e) {
            log.warn("Error extracting user ID from token: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Delete (revoke) a session
     * @param token the session token
     */
    public void deleteSession(String token) {
        try {
            sessionAuthBusiness.deleteSession(token);
            log.info("Session token revoked successfully");
        } catch (Exception e) {
            log.warn("Error revoking token: {}", e.getMessage());
        }
    }
    
    /**
     * Delete all sessions for a user
     * @param userId the user ID
     * @return true if any sessions were deleted, false otherwise
     */
    public boolean deleteUserSessions(String userId) {
        try {
            boolean result = sessionAuthBusiness.deleteUserSessions(userId);
            if (result) {
                log.info("All sessions for user {} revoked successfully", userId);
            } else {
                log.info("No active sessions found for user {}", userId);
            }
            return result;
        } catch (Exception e) {
            log.warn("Error deleting user sessions for {}: {}", userId, e.getMessage());
            return false;
        }
    }

    /**
     * Refresh an access token
     * @param refreshToken the refresh token
     * @param ipAddress the IP address
     * @return the new access token if successful, empty otherwise
     */
    public Optional<String> refreshToken(String refreshToken, String ipAddress) {
        return sessionAuthBusiness.refreshAccessToken(refreshToken, ipAddress);
    }

    /**
     * Clean up expired sessions
     */
    @Scheduled(fixedRate = 3600000) // Run every hour
    public void cleanupExpiredSessions() {
        log.info("Delegating cleanup of expired tokens to SessionAuthBusiness");
        sessionAuthBusiness.cleanupExpiredTokens();
    }
}
