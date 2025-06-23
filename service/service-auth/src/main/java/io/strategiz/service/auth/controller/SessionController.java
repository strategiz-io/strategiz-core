package io.strategiz.service.auth.controller;

import io.strategiz.business.tokenauth.SessionAuthBusiness;
import io.strategiz.service.auth.model.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Controller for session management operations
 * This controller handles session-specific operations like refresh, validation, and revocation
 * It's separate from authentication controllers to maintain clean separation of concerns
 */
@RestController
@RequestMapping("/auth/sessions")
public class SessionController {
    
    private static final Logger logger = LoggerFactory.getLogger(SessionController.class);
    private final SessionAuthBusiness sessionAuthBusiness;
    
    public SessionController(SessionAuthBusiness sessionAuthBusiness) {
        this.sessionAuthBusiness = sessionAuthBusiness;
    }
    
    /**
     * Refresh access token using a refresh token
     * 
     * @param request Refresh token request
     * @param httpRequest HTTP request to extract client IP
     * @return New access token
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshSessionResponse>> refreshSession(
            @RequestBody RefreshSessionRequest request,
            HttpServletRequest httpRequest) {
        
        logger.info("Refreshing session");
        
        try {
            Optional<String> newAccessTokenOpt = sessionAuthBusiness.refreshAccessToken(
                    request.refreshToken(),
                    getClientIp(httpRequest));
            
            if (newAccessTokenOpt.isPresent()) {
                logger.info("Session refresh successful");
                RefreshSessionResponse response = new RefreshSessionResponse(newAccessTokenOpt.get());
                return ResponseEntity.ok(
                    ApiResponse.<RefreshSessionResponse>success("Session refreshed successfully", response)
                );
            } else {
                logger.warn("Session refresh failed - invalid refresh token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ApiResponse.<RefreshSessionResponse>error("Invalid refresh token")
                );
            }
        } catch (Exception e) {
            logger.error("Error during session refresh: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.<RefreshSessionResponse>error("Session refresh error: " + e.getMessage())
            );
        }
    }
    
    /**
     * Validate an access token
     * 
     * @param request Token validation request
     * @return Validation result with user ID if valid
     */
    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<SessionValidationResponse>> validateSession(
            @RequestBody SessionValidationRequest request) {
        
        logger.info("Validating session");
        
        try {
            Optional<String> userIdOpt = sessionAuthBusiness.validateAccessToken(request.accessToken());
            
            if (userIdOpt.isPresent()) {
                String userId = userIdOpt.get();
                logger.info("Session validation successful for user: {}", userId);
                SessionValidationResponse response = new SessionValidationResponse(true, userId);
                return ResponseEntity.ok(
                    ApiResponse.<SessionValidationResponse>success("Session is valid", response)
                );
            } else {
                logger.warn("Session validation failed - invalid token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ApiResponse.<SessionValidationResponse>error("Invalid token")
                );
            }
        } catch (Exception e) {
            logger.error("Error during session validation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.<SessionValidationResponse>error("Session validation error: " + e.getMessage())
            );
        }
    }
    
    /**
     * Revoke a token (can be either access or refresh token)
     * 
     * @param request Token revocation request
     * @return Revocation status
     */
    @PostMapping("/revoke")
    public ResponseEntity<ApiResponse<RevocationResponse>> revokeSession(
            @RequestBody SessionRevocationRequest request) {
        
        logger.info("Revoking session");
        
        try {
            boolean revoked = sessionAuthBusiness.revokeToken(request.token());
            
            RevocationResponse response = new RevocationResponse(revoked);
            if (revoked) {
                logger.info("Session revocation successful");
                return ResponseEntity.ok(
                    ApiResponse.<RevocationResponse>success("Session revoked successfully", response)
                );
            } else {
                logger.warn("Session revocation failed - token not found or already revoked");
                return ResponseEntity.ok(
                    ApiResponse.<RevocationResponse>success("Session not found or already revoked", response)
                );
            }
        } catch (Exception e) {
            logger.error("Error during session revocation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.<RevocationResponse>error("Session revocation error: " + e.getMessage())
            );
        }
    }
    
    /**
     * Revoke all sessions for a user
     * 
     * @param userId User ID
     * @return Number of sessions revoked
     */
    @PostMapping("/revoke-all/{userId}")
    public ResponseEntity<ApiResponse<RevokeAllResponse>> revokeAllSessions(@PathVariable String userId) {
        
        logger.info("Revoking all sessions for user: {}", userId);
        
        try {
            int count = sessionAuthBusiness.revokeAllUserTokens(userId);
            logger.info("Revoked {} sessions for user: {}", count, userId);
            RevokeAllResponse response = new RevokeAllResponse(count);
            return ResponseEntity.ok(
                ApiResponse.<RevokeAllResponse>success("All sessions revoked for user", response)
            );
        } catch (Exception e) {
            logger.error("Error during session revocation for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.<RevokeAllResponse>error("Session revocation error: " + e.getMessage())
            );
        }
    }
    
    /**
     * Extract client IP address from request
     * 
     * @param request HTTP request
     * @return Client IP address
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Return the first IP in X-Forwarded-For header (client IP)
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
    
    // Request and response record classes
    
    /**
     * Request to refresh session
     */
    public record RefreshSessionRequest(String refreshToken) {}
    
    /**
     * Response for session refresh
     */
    public record RefreshSessionResponse(String accessToken) {}
    
    /**
     * Request for session validation
     */
    public record SessionValidationRequest(String accessToken) {}
    
    /**
     * Response for session validation
     */
    public record SessionValidationResponse(boolean valid, String userId) {}
    
    /**
     * Request for session revocation
     */
    public record SessionRevocationRequest(String token) {}
    
    /**
     * Response for session revocation
     */
    public record RevocationResponse(boolean revoked) {}
    
    /**
     * Response for revoking all sessions
     */
    public record RevokeAllResponse(int count) {}
}
