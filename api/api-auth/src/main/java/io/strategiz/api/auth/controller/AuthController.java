package io.strategiz.api.auth.controller;

import io.strategiz.api.auth.model.ApiResponse;
import io.strategiz.data.auth.Session;
import io.strategiz.service.auth.UserAuthService;
import io.strategiz.service.auth.SessionService;
import com.google.firebase.auth.FirebaseToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for authentication-related endpoints
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserAuthService userAuthService;

    @Autowired
    private SessionService sessionService;

    /**
     * Verify a user authentication token
     *
     * @param token Authentication token
     * @return Response with token verification status
     */
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyToken(@RequestParam String token) {
        try {
            // Verify the user authentication token
            FirebaseToken decodedToken = userAuthService.verifyIdToken(token);
            
            if (decodedToken != null) {
                Map<String, Object> claims = decodedToken.getClaims();
                
                return ResponseEntity.ok(
                    ApiResponse.<Map<String, Object>>success("Token verified successfully", claims)
                );
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ApiResponse.<Map<String, Object>>error("Invalid token")
                );
            }
        } catch (Exception e) {
            logger.error("Error processing request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.<Map<String, Object>>error("Error processing request: " + e.getMessage())
            );
        }
    }

    /**
     * Create a new session for a user after sign-in
     *
     * @param userId User ID
     * @return Response with session details
     */
    @PostMapping("/sessions")
    public ResponseEntity<ApiResponse<Session>> createSession(@RequestParam String userId) {
        try {
            // Create a new session
            Session session = sessionService.createSession(userId);
            
            if (session != null) {
                return ResponseEntity.ok(
                    ApiResponse.<Session>success("Session created successfully", session)
                );
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.<Session>error("Failed to create session")
                );
            }
        } catch (Exception e) {
            logger.error("Error processing request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.<Session>error("Error creating session: " + e.getMessage())
            );
        }
    }

    /**
     * Get all sessions for a user
     *
     * @param userId User ID
     * @return Response with list of sessions
     */
    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<List<Session>>> getUserSessions(@RequestParam String userId) {
        try {
            // Get all sessions for the user
            List<Session> sessions = sessionService.getUserSessions(userId);
            
            return ResponseEntity.ok(
                ApiResponse.<List<Session>>success("Sessions retrieved successfully", sessions)
            );
        } catch (Exception e) {
            logger.error("Error processing request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.<List<Session>>error("Error retrieving sessions: " + e.getMessage())
            );
        }
    }

    /**
     * Delete a specific session
     *
     * @param sessionId Session ID
     * @return Response with deletion status
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<Boolean>> deleteSession(@PathVariable String sessionId) {
        try {
            // Delete the session
            boolean deleted = sessionService.deleteSession(sessionId);
            
            if (deleted) {
                return ResponseEntity.ok(
                    ApiResponse.<Boolean>success("Session deleted successfully", true)
                );
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.<Boolean>error("Session not found or could not be deleted", false)
                );
            }
        } catch (Exception e) {
            logger.error("Error processing request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.<Boolean>error("Error deleting session: " + e.getMessage())
            );
        }
    }

    /**
     * Logout a user by deleting all their sessions
     *
     * @param token User authentication token
     * @return Response with logout status
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Boolean>> logout(@RequestParam String token) {
        try {
            // Verify the user authentication token
            FirebaseToken decodedToken = userAuthService.verifyIdToken(token);
            
            if (decodedToken != null) {
                String userId = decodedToken.getUid();
                boolean loggedOut = sessionService.deleteUserSessions(userId);
                
                if (loggedOut) {
                    return ResponseEntity.ok(
                        ApiResponse.<Boolean>success("User logged out successfully", true)
                    );
                } else {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        ApiResponse.<Boolean>error("Failed to log out user", false)
                    );
                }
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ApiResponse.<Boolean>error("Invalid token", false)
                );
            }
        } catch (Exception e) {
            logger.error("Error processing request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.<Boolean>error("Error logging out user: " + e.getMessage(), false)
            );
        }
    }
}
