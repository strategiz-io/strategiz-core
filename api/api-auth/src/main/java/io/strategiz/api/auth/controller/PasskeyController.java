package io.strategiz.api.auth.controller;

import io.strategiz.api.auth.model.ApiResponse;
import io.strategiz.data.auth.Passkey;
import io.strategiz.service.auth.PasskeyService;
import io.strategiz.service.auth.PasskeyService.PasskeyAuthResult;
import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for passkey management endpoints
 */
@RestController
@RequestMapping("/auth/passkeys")
public class PasskeyController {

    private static final Logger logger = LoggerFactory.getLogger(PasskeyController.class);

    @Autowired
    private PasskeyService passkeyService;

    /**
     * Sign up with a new passkey
     *
     * @param userId User ID
     * @param credentialId Credential ID
     * @param publicKey Public key
     * @return Response with passkey details
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Passkey>> registerPasskey(
            @RequestParam String userId,
            @RequestParam String credentialId,
            @RequestParam String publicKey) {
        try {
            Passkey passkey = passkeyService.registerPasskey(userId, credentialId, publicKey);
            return ResponseEntity.ok(
                ApiResponse.<Passkey>success("Passkey sign-up successful", passkey)
            );
        } catch (Exception e) {
            logger.error("Error during passkey sign-up: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.<Passkey>error("Error during passkey sign-up: " + e.getMessage())
            );
        }
    }

    /**
     * Get all passkeys for a user
     *
     * @param userId User ID
     * @return Response with list of passkeys
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Passkey>>> getUserPasskeys(@RequestParam String userId) {
        try {
            List<Passkey> passkeys = passkeyService.getUserPasskeys(userId);
            return ResponseEntity.ok(
                ApiResponse.<List<Passkey>>success("Passkeys retrieved successfully", passkeys)
            );
        } catch (Exception e) {
            logger.error("Error retrieving passkeys: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.<List<Passkey>>error("Error retrieving passkeys: " + e.getMessage())
            );
        }
    }

    /**
     * Delete a passkey
     *
     * @param credentialId Credential ID
     * @return Response with deletion status
     */
    @DeleteMapping("/{credentialId}")
    public ResponseEntity<ApiResponse<Boolean>> deletePasskey(@PathVariable String credentialId) {
        try {
            boolean deleted = passkeyService.deletePasskey(credentialId);
            if (deleted) {
                return ResponseEntity.ok(
                    ApiResponse.<Boolean>success("Passkey deleted successfully", true)
                );
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.<Boolean>error("Passkey not found or could not be deleted", false)
                );
            }
        } catch (Exception e) {
            logger.error("Error deleting passkey: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.<Boolean>error("Error deleting passkey: " + e.getMessage(), false)
            );
        }
    }
    
    /**
     * Sign in with passkey
     * 
     * @param request Authentication request
     * @param httpRequest HTTP request to extract client IP
     * @return Authentication result with tokens
     */
    @PostMapping("/signin")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> authenticate(
            @RequestBody PasskeyAuthenticationRequest request,
            HttpServletRequest httpRequest) {
            
        logger.info("Signing in with passkey for user: {}", request.userId());
        
        try {
            PasskeyAuthResult result = passkeyService.verifyAssertion(
                    request.credentialId(),
                    request.userId(), 
                    request.signature(), 
                    request.authenticatorData(), 
                    request.clientDataJSON(),
                    getClientIp(httpRequest),
                    request.deviceId());
                
            if (result.success()) {
                logger.info("Passkey sign-in successful for user: {}", request.userId());
                AuthenticationResponse authResponse = new AuthenticationResponse(
                        result.accessToken(), 
                        result.refreshToken(), 
                        "Bearer");
                        
                return ResponseEntity.ok(
                    ApiResponse.<AuthenticationResponse>success("Sign-in successful", authResponse)
                );
            } else {
                logger.warn("Passkey sign-in failed for user: {}", request.userId());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ApiResponse.<AuthenticationResponse>error("Sign-in failed")
                );
            }
        } catch (Exception e) {
            logger.error("Error during passkey sign-in: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.<AuthenticationResponse>error("Sign-in error: " + e.getMessage())
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
    
    /**
     * Request for passkey sign-in
     */
    public record PasskeyAuthenticationRequest(
            String credentialId,
            String userId,
            String signature,
            String authenticatorData,
            String clientDataJSON,
            String deviceId) {}
            
    /**
     * Authentication response containing tokens
     */
    public record AuthenticationResponse(
            String accessToken,
            String refreshToken,
            String tokenType) {}
}
