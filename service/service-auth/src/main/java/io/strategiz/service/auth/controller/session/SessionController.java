package io.strategiz.service.auth.controller.session;

import io.strategiz.service.auth.service.session.SessionService;
import io.strategiz.service.auth.service.session.TokenSessionService;
import io.strategiz.service.auth.model.session.RefreshSessionRequest;
import io.strategiz.service.auth.model.session.RefreshSessionResponse;
import io.strategiz.service.auth.model.session.RevocationResponse;
import io.strategiz.service.auth.model.session.RevokeAllResponse;
import io.strategiz.service.auth.model.session.SessionRevocationRequest;
import io.strategiz.service.auth.model.session.SessionValidationRequest;
import io.strategiz.service.auth.model.session.SessionValidationResponse;
import io.strategiz.service.auth.model.session.CurrentUserResponse;
import io.strategiz.service.auth.exception.ServiceAuthErrorDetails;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.business.tokenauth.model.SessionValidationResult;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.base.constants.ModuleConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/v1/auth/session")
public class SessionController extends BaseController {
    
    @Override
    protected String getModuleName() {
        return ModuleConstants.AUTH_MODULE;
    }

    private static final Logger log = LoggerFactory.getLogger(SessionController.class);
    
    private final SessionService sessionService;
    private final TokenSessionService tokenSessionService;
    
