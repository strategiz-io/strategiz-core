package io.strategiz.service.auth.service.session;

import io.strategiz.service.auth.model.session.SignOutResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for handling user sign out operations
 * Manages session cleanup and token revocation
 */
@Service
public class SignOutService {

    private static final Logger log = LoggerFactory.getLogger(SignOutService.class);
    
    private final SessionService sessionService;
    
    public SignOutService(SessionService sessionService) {
        this.sessionService = sessionService;
    }
    
    /**
     * Sign out a user and clean up their sessions
     * 
     * @param userId The user ID to sign out
     * @param sessionId The current session ID (token) to revoke
     * @param deviceId The device ID (optional)
     * @param revokeAllSessions Whether to revoke all sessions or just the current one
     * @return SignOutResponse with operation result
     */
    public SignOutResponse signOut(String userId, String sessionId, String deviceId, boolean revokeAllSessions) {
        log.info("Processing sign out for user: {} (revokeAll: {})", userId, revokeAllSessions);
        
        try {
            int sessionsRevoked = 0;
            
            if (revokeAllSessions) {
                // Revoke all sessions for the user
                boolean deleted = sessionService.deleteUserSessions(userId);
                sessionsRevoked = deleted ? 1 : 0; // Simplified count
                log.info("Revoked all sessions for user: {}", userId);
            } else if (sessionId != null && !sessionId.isBlank()) {
                // Revoke just the specific session
                sessionService.deleteSession(sessionId);
                sessionsRevoked = 1;
                log.info("Revoked specific session for user: {}", userId);
            }
            
            return new SignOutResponse(
                true,
                "User signed out successfully",
                sessionsRevoked
            );
            
        } catch (Exception e) {
            log.error("Error during sign out for user {}: {}", userId, e.getMessage());
            return new SignOutResponse(
                false,
                "Sign out failed: " + e.getMessage(),
                0
            );
        }
    }
} 