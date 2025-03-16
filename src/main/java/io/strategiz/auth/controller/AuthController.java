package io.strategiz.auth.controller;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import io.strategiz.auth.service.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for handling authentication-related endpoints
 */
@RestController
@RequestMapping("/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private SessionService sessionService;

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
     *
     * @param authHeader The Authorization header containing the Bearer token
     * @return A response containing the session ID
     */
    @PostMapping("/session")
    public ResponseEntity<Map<String, Object>> createSession(
            @RequestHeader(name = "Authorization", required = false) String authHeader) {
        
        Map<String, Object> response = new HashMap<>();
        
        // Check if Authorization header is present and has the correct format
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("Missing or invalid Authorization header in create-session request");
            response.put("success", false);
            response.put("message", "Missing or invalid Authorization header");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        // Extract the token from the Authorization header
        String token = authHeader.substring(7); // Remove "Bearer " prefix
        
        try {
            // Verify the token with Firebase Auth
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
            String userId = decodedToken.getUid();
            
            // Create a new session
            String sessionId = sessionService.createSession(userId, token);
            
            if (sessionId == null) {
                logger.error("Failed to create session for user: {}", userId);
                response.put("success", false);
                response.put("message", "Failed to create session");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            
            // Session created successfully
            logger.info("Session created successfully for user: {}", userId);
            response.put("success", true);
            response.put("sessionId", sessionId);
            response.put("userId", userId);
            response.put("email", decodedToken.getEmail());
            
            return ResponseEntity.ok(response);
            
        } catch (FirebaseAuthException e) {
            // Token is invalid or expired
            logger.error("Invalid or expired token in create-session: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "Invalid or expired token: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }
    
    /**
     * Validates a session
     *
     * @param sessionId The session ID
     * @return A response indicating if the session is valid
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> validateSession(
            @PathVariable String sessionId) {
        
        Map<String, Object> response = new HashMap<>();
        
        String userId = sessionService.validateSession(sessionId);
        
        if (userId == null) {
            logger.warn("Invalid or expired session: {}", sessionId);
            response.put("valid", false);
            response.put("message", "Invalid or expired session");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        // Session is valid
        logger.info("Session validated successfully for user: {}", userId);
        response.put("valid", true);
        response.put("userId", userId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Refreshes a session with a new token
     *
     * @param sessionId The session ID
     * @param authHeader The Authorization header containing the Bearer token
     * @return A response indicating if the session was refreshed
     */
    @PutMapping("/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> refreshSession(
            @PathVariable String sessionId,
            @RequestHeader(name = "Authorization", required = false) String authHeader) {
        
        Map<String, Object> response = new HashMap<>();
        
        // Check if Authorization header is present and has the correct format
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("Missing or invalid Authorization header in refresh-session request");
            response.put("success", false);
            response.put("message", "Missing or invalid Authorization header");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        // Extract the token from the Authorization header
        String token = authHeader.substring(7); // Remove "Bearer " prefix
        
        boolean refreshed = sessionService.refreshSession(sessionId, token);
        
        if (!refreshed) {
            logger.error("Failed to refresh session: {}", sessionId);
            response.put("success", false);
            response.put("message", "Failed to refresh session");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        
        // Session refreshed successfully
        logger.info("Session refreshed successfully: {}", sessionId);
        response.put("success", true);
        response.put("message", "Session refreshed successfully");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Invalidates a session
     *
     * @param sessionId The session ID
     * @return A response indicating if the session was invalidated
     */
    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> invalidateSession(
            @PathVariable String sessionId) {
        
        Map<String, Object> response = new HashMap<>();
        
        boolean invalidated = sessionService.invalidateSession(sessionId);
        
        if (!invalidated) {
            logger.error("Failed to invalidate session: {}", sessionId);
            response.put("success", false);
            response.put("message", "Failed to invalidate session");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        
        // Session invalidated successfully
        logger.info("Session invalidated successfully: {}", sessionId);
        response.put("success", true);
        response.put("message", "Session invalidated successfully");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Invalidates all sessions for a user
     *
     * @param userId The user ID
     * @return A response indicating the number of sessions invalidated
     */
    @DeleteMapping("/sessions/user/{userId}")
    public ResponseEntity<Map<String, Object>> invalidateAllUserSessions(
            @PathVariable String userId,
            @RequestHeader(name = "Authorization", required = false) String authHeader) {
        
        Map<String, Object> response = new HashMap<>();
        
        // Check if Authorization header is present and has the correct format
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("Missing or invalid Authorization header in invalidate-all-user-sessions request");
            response.put("success", false);
            response.put("message", "Missing or invalid Authorization header");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        // Extract the token from the Authorization header
        String token = authHeader.substring(7); // Remove "Bearer " prefix
        
        try {
            // Verify the token with Firebase Auth
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
            
            // Only allow users to invalidate their own sessions or admins to invalidate any session
            if (!decodedToken.getUid().equals(userId) && !Boolean.TRUE.equals(decodedToken.getClaims().get("admin"))) {
                logger.error("Unauthorized to invalidate sessions for user: {}", userId);
                response.put("success", false);
                response.put("message", "Unauthorized to invalidate sessions for this user");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            int count = sessionService.invalidateAllUserSessions(userId);
            
            // Sessions invalidated successfully
            logger.info("Invalidated {} sessions for user: {}", count, userId);
            response.put("success", true);
            response.put("message", count + " sessions invalidated successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (FirebaseAuthException e) {
            // Token is invalid or expired
            logger.error("Invalid or expired token: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "Invalid or expired token: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }
}
