package io.strategiz.service.auth.controller.session;

import io.strategiz.business.tokenauth.SessionAuthBusiness;
import io.strategiz.service.auth.service.session.SessionService;
import io.strategiz.service.auth.model.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Controller for handling user sign out operations
 */
@RestController
@RequestMapping("/auth")
public class SignOutController {

    private static final Logger logger = LoggerFactory.getLogger(SignOutController.class);

    private final SessionService sessionService;
    private final SessionAuthBusiness sessionAuthBusiness;
    
    public SignOutController(SessionService sessionService, SessionAuthBusiness sessionAuthBusiness) {
        this.sessionService = sessionService;
        this.sessionAuthBusiness = sessionAuthBusiness;
    }

    /**
     * Sign out a user by deleting all their sessions
     *
     * @param authHeader Authorization header containing the token
     * @return Response with sign-out status
     */
    @PostMapping("/sign-out")
    public ResponseEntity<ApiResponse<SignOutResponse>> signOut(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                logger.warn("Invalid authorization header for sign-out");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ApiResponse.<SignOutResponse>error("Invalid authorization token")
                );
            }
            
            String token = authHeader.substring(7); // Remove "Bearer " prefix
            Optional<String> userIdOpt = sessionAuthBusiness.validateAccessToken(token);
            
            if (userIdOpt.isPresent()) {
                String userId = userIdOpt.get();
                
                // Revoke the token
                boolean tokenRevoked = sessionAuthBusiness.revokeToken(token);
                
                // Also clean up any session data
                boolean sessionsDeleted = sessionService.deleteUserSessions(userId);
                
                // Use both token and session deletion status for the response
                SignOutResponse response = new SignOutResponse(tokenRevoked && sessionsDeleted);
                logger.info("User signed out successfully: {}", userId);
                return ResponseEntity.ok(
                    ApiResponse.<SignOutResponse>success("User signed out successfully", response)
                );
            } else {
                logger.warn("Invalid token provided for sign-out");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ApiResponse.<SignOutResponse>error("Invalid authentication token")
                );
            }
        } catch (Exception e) {
            logger.error("Error signing out user: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.<SignOutResponse>error("Error signing out user: " + e.getMessage())
            );
        }
    }
    
    /**
     * Response for sign-out operation
     */
    public record SignOutResponse(boolean success) {}
}
