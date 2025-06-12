package io.strategiz.api.auth.controller;

import io.strategiz.api.auth.model.ApiResponse;
import io.strategiz.service.auth.token.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Controller for token management operations
 * This controller handles token-specific operations like refresh, validation, and revocation
 * It's separate from authentication controllers to maintain clean separation of concerns
 */
@RestController
@RequestMapping("/auth/tokens")
public class TokenController {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenController.class);
    private final TokenService tokenService;
    
    @Autowired
    public TokenController(TokenService tokenService) {
        this.tokenService = tokenService;
    }
    
    /**
     * Refresh access token using a refresh token
     * 
     * @param request Refresh token request
     * @param httpRequest HTTP request to extract client IP
     * @return New access token
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refreshToken(
            @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {
        
        logger.info("Refreshing token");
        
        try {
            Optional<String> newAccessTokenOpt = tokenService.refreshAccessToken(
                    request.refreshToken(),
                    getClientIp(httpRequest));
            
            if (newAccessTokenOpt.isPresent()) {
                logger.info("Token refresh successful");
                RefreshTokenResponse response = new RefreshTokenResponse(newAccessTokenOpt.get());
                return ResponseEntity.ok(
                    ApiResponse.<RefreshTokenResponse>success("Token refreshed successfully", response)
                );
            } else {
                logger.warn("Token refresh failed - invalid refresh token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ApiResponse.<RefreshTokenResponse>error("Invalid refresh token")
                );
            }
        } catch (Exception e) {
            logger.error("Error during token refresh: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.<RefreshTokenResponse>error("Token refresh error: " + e.getMessage())
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
    public ResponseEntity<ApiResponse<TokenValidationResponse>> validateToken(
            @RequestBody TokenValidationRequest request) {
        
        logger.info("Validating token");
        
        try {
            Optional<String> userIdOpt = tokenService.validateAccessToken(request.accessToken());
            
            if (userIdOpt.isPresent()) {
                String userId = userIdOpt.get();
                logger.info("Token validation successful for user: {}", userId);
                TokenValidationResponse response = new TokenValidationResponse(true, userId);
                return ResponseEntity.ok(
                    ApiResponse.<TokenValidationResponse>success("Token is valid", response)
                );
            } else {
                logger.warn("Token validation failed - invalid token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ApiResponse.<TokenValidationResponse>error("Invalid token")
                );
            }
        } catch (Exception e) {
            logger.error("Error during token validation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.<TokenValidationResponse>error("Token validation error: " + e.getMessage())
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
    public ResponseEntity<ApiResponse<RevocationResponse>> revokeToken(
            @RequestBody TokenRevocationRequest request) {
        
        logger.info("Revoking token");
        
        try {
            boolean revoked = tokenService.revokeToken(request.token());
            
            RevocationResponse response = new RevocationResponse(revoked);
            if (revoked) {
                logger.info("Token revocation successful");
                return ResponseEntity.ok(
                    ApiResponse.<RevocationResponse>success("Token revoked successfully", response)
                );
            } else {
                logger.warn("Token revocation failed - token not found or already revoked");
                return ResponseEntity.ok(
                    ApiResponse.<RevocationResponse>success("Token not found or already revoked", response)
                );
            }
        } catch (Exception e) {
            logger.error("Error during token revocation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.<RevocationResponse>error("Token revocation error: " + e.getMessage())
            );
        }
    }
    
    /**
     * Revoke all tokens for a user
     * 
     * @param userId User ID
     * @return Number of tokens revoked
     */
    @PostMapping("/revoke-all/{userId}")
    public ResponseEntity<ApiResponse<RevokeAllResponse>> revokeAllTokens(@PathVariable String userId) {
        
        logger.info("Revoking all tokens for user: {}", userId);
        
        try {
            int count = tokenService.revokeAllUserTokens(userId);
            logger.info("Revoked {} tokens for user: {}", count, userId);
            RevokeAllResponse response = new RevokeAllResponse(count);
            return ResponseEntity.ok(
                ApiResponse.<RevokeAllResponse>success("All tokens revoked for user", response)
            );
        } catch (Exception e) {
            logger.error("Error during token revocation for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.<RevokeAllResponse>error("Token revocation error: " + e.getMessage())
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
     * Request to refresh token
     */
    public record RefreshTokenRequest(String refreshToken) {}
    
    /**
     * Response for token refresh
     */
    public record RefreshTokenResponse(String accessToken) {}
    
    /**
     * Request for token validation
     */
    public record TokenValidationRequest(String accessToken) {}
    
    /**
     * Response for token validation
     */
    public record TokenValidationResponse(boolean valid, String userId) {}
    
    /**
     * Request for token revocation
     */
    public record TokenRevocationRequest(String token) {}
    
    /**
     * Response for token revocation
     */
    public record RevocationResponse(boolean revoked) {}
    
    /**
     * Response for revoking all tokens
     */
    public record RevokeAllResponse(int count) {}
}