    public SessionController(SessionService sessionService, TokenSessionService tokenSessionService) {
        this.sessionService = sessionService;
        this.tokenSessionService = tokenSessionService;
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
        
        // Refresh session using token service - let exceptions bubble up
        java.util.Optional<String> newTokenOpt = tokenSessionService.refreshToken(request.refreshToken(), ipAddress);
        
        if (newTokenOpt.isEmpty()) {
            throwModuleException(ServiceAuthErrorDetails.REFRESH_TOKEN_INVALID, request.refreshToken());
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
        
        // Validate session using token service - let exceptions bubble up
        boolean isValid = tokenSessionService.validateSession(request.accessToken());
        java.util.Optional<String> userIdOpt = tokenSessionService.getUserIdFromToken(request.accessToken());
        
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
        
        // Revoke session using token service - let exceptions bubble up
        tokenSessionService.deleteSession(request.sessionId());
        
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
        
        // Revoke all sessions using token service - let exceptions bubble up
        boolean deleted = tokenSessionService.deleteUserSessions(userId);
        
        RevokeAllResponse response = new RevokeAllResponse(
            deleted ? 1 : 0, // Simple count - in real implementation would return actual count
            deleted ? "All sessions revoked successfully" : "No sessions found to revoke"
        );
        
        // Return clean response - headers added by StandardHeadersInterceptor
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get current user information from a valid session token
     * 
     * @param request Session validation request containing access token
     * @return Clean user response - no wrapper, let GlobalExceptionHandler handle errors
     */
    @PostMapping("/current-user")
    public ResponseEntity<CurrentUserResponse> getCurrentUser(@Valid @RequestBody SessionValidationRequest request) {
        log.info("Getting current user from session token");
        
        // Validate session and extract user ID using token service
        java.util.Optional<String> userIdOpt = tokenSessionService.getUserIdFromToken(request.accessToken());
        
        if (userIdOpt.isEmpty()) {
            throwModuleException(ServiceAuthErrorDetails.INVALID_TOKEN, "Invalid or expired token");
        }
        
        String userId = userIdOpt.get();
        
        // For now, return basic user info from token
        // TODO: Integrate with user service to get full user profile
        CurrentUserResponse response = new CurrentUserResponse(
            userId,
            userId + "@strategiz.io", // Temporary email format
            "User " + userId.substring(0, Math.min(8, userId.length())), // Temporary name
            System.currentTimeMillis() / 1000 // Current timestamp as created date
        );
        
        // Return clean response - headers added by StandardHeadersInterceptor
        return ResponseEntity.ok(response);
    }

    // === SERVER-SIDE SESSION MANAGEMENT ENDPOINTS (Firestore-based) ===

    /**
     * Validate server-side session using Firestore-based session management
     * 
     * @param httpRequest HTTP servlet request containing session
     * @return Clean validation response with user information
     */
    @PostMapping("/validate-server")
    public ResponseEntity<SessionValidationResponse> validateServerSession(jakarta.servlet.http.HttpServletRequest httpRequest) {
        log.info("Validating Firestore-based server-side session");
        
        java.util.Optional<SessionValidationResult> validationOpt = sessionService.validateSession(httpRequest);
        
        if (validationOpt.isEmpty()) {
            return ResponseEntity.ok(new SessionValidationResponse(false, null, 0L));
        }
        
        SessionValidationResult validation = validationOpt.get();
        SessionValidationResponse response = new SessionValidationResponse(
            validation.isValid(),
            validation.getUserId(),
            validation.getExpiresAt().getEpochSecond()
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get current user information from server-side session (new architecture)
     *
     * @param httpRequest HTTP servlet request containing session
     * @return Clean user response with session data
     */
    @PostMapping("/current-user-server")
    public ResponseEntity<CurrentUserResponse> getCurrentUserFromSession(jakarta.servlet.http.HttpServletRequest httpRequest) {
        log.info("Getting current user from server-side session");

        java.util.Optional<SessionValidationResult> validationOpt = sessionService.validateSession(httpRequest);

        if (validationOpt.isEmpty()) {
            throwModuleException(ServiceAuthErrorDetails.INVALID_TOKEN, "No valid session found");
        }

        SessionValidationResult validation = validationOpt.get();
        CurrentUserResponse response = new CurrentUserResponse(
            validation.getUserId(),
            validation.getUserEmail(),
            "User " + validation.getUserId().substring(0, Math.min(8, validation.getUserId().length())),
            validation.getLastAccessedAt().getEpochSecond()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * BEST PRACTICE: Simple session validation from HTTP-only cookie
     * Returns user data if valid session, 401 if not
     */
    @GetMapping("/validate-cookie")
    public ResponseEntity<CurrentUserResponse> validateSessionCookie(jakarta.servlet.http.HttpServletRequest httpRequest) {
        log.info("Validating session from HTTP-only cookie");

        try {
            java.util.Optional<SessionValidationResult> validationOpt = sessionService.validateSession(httpRequest);

            if (validationOpt.isEmpty()) {
                log.info("No valid session cookie found");
                return ResponseEntity.status(401).build();
            }

            SessionValidationResult validation = validationOpt.get();
            CurrentUserResponse response = new CurrentUserResponse(
                validation.getUserId(),
                validation.getUserEmail(),
                "User " + validation.getUserId().substring(0, Math.min(8, validation.getUserId().length())),
                validation.getLastAccessedAt().getEpochSecond()
            );

            log.info("Valid session found for user: {}", validation.getUserId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error validating session cookie: {}", e.getMessage(), e);
            return ResponseEntity.status(401).build();
        }
    }

    /**
     * BEST PRACTICE: Simple logout endpoint
     * Destroys server session and clears HTTP-only cookie
     */
    @PostMapping("/logout")
    public ResponseEntity<RevocationResponse> logout(
            jakarta.servlet.http.HttpServletRequest httpRequest,
            jakarta.servlet.http.HttpServletResponse httpResponse) {
        log.info("Processing logout request");

        boolean terminated = sessionService.terminateSession(httpRequest, httpResponse);

        RevocationResponse response = new RevocationResponse(
            terminated,
            terminated ? "Logged out successfully" : "No active session"
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Terminate current server-side session (logout)
     * 
     * @param httpRequest HTTP servlet request containing session
     * @param httpResponse HTTP servlet response for clearing cookies
     * @return Clean response indicating success
     */
    @PostMapping("/logout-server")
    public ResponseEntity<RevocationResponse> logoutServerSession(
            jakarta.servlet.http.HttpServletRequest httpRequest,
            jakarta.servlet.http.HttpServletResponse httpResponse) {
        log.info("Terminating server-side session");
        
        boolean terminated = sessionService.terminateSession(httpRequest, httpResponse);
        
        RevocationResponse response = new RevocationResponse(
            terminated,
            terminated ? "Session terminated successfully" : "No session found"
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get active sessions for current user (new architecture)
     * 
     * @param httpRequest HTTP servlet request containing session
     * @return List of active sessions for the user
     */
    @PostMapping("/user-sessions")
    public ResponseEntity<java.util.List<java.util.Map<String, Object>>> getUserSessions(
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        log.info("Getting user active sessions");
        
        java.util.Optional<SessionValidationResult> validationOpt = sessionService.validateSession(httpRequest);
        
        if (validationOpt.isEmpty()) {
            throwModuleException(ServiceAuthErrorDetails.INVALID_TOKEN, "No valid session found");
        }
        
        String userId = validationOpt.get().getUserId();
        java.util.List<io.strategiz.data.session.entity.SessionEntity> sessions = sessionService.getUserActiveSessions(userId);
        
        java.util.List<java.util.Map<String, Object>> sessionList = sessions.stream()
            .map(session -> {
                java.util.Map<String, Object> sessionMap = new java.util.HashMap<>();
                sessionMap.put("sessionId", session.getSessionId());
                sessionMap.put("ipAddress", session.getIpAddress() != null ? session.getIpAddress() : "unknown");
                sessionMap.put("userAgent", "unknown"); // SessionEntity doesn't have userAgent
                sessionMap.put("createdAt", session.getIssuedAt() != null ? session.getIssuedAt().getEpochSecond() : 0);
                sessionMap.put("lastAccessedAt", session.getLastAccessedAt() != null ? session.getLastAccessedAt().getEpochSecond() : 0);
                sessionMap.put("expiresAt", session.getExpiresAt() != null ? session.getExpiresAt().getEpochSecond() : 0);
                return sessionMap;
            })
            .collect(java.util.stream.Collectors.toList());
        
        return ResponseEntity.ok(sessionList);
    }
}
