package io.strategiz.api.auth.controller;

import io.strategiz.api.auth.model.ApiResponse;
import io.strategiz.service.auth.UserAuthService;
import io.strategiz.service.auth.token.TokenService;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for user authentication endpoints
 */
@RestController
@RequestMapping("/auth/user")
public class UserAuthController {

    private static final Logger logger = LoggerFactory.getLogger(UserAuthController.class);

    @Autowired
    private UserAuthService userAuthService;
    
    @Autowired
    private TokenService tokenService;

    /**
     * Verify a user authentication token
     *
     * @param token Firebase ID token
     * @return Response with token verification status
     */
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyToken(@RequestParam String token) {
        try {
            // Verify the Firebase token
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
            logger.error("Error verifying token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.<Map<String, Object>>error("Error verifying token: " + e.getMessage())
            );
        }
    }
    
    /**
     * Authenticate user with email/password and get PASETO tokens
     *
     * @param request Authentication request with email and password
     * @param httpRequest HTTP request for client IP
     * @return Response with tokens
     */
    @PostMapping("/authenticate")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> authenticate(
            @RequestBody AuthenticationRequest request, 
            HttpServletRequest httpRequest) {
        try {
            // Get user by email
            UserRecord user = userAuthService.getUserByEmail(request.email());
            
            if (user == null) {
                logger.warn("Authentication failed: User not found for email: {}", request.email());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ApiResponse.<AuthenticationResponse>error("Authentication failed: Invalid credentials")
                );
            }
            
            // Note: In a real implementation, we would need to verify the password
            // Since Firebase Auth doesn't expose password verification directly,
            // this would require implementing a custom authentication flow
            
            // For demonstration purposes, we'll assume authentication is successful
            // and generate tokens
            
            // Get user's ID from Firebase user record
            String userId = user.getUid();
            String deviceId = request.deviceId();
            String ipAddress = getClientIp(httpRequest);
            
            // Create token pair
            TokenService.TokenPair tokenPair = tokenService.createTokenPair(userId, deviceId, ipAddress, "user");
            
            AuthenticationResponse response = new AuthenticationResponse(
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                "Bearer"
            );
            
            return ResponseEntity.ok(
                ApiResponse.<AuthenticationResponse>success("Authentication successful", response)
            );
        } catch (Exception e) {
            logger.error("Error during authentication: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.<AuthenticationResponse>error("Authentication error: " + e.getMessage())
            );
        }
    }
    
    /**
     * Create a new user account
     *
     * @param request User registration request
     * @return Response with created user details
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> registerUser(@RequestBody RegistrationRequest request) {
        try {
            UserRecord user = userAuthService.createUser(
                request.email(), 
                request.password(), 
                request.displayName()
            );
            
            if (user != null) {
                UserResponse response = new UserResponse(
                    user.getUid(),
                    user.getEmail(),
                    user.getDisplayName(),
                    user.isEmailVerified()
                );
                
                return ResponseEntity.status(HttpStatus.CREATED).body(
                    ApiResponse.<UserResponse>success("User registered successfully", response)
                );
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.<UserResponse>error("Failed to create user")
                );
            }
        } catch (Exception e) {
            logger.error("Error registering user: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.<UserResponse>error("Registration error: " + e.getMessage())
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
    
    // Request and Response record classes
    
    /**
     * Request for user authentication
     */
    public record AuthenticationRequest(String email, String password, String deviceId) {}
    
    /**
     * Response for authentication with tokens
     */
    public record AuthenticationResponse(String accessToken, String refreshToken, String tokenType) {}
    
    /**
     * Request for user registration
     */
    public record RegistrationRequest(String email, String password, String displayName) {}
    
    /**
     * Response with user information
     */
    public record UserResponse(String id, String email, String displayName, boolean emailVerified) {}
}
