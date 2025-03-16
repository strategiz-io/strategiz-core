package io.strategiz.auth.controller;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import io.strategiz.auth.model.Session;
import io.strategiz.auth.repository.SessionRepository;
import io.strategiz.auth.response.ApiResponse;
import io.strategiz.auth.service.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.lang.InterruptedException;

/**
 * Controller for handling authentication-related endpoints
 */
@RestController
@RequestMapping("/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private SessionService sessionService;

    @Autowired
    private SessionRepository sessionRepository;

    /**
     * Verifies if a Firebase token is valid
     * This endpoint is used by the frontend to check if a stored token is still valid
     * without needing to make a full API request
     *
     * @param authHeader The Authorization header containing the Bearer token
     * @return A response indicating if the token is valid
     */
    @GetMapping("/verify-token")
    public ResponseEntity<Map<String, Object>> verifyToken(
            @RequestHeader(name = "Authorization", required = false) String authHeader) {
        
        Map<String, Object> response = new HashMap<>();
        
        // Check if Authorization header is present and has the correct format
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("Missing or invalid Authorization header in verify-token request");
            response.put("valid", false);
            response.put("message", "Missing or invalid Authorization header");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        // Extract the token from the Authorization header
        String token = authHeader.substring(7); // Remove "Bearer " prefix
        
        try {
            // Verify the token with Firebase Auth
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
            
            // Token is valid, return success response
            logger.info("Token verified successfully for user: {}", decodedToken.getUid());
            response.put("valid", true);
            response.put("uid", decodedToken.getUid());
            response.put("email", decodedToken.getEmail());
            
            return ResponseEntity.ok(response);
            
        } catch (FirebaseAuthException e) {
            // Token is invalid or expired
            logger.error("Invalid or expired token: {}", e.getMessage());
            response.put("valid", false);
            response.put("message", "Invalid or expired token: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }
    
    /**
     * Creates a new session for a user
     * @param request The request containing the Firebase ID token
     * @return A response with the session ID
     */
    @PostMapping("/session")
    public ResponseEntity<ApiResponse<Map<String, String>>> createSession(@RequestBody Map<String, String> request) {
        String token = request.get("token");

        if (token == null || token.isEmpty()) {
            logger.error("Failed to create session: token is null or empty");
            return ResponseEntity.badRequest().body(
                    ApiResponse.<Map<String, String>>builder()
                            .success(false)
                            .message("token is required")
                            .build()
            );
        }

        try {
            logger.info("Creating session with token");
            String sessionId = sessionService.createSession(token);
            
            if (sessionId == null) {
                logger.error("Failed to create session: sessionId is null");
                return ResponseEntity.internalServerError().body(
                        ApiResponse.<Map<String, String>>builder()
                                .success(false)
                                .message("Failed to create session")
                                .build()
                );
            }
            
            return ResponseEntity.ok(
                    ApiResponse.<Map<String, String>>builder()
                            .success(true)
                            .data(Map.of("sessionId", sessionId))
                            .build()
            );
        } catch (Exception e) {
            logger.error("Failed to create session", e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.<Map<String, String>>builder()
                            .success(false)
                            .message("Failed to create session: " + e.getMessage())
                            .build()
            );
        }
    }
    
    /**
     * Validates a session
     *
     * @param sessionId The session ID
     * @return A response indicating if the session is valid
     */
    @GetMapping("/validate-session/{sessionId}")
    public ResponseEntity<Map<String, Object>> validateSession(
            @PathVariable String sessionId) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("Validating session: {}", sessionId);
            
            Optional<Session> optionalSession = sessionService.validateSession(sessionId);
            
            if (optionalSession.isPresent()) {
                Session session = optionalSession.get();
                logger.info("Session validated successfully: {}", sessionId);
                response.put("success", true);
                response.put("userId", session.getUserId());
                response.put("expiresAt", session.getExpiresAt());
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Session validation failed: {}", sessionId);
                response.put("success", false);
                response.put("message", "Invalid session");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
        } catch (Exception e) {
            logger.error("Failed to validate session: {}", sessionId, e);
            response.put("success", false);
            response.put("message", "Failed to validate session: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Refreshes a session with a new token
     *
     * @param request The request containing the session ID
     * @return A response indicating if the session was refreshed
     */
    @PostMapping("/refresh-session")
    public ResponseEntity<Map<String, Object>> refreshSession(
            @RequestBody Map<String, String> request) {
        
        Map<String, Object> response = new HashMap<>();
        
        String sessionId = request.get("sessionId");
        
        if (sessionId == null) {
            logger.warn("Missing sessionId in refresh request");
            response.put("success", false);
            response.put("message", "sessionId is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        
        try {
            logger.info("Refreshing session: {}", sessionId);
            
            // Refresh the session
            Session refreshedSession = sessionService.refreshSession(sessionId);
            
            if (refreshedSession != null) {
                logger.info("Session refreshed successfully: {}", sessionId);
                response.put("success", true);
                response.put("message", "Session refreshed successfully");
                response.put("sessionId", sessionId);
                response.put("userId", refreshedSession.getUserId());
                response.put("expiresAt", refreshedSession.getExpiresAt());
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Session not found for refresh: {}", sessionId);
                response.put("success", false);
                response.put("message", "Invalid session");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        } catch (InterruptedException e) {
            logger.error("Failed to refresh session: {}", sessionId, e);
            response.put("success", false);
            response.put("message", "Failed to refresh session: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (ExecutionException e) {
            logger.error("Failed to refresh session: {}", sessionId, e);
            response.put("success", false);
            response.put("message", "Failed to refresh session: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Invalidates a session
     * @param sessionId The session ID to invalidate
     * @return A response indicating success or failure
     */
    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> invalidateSession(@PathVariable("sessionId") String sessionId) {
        try {
            logger.info("Invalidating session: {}", sessionId);
            
            // Since we no longer have an invalidateSession method, we'll delete the session directly
            sessionRepository.deleteById(sessionId).get();
            
            return ResponseEntity.ok(
                    ApiResponse.<Void>builder()
                            .success(true)
                            .message("Session invalidated successfully")
                            .build()
            );
        } catch (Exception e) {
            logger.error("Failed to invalidate session: {}", sessionId, e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.<Void>builder()
                            .success(false)
                            .message("Failed to invalidate session: " + e.getMessage())
                            .build()
            );
        }
    }
    
    /**
     * Invalidates all sessions for a user
     * @param userId The user ID
     * @param authHeader The Authorization header containing the Bearer token
     * @return A response indicating success or failure
     */
    @DeleteMapping("/sessions/user/{userId}")
    public ResponseEntity<ApiResponse<Void>> invalidateAllUserSessions(
            @PathVariable("userId") String userId,
            @RequestHeader(name = "Authorization", required = false) String authHeader) {
        try {
            logger.info("Invalidating all sessions for user: {}", userId);
            
            // Check if Authorization header is present and has the correct format
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                logger.warn("Missing or invalid Authorization header in invalidate-all-user-sessions request");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        ApiResponse.<Void>builder()
                                .success(false)
                                .message("Missing or invalid Authorization header")
                                .build()
                );
            }
            
            // Extract the token from the Authorization header
            String token = authHeader.substring(7); // Remove "Bearer " prefix
            
            try {
                // Verify the token with Firebase Auth
                FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
                
                // Only allow users to invalidate their own sessions or admins to invalidate any session
                if (!decodedToken.getUid().equals(userId) && !Boolean.TRUE.equals(decodedToken.getClaims().get("admin"))) {
                    logger.error("Unauthorized to invalidate sessions for user: {}", userId);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                            ApiResponse.<Void>builder()
                                    .success(false)
                                    .message("Unauthorized to invalidate sessions for this user")
                                    .build()
                    );
                }
            } catch (FirebaseAuthException e) {
                // Token is invalid or expired
                logger.error("Invalid or expired token: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        ApiResponse.<Void>builder()
                                .success(false)
                                .message("Invalid or expired token: " + e.getMessage())
                                .build()
                );
            }
            
            // Find all sessions for the user and delete them
            List<Session> userSessions = sessionRepository.findByUserId(userId).get();
            int count = 0;
            
            for (Session session : userSessions) {
                sessionRepository.deleteById(session.getId()).get();
                count++;
            }
            
            return ResponseEntity.ok(
                    ApiResponse.<Void>builder()
                            .success(true)
                            .message("Invalidated " + count + " sessions for user")
                            .build()
            );
        } catch (Exception e) {
            logger.error("Failed to invalidate sessions for user: {}", userId, e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.<Void>builder()
                            .success(false)
                            .message("Failed to invalidate sessions: " + e.getMessage())
                            .build()
            );
        }
    }
    
    // Removed passkey and device session endpoints as they've been moved to dedicated controllers:
    // - PasskeyController: handles /auth/passkey/session
    // - DeviceIdentityController: handles /auth/device/session

}
