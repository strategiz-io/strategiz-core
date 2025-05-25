package io.strategiz.api.auth;

import com.google.firebase.auth.FirebaseToken;
import io.strategiz.data.auth.Session;
import io.strategiz.service.auth.FirebaseAuthService;
import io.strategiz.service.auth.SessionService;
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

/**
 * Controller for handling authentication-related endpoints
 */
@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080", "https://strategiz.io"})
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final SessionService sessionService;
    private final FirebaseAuthService firebaseAuthService;

    @Autowired
    public AuthController(SessionService sessionService, FirebaseAuthService firebaseAuthService) {
        this.sessionService = sessionService;
        this.firebaseAuthService = firebaseAuthService;
    }

    /**
     * Verifies if a Firebase token is valid
     * This endpoint is used by the frontend to check if a stored token is still valid
     * without needing to make a full API request
     *
     * @param authHeader The Authorization header containing the Bearer token
     * @return A response indicating if the token is valid
     */
    @GetMapping("/verify-token")
    public ResponseEntity<ApiResponse> verifyToken(
            @RequestHeader(name = "Authorization", required = false) String authHeader) {
        
        try {
            logger.info("Verifying token");
            
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                logger.warn("No token provided or invalid format");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("No token provided or invalid format"));
            }
            
            String token = authHeader.substring(7); // Remove "Bearer " prefix
            
            // Verify the Firebase token
            FirebaseToken decodedToken = firebaseAuthService.verifyIdToken(token);
            
            if (decodedToken == null) {
                logger.error("Token verification failed");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Token is invalid"));
            }
            
            String userId = decodedToken.getUid();
            logger.info("Token verified for user: {}", userId);
            
            Map<String, Object> userData = firebaseAuthService.getUserDataFromToken(decodedToken);
            return ResponseEntity.ok(ApiResponse.success("Token is valid", userData));
        } catch (Exception e) {
            logger.error("Error processing request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error processing request: " + e.getMessage()));
        }
    }
    
    /**
     * Creates a new session for a user
     *
     * @param authHeader The Authorization header containing the Bearer token
     * @return A response containing the session details
     */
    @PostMapping("/sessions")
    public ResponseEntity<ApiResponse> createSession(
            @RequestHeader(name = "Authorization", required = false) String authHeader) {
        
        try {
            logger.info("Creating new session");
            
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                logger.warn("No token provided or invalid format");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("No token provided or invalid format"));
            }
            
            String token = authHeader.substring(7); // Remove "Bearer " prefix
            
            // Verify the Firebase token
            FirebaseToken decodedToken = firebaseAuthService.verifyIdToken(token);
            
            if (decodedToken == null) {
                logger.error("Token verification failed");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Token is invalid"));
            }
            
            String userId = decodedToken.getUid();
            
            // Create a new session
            Session session = sessionService.createSession(userId);
            
            logger.info("Session created for user: {}", userId);
            
            Map<String, Object> sessionData = new HashMap<>();
            sessionData.put("sessionId", session.getId());
            sessionData.put("token", session.getToken());
            sessionData.put("expiresAt", session.getExpiresAt());
            
            return ResponseEntity.ok(ApiResponse.success("Session created", sessionData));
        } catch (Exception e) {
            logger.error("Error processing request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error processing request: " + e.getMessage()));
        }
    }
    
    /**
     * Validates a session token
     *
     * @param sessionToken The session token to validate
     * @return A response indicating if the session is valid
     */
    @GetMapping("/sessions/validate")
    public ResponseEntity<ApiResponse> validateSession(
            @RequestParam String sessionToken) {
        
        logger.info("Validating session token");
        
        Optional<Session> sessionOpt = sessionService.validateSession(sessionToken);
        
        if (sessionOpt.isEmpty()) {
            logger.warn("Invalid or expired session token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid or expired session token"));
        }
        
        Session session = sessionOpt.get();
        logger.info("Session validated for user: {}", session.getUserId());
        
        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("sessionId", session.getId());
        sessionData.put("userId", session.getUserId());
        sessionData.put("expiresAt", session.getExpiresAt());
        
        return ResponseEntity.ok(ApiResponse.success("Session is valid", sessionData));
    }
    
    /**
     * Gets all sessions for a user
     *
     * @param authHeader The Authorization header containing the Bearer token
     * @return A response containing the user's sessions
     */
    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse> getUserSessions(
            @RequestHeader(name = "Authorization", required = false) String authHeader) {
        
        try {
            logger.info("Getting user sessions");
            
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                logger.warn("No token provided or invalid format");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("No token provided or invalid format"));
            }
            
            String token = authHeader.substring(7); // Remove "Bearer " prefix
            
            // Verify the Firebase token
            FirebaseToken decodedToken = firebaseAuthService.verifyIdToken(token);
            
            if (decodedToken == null) {
                logger.error("Token verification failed");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Token is invalid"));
            }
            
            String userId = decodedToken.getUid();
            
            // Get all sessions for the user
            List<Session> sessions = sessionService.getUserSessions(userId);
            
            logger.info("Found {} sessions for user: {}", sessions.size(), userId);
            
            Map<String, Object> sessionsData = new HashMap<>();
            sessionsData.put("count", sessions.size());
            sessionsData.put("sessions", sessions);
            
            return ResponseEntity.ok(ApiResponse.success("User sessions retrieved", sessionsData));
        } catch (Exception e) {
            logger.error("Error processing request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error processing request: " + e.getMessage()));
        }
    }
    
    /**
     * Deletes a session
     *
     * @param sessionId The ID of the session to delete
     * @param authHeader The Authorization header containing the Bearer token
     * @return A response indicating if the session was deleted
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse> deleteSession(
            @PathVariable String sessionId,
            @RequestHeader(name = "Authorization", required = false) String authHeader) {
        
        try {
            logger.info("Deleting session: {}", sessionId);
            
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                logger.warn("No token provided or invalid format");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("No token provided or invalid format"));
            }
            
            String token = authHeader.substring(7); // Remove "Bearer " prefix
            
            // Verify the Firebase token
            FirebaseToken decodedToken = firebaseAuthService.verifyIdToken(token);
            
            if (decodedToken == null) {
                logger.error("Token verification failed");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Token is invalid"));
            }
            
            // Delete the session
            boolean deleted = sessionService.deleteSession(sessionId);
            
            if (deleted) {
                logger.info("Session deleted: {}", sessionId);
                return ResponseEntity.ok(ApiResponse.success("Session deleted"));
            } else {
                logger.warn("Session not found or could not be deleted: {}", sessionId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Session not found or could not be deleted"));
            }
        } catch (Exception e) {
            logger.error("Error processing request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error processing request: " + e.getMessage()));
        }
    }
    
    /**
     * Logs out a user by deleting all their sessions
     *
     * @param authHeader The Authorization header containing the Bearer token
     * @return A response indicating if the logout was successful
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(
            @RequestHeader(name = "Authorization", required = false) String authHeader) {
        
        try {
            logger.info("Logging out user");
            
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                logger.warn("No token provided or invalid format");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("No token provided or invalid format"));
            }
            
            String token = authHeader.substring(7); // Remove "Bearer " prefix
            
            // Verify the Firebase token
            FirebaseToken decodedToken = firebaseAuthService.verifyIdToken(token);
            
            if (decodedToken == null) {
                logger.error("Token verification failed");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Token is invalid"));
            }
            
            String userId = decodedToken.getUid();
            
            // Delete all sessions for the user
            boolean deleted = sessionService.deleteUserSessions(userId);
            
            if (deleted) {
                logger.info("All sessions deleted for user: {}", userId);
                return ResponseEntity.ok(ApiResponse.success("Logout successful"));
            } else {
                logger.warn("Failed to delete all sessions for user: {}", userId);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.error("Failed to delete all sessions"));
            }
        } catch (Exception e) {
            logger.error("Error processing request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error processing request: " + e.getMessage()));
        }
    }
}
