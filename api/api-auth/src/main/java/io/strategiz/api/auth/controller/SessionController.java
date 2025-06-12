package io.strategiz.api.auth.controller;

import io.strategiz.api.auth.model.ApiResponse;
import io.strategiz.data.auth.Session;
import io.strategiz.service.auth.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for session management endpoints
 */
@RestController
@RequestMapping("/auth/sessions")
public class SessionController {

    private static final Logger logger = LoggerFactory.getLogger(SessionController.class);

    @Autowired
    private SessionService sessionService;

    /**
     * Create a new session for a user after sign-in
     *
     * @param userId User ID
     * @return Response with session details
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Session>> createSession(@RequestParam String userId) {
        try {
            // Create a new session
            Session session = sessionService.createSession(userId);
            
            if (session != null) {
                logger.info("Session created for user: {}", userId);
                return ResponseEntity.ok(
                    ApiResponse.<Session>success("Session created successfully", session)
                );
            } else {
                logger.warn("Failed to create session for user: {}", userId);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.<Session>error("Failed to create session")
                );
            }
        } catch (Exception e) {
            logger.error("Error creating session: {}", e.getMessage());
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
    @GetMapping
    public ResponseEntity<ApiResponse<List<Session>>> getUserSessions(@RequestParam String userId) {
        try {
            // Get all sessions for the user
            List<Session> sessions = sessionService.getUserSessions(userId);
            
            logger.info("Retrieved {} sessions for user: {}", sessions.size(), userId);
            return ResponseEntity.ok(
                ApiResponse.<List<Session>>success("Sessions retrieved successfully", sessions)
            );
        } catch (Exception e) {
            logger.error("Error retrieving sessions: {}", e.getMessage());
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
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<ApiResponse<Boolean>> deleteSession(@PathVariable String sessionId) {
        try {
            // Delete the session
            boolean deleted = sessionService.deleteSession(sessionId);
            
            if (deleted) {
                logger.info("Session deleted: {}", sessionId);
                return ResponseEntity.ok(
                    ApiResponse.<Boolean>success("Session deleted successfully", true)
                );
            } else {
                logger.warn("Session not found: {}", sessionId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.<Boolean>error("Session not found or could not be deleted", false)
                );
            }
        } catch (Exception e) {
            logger.error("Error deleting session: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.<Boolean>error("Error deleting session: " + e.getMessage())
            );
        }
    }
    
    /**
     * Delete all sessions for a user
     *
     * @param userId User ID
     * @return Response with deletion status
     */
    @DeleteMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<Boolean>> deleteUserSessions(@PathVariable String userId) {
        try {
            // Delete all sessions for the user
            boolean loggedOut = sessionService.deleteUserSessions(userId);
            
            if (loggedOut) {
                logger.info("All sessions deleted for user: {}", userId);
                return ResponseEntity.ok(
                    ApiResponse.<Boolean>success("All sessions deleted successfully", true)
                );
            } else {
                logger.warn("Failed to delete sessions for user: {}", userId);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.<Boolean>error("Failed to delete sessions", false)
                );
            }
        } catch (Exception e) {
            logger.error("Error deleting sessions: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.<Boolean>error("Error deleting sessions: " + e.getMessage(), false)
            );
        }
    }
}
