package io.strategiz.api.auth.controller;

import io.strategiz.api.auth.model.ApiResponse;
import io.strategiz.api.auth.model.totp.*;
import io.strategiz.data.user.model.User;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.service.auth.totp.TotpService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * Controller for Time-based One-Time Password (TOTP) authentication endpoints
 */
@RestController
@RequestMapping("/auth/totp")
public class TotpController {

    private static final Logger log = LoggerFactory.getLogger(TotpController.class);

    @Autowired
    private TotpService totpService;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Initiates TOTP sign-up (setup) for a user
     * 
     * @param request Setup request containing user details
     * @return Response with secret and QR code
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<TotpSetupResponse>> setupTotp(@RequestBody TotpSetupRequest request) {
        log.info("Setting up TOTP for user ID: {}", request.userId());
        
        try {
            // Verify user exists
            Optional<User> userOpt = userRepository.findById(request.userId());
            if (userOpt.isEmpty()) {
                log.warn("User not found for TOTP setup: {}", request.userId());
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("User not found"));
            }
            
            // Check if TOTP is already enabled
            if (totpService.isTotpEnabledForUser(request.userId())) {
                log.warn("TOTP already enabled for user: {}", request.userId());
                return ResponseEntity
                        .status(HttpStatus.CONFLICT)
                        .body(ApiResponse.error("TOTP is already enabled for this user"));
            }
            
            // Generate secret and QR code
            String secret = totpService.generateSecret();
            String email = request.email() != null ? request.email() : userOpt.get().getProfile().getEmail();
            String qrCodeImageUri = totpService.generateQrCodeImageUri(secret, email);
            
            TotpSetupResponse response = TotpSetupResponse.success(secret, qrCodeImageUri);
            return ResponseEntity.ok(ApiResponse.success("TOTP setup successful", response));
            
        } catch (Exception e) {
            log.error("Error setting up TOTP: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error setting up TOTP: " + e.getMessage()));
        }
    }
    
    /**
     * Verifies a TOTP code and completes the sign-up process
     * 
     * @param request Verification request with code and secret
     * @return Response indicating verification result
     */
    @PostMapping("/signup/verify")
    public ResponseEntity<ApiResponse<TotpVerifyResponse>> verifyTotp(@RequestBody TotpVerifyRequest request) {
        log.info("Verifying TOTP setup for user ID: {}", request.userId());
        
        try {
            // Verify the TOTP code first
            if (!totpService.verifyCode(request.secret(), request.code())) {
                log.warn("Invalid TOTP code provided for verification: {}", request.userId());
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Invalid verification code"));
            }
            
            // Enable TOTP for the user
            boolean enabled = totpService.enableTotpForUser(request.userId(), request.secret(), null);
            if (!enabled) {
                log.error("Failed to enable TOTP for user: {}", request.userId());
                return ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.error("Failed to enable TOTP"));
            }
            
            return ResponseEntity.ok(ApiResponse.success("TOTP verification successful", TotpVerifyResponse.createSuccess()));
            
        } catch (Exception e) {
            log.error("Error verifying TOTP: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error verifying TOTP: " + e.getMessage()));
        }
    }
    
    /**
     * Signs in a user with a TOTP code
     * 
     * @param request Authentication request with user ID and code
     * @param httpRequest HTTP request for IP address extraction
     * @return Response with authentication tokens if successful
     */
    @PostMapping("/signin")
    public ResponseEntity<ApiResponse<TotpAuthResponse>> authenticateWithTotp(
            @RequestBody TotpAuthRequest request, HttpServletRequest httpRequest) {
        log.info("Authenticating with TOTP for user ID: {}", request.userId());
        
        try {
            String ipAddress = getClientIp(httpRequest);
            Optional<io.strategiz.service.auth.token.TokenService.TokenPair> tokenPairOpt = 
                    totpService.authenticateWithTotp(request.userId(), request.code(), request.deviceId(), ipAddress);
            
            if (tokenPairOpt.isEmpty()) {
                log.warn("TOTP authentication failed for user: {}", request.userId());
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Authentication failed"));
            }
            
            io.strategiz.service.auth.token.TokenService.TokenPair tokenPair = tokenPairOpt.get();
            TotpAuthResponse response = TotpAuthResponse.success(tokenPair.accessToken(), tokenPair.refreshToken());
            
            return ResponseEntity.ok(ApiResponse.success("TOTP authentication successful", response));
            
        } catch (Exception e) {
            log.error("Error during TOTP authentication: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error during authentication: " + e.getMessage()));
        }
    }
    
    /**
     * Disables TOTP for a user
     * 
     * @param userId User ID
     * @return Response indicating success or failure
     */
    @DeleteMapping("/disable/{userId}")
    public ResponseEntity<ApiResponse<TotpVerifyResponse>> disableTotp(@PathVariable String userId) {
        log.info("Disabling TOTP for user ID: {}", userId);
        
        try {
            // Check if TOTP is currently enabled
            if (!totpService.isTotpEnabledForUser(userId)) {
                log.warn("TOTP not enabled for user: {}", userId);
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("TOTP is not enabled for this user"));
            }
            
            // Disable TOTP
            boolean disabled = totpService.disableTotpForUser(userId);
            if (!disabled) {
                log.error("Failed to disable TOTP for user: {}", userId);
                return ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.error("Failed to disable TOTP"));
            }
            
            return ResponseEntity.ok(ApiResponse.success("TOTP disabled successfully", TotpVerifyResponse.createSuccess()));
            
        } catch (Exception e) {
            log.error("Error disabling TOTP: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error disabling TOTP: " + e.getMessage()));
        }
    }
    
    /**
     * Checks if TOTP is enabled for a user
     * 
     * @param userId User ID
     * @return Response indicating if TOTP is enabled
     */
    @GetMapping("/status/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkTotpStatus(@PathVariable String userId) {
        log.info("Checking TOTP status for user ID: {}", userId);
        
        try {
            boolean isEnabled = totpService.isTotpEnabledForUser(userId);
            Map<String, Boolean> status = Map.of("enabled", isEnabled);
            
            return ResponseEntity.ok(ApiResponse.success("TOTP status retrieved", status));
        } catch (Exception e) {
            log.error("Error checking TOTP status: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error checking TOTP status: " + e.getMessage()));
        }
    }
    
    /**
     * Gets the client IP address from the request
     * 
     * @param request HTTP request
     * @return IP address as string
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Return the first IP in the list if it's a comma-separated list
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
