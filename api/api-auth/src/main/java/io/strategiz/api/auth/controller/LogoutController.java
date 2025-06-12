package io.strategiz.api.auth.controller;

import com.google.firebase.auth.FirebaseToken;
import io.strategiz.api.auth.model.ApiResponse;
import io.strategiz.service.auth.SessionService;
import io.strategiz.service.auth.UserAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for handling user logout operations
 */
@RestController
@RequestMapping("/auth")
public class LogoutController {

    private static final Logger logger = LoggerFactory.getLogger(LogoutController.class);

    @Autowired
    private UserAuthService userAuthService;

    @Autowired
    private SessionService sessionService;

    /**
     * Sign out a user by deleting all their sessions
     *
     * @param token User authentication token
     * @return Response with sign-out status
     */
    @PostMapping("/signout")
    public ResponseEntity<ApiResponse<Boolean>> signOut(@RequestParam String token) {
        try {
            // Verify the user authentication token
            FirebaseToken decodedToken = userAuthService.verifyIdToken(token);
            
            if (decodedToken != null) {
                String userId = decodedToken.getUid();
                boolean loggedOut = sessionService.deleteUserSessions(userId);
                
                if (loggedOut) {
                    logger.info("User signed out successfully: {}", userId);
                    return ResponseEntity.ok(
                        ApiResponse.<Boolean>success("User signed out successfully", true)
                    );
                } else {
                    logger.warn("Failed to sign out user: {}", userId);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        ApiResponse.<Boolean>error("Failed to sign out user", false)
                    );
                }
            } else {
                logger.warn("Invalid token provided for sign-out");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ApiResponse.<Boolean>error("Invalid token", false)
                );
            }
        } catch (Exception e) {
            logger.error("Error signing out user: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.<Boolean>error("Error signing out user: " + e.getMessage(), false)
            );
        }
    }
}
