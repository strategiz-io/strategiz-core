package io.strategiz.service.auth.controller.session;

import io.strategiz.service.auth.service.session.SessionService;
import io.strategiz.service.auth.model.session.RefreshSessionRequest;
import io.strategiz.service.auth.model.session.RefreshSessionResponse;
import io.strategiz.service.auth.model.session.RevocationResponse;
import io.strategiz.service.auth.model.session.RevokeAllResponse;
import io.strategiz.service.auth.model.session.SessionRevocationRequest;
import io.strategiz.service.auth.model.session.SessionValidationRequest;
import io.strategiz.service.auth.model.session.SessionValidationResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * Controller for session management operations.
 * Handles session refresh, validation, and revocation.
 * Uses clean architecture - returns resources directly, no wrappers.
 */
@RestController
@RequestMapping("/auth/session")
public class SessionController {

    private static final Logger log = LoggerFactory.getLogger(SessionController.class);
    
    private final SessionService sessionService;
    
    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }
    
    /**
     * Refresh an existing session
     * 
     * @param request Session refresh request containing refresh token
     * @return Clean refresh response with new tokens - no wrapper, let GlobalExceptionHandler handle errors
     */
    @PostMapping("/refresh")
    public ResponseEntity<RefreshSessionResponse> refreshSession(@Valid @RequestBody RefreshSessionRequest request,
                                                               jakarta.servlet.http.HttpServletRequest httpRequest) {
        log.info("Refreshing session with refresh token");
        
        // Extract IP address
        String ipAddress = getClientIp(httpRequest);
        
        // Refresh session - let exceptions bubble up
        java.util.Optional<String> newTokenOpt = sessionService.refreshToken(request.refreshToken(), ipAddress);
        
        if (newTokenOpt.isEmpty()) {
            throw new RuntimeException("Failed to refresh session - invalid refresh token");
        }
        
        RefreshSessionResponse response = new RefreshSessionResponse(
            newTokenOpt.get(),
            request.refreshToken(), // Keep same refresh token for now
            86400L // 24 hours in seconds
        );
        
        // Return clean response - headers added by StandardHeadersInterceptor
        return ResponseEntity.ok(response);
    }
    
    /**
     * Extract client IP address from request
     */
    private String getClientIp(jakarta.servlet.http.HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
    
    /**
     * Validate a session token
     * 
     * @param request Session validation request containing access token
     * @return Clean validation response - no wrapper, let GlobalExceptionHandler handle errors
     */
    @PostMapping("/validate")
    public ResponseEntity<SessionValidationResponse> validateSession(@Valid @RequestBody SessionValidationRequest request) {
        log.info("Validating session token");
        
        // Validate session - let exceptions bubble up
        boolean isValid = sessionService.validateSession(request.accessToken());
        java.util.Optional<String> userIdOpt = sessionService.getUserIdFromToken(request.accessToken());
        
        SessionValidationResponse response = new SessionValidationResponse(
            isValid,
            userIdOpt.orElse(null),
            System.currentTimeMillis() / 1000 + 86400L // Current time + 24 hours
        );
        
        // Return clean response - headers added by StandardHeadersInterceptor
        return ResponseEntity.ok(response);
    }
    
    /**
     * Revoke a specific session
     * 
     * @param request Session revocation request containing session ID (token) and user ID
     * @return Clean revocation response - no wrapper, let GlobalExceptionHandler handle errors
     */
    @PostMapping("/revoke")
    public ResponseEntity<RevocationResponse> revokeSession(@Valid @RequestBody SessionRevocationRequest request) {
        log.info("Revoking session: {}", request.sessionId());
        
        // Revoke session - let exceptions bubble up
        sessionService.deleteSession(request.sessionId());
        
        RevocationResponse response = new RevocationResponse(
            true,
            "Session revoked successfully"
        );
        
        // Return clean response - headers added by StandardHeadersInterceptor
        return ResponseEntity.ok(response);
    }
    
    /**
     * Revoke all sessions for a user
     * 
     * @param userId The user ID whose sessions to revoke
     * @return Clean revocation response - no wrapper, let GlobalExceptionHandler handle errors
     */
    @PostMapping("/revoke-all/{userId}")
    public ResponseEntity<RevokeAllResponse> revokeAllSessions(@PathVariable String userId) {
        log.info("Revoking all sessions for user: {}", userId);
        
        // Revoke all sessions - let exceptions bubble up
        boolean deleted = sessionService.deleteUserSessions(userId);
        
        RevokeAllResponse response = new RevokeAllResponse(
            deleted ? 1 : 0, // Simple count - in real implementation would return actual count
            deleted ? "All sessions revoked successfully" : "No sessions found to revoke"
        );
        
        // Return clean response - headers added by StandardHeadersInterceptor
        return ResponseEntity.ok(response);
    }
}
