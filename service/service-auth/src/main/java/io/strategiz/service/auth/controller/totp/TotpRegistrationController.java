package io.strategiz.service.auth.controller.totp;

import io.strategiz.data.user.model.User;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.service.auth.service.totp.TotpRegistrationService;
import io.strategiz.service.auth.model.totp.*;
import io.strategiz.service.auth.model.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * REST controller for TOTP registration operations
 * Handles setup, verification, and management of TOTP for users
 */
@RestController
@RequestMapping("/auth/totp/registration")
public class TotpRegistrationController {
    private static final Logger log = LoggerFactory.getLogger(TotpRegistrationController.class);

    @Autowired
    private TotpRegistrationService totpRegistrationService;
    
    @Autowired
    private UserRepository userRepository;

    /**
     * Initiates TOTP registration for a user
     * Returns a QR code for the user to scan with their authenticator app
     * 
     * @param request TOTP registration request with user ID
     * @return Response with QR code for TOTP setup
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TotpRegistrationResponse>> registerTotp(@RequestBody TotpRegistrationRequest request) {
        log.info("Initiating TOTP registration for user ID: {}", request.userId());
        
        try {
            if (totpRegistrationService.isTotpSetUp(request.userId())) {
                log.warn("TOTP already registered for user: {}", request.userId());
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("TOTP is already registered for this user"));
            }
            
            Optional<User> userOpt = userRepository.findById(request.userId());
            if (userOpt.isEmpty()) {
                log.warn("User not found: {}", request.userId());
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("User not found"));
            }
            
            // Get user's email which will be used as the TOTP account name
            String email = userOpt.get().getProfile() != null ? userOpt.get().getProfile().getEmail() : null;
            if (email == null || email.isBlank()) {
                log.warn("User email is null or empty: {}", request.userId());
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("User email is required for TOTP registration"));
            }
            
            // Generate new TOTP secret and QR code
            String qrCodeImageUri = totpRegistrationService.generateTotpSecret(request.userId());
            
            // Note: Secret is now managed internally by the service and not returned directly
            // We use a placeholder here as the secret needs to be passed to the frontend
            // for initialization, but it's stored securely in the database
            String secretPlaceholder = "SECRET_MANAGED_BY_SERVICE";
            
            TotpRegistrationResponse response = TotpRegistrationResponse.success(secretPlaceholder, qrCodeImageUri);
            return ResponseEntity.ok(ApiResponse.success("TOTP registration initiated", response));
            
        } catch (Exception e) {
            log.error("Error during TOTP registration: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("TOTP registration error: " + e.getMessage()));
        }
    }
    
    /**
     * Complete TOTP registration by verifying and enabling TOTP for a user
     * This endpoint is used after the user scans the QR code and enters a code
     * from their authenticator app to confirm successful setup
     * 
     * @param request TOTP verification request with user ID and code
     * @return Response indicating verification success or failure
     */
    @PostMapping("/activate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> activateTotp(@RequestBody Map<String, String> request) {
        log.info("Activating TOTP for user ID: {}", request.get("userId"));
        
        try {
            boolean verified = totpRegistrationService.enableTotp(
                request.get("userId"), 
                request.get("sessionToken"), 
                request.get("code")
            );
            
            if (verified) {
                log.info("TOTP activated successfully for user: {}", request.get("userId"));
                Map<String, Object> response = Map.of("activated", true);
                return ResponseEntity.ok(ApiResponse.success("TOTP activation successful", response));
            } else {
                log.warn("TOTP activation failed for user: {}", request.get("userId"));
                Map<String, Object> response = Map.of("activated", false);
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("TOTP activation failed", response));
            }
        } catch (Exception e) {
            log.error("Error activating TOTP: {}", e.getMessage(), e);
            Map<String, Object> response = Map.of("activated", false, "error", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("TOTP activation error: " + e.getMessage(), response));
        }
    }
    
    /**
     * Disables TOTP for a user
     * 
     * @param userId User ID
     * @return Response indicating success or failure
     */
    @DeleteMapping("/disable/{userId}")
    public ResponseEntity<ApiResponse<Void>> disableTotp(@PathVariable String userId) {
        log.info("Disabling TOTP for user ID: {}", userId);
        
        try {
            // Check if TOTP is already registered
            if (!totpRegistrationService.isTotpSetUp(userId)) {
                log.warn("TOTP not registered for user: {}", userId);
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("TOTP is not registered for this user"));
            }
            
            // Disable TOTP
            totpRegistrationService.disableTotp(userId);
            return ResponseEntity.ok(ApiResponse.success("TOTP disabled successfully"));
            
        } catch (Exception e) {
            log.error("Error disabling TOTP: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error disabling TOTP: " + e.getMessage()));
        }
    }
    
    /**
     * Checks if TOTP is registered for a user
     * 
     * @param userId User ID
     * @return Response indicating if TOTP is registered
     */
    @GetMapping("/status/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkTotpStatus(@PathVariable String userId) {
        log.info("Checking TOTP status for user ID: {}", userId);
        
        try {
            boolean enabled = totpRegistrationService.isTotpSetUp(userId);
            return ResponseEntity.ok(ApiResponse.success("TOTP status", Map.of("enabled", enabled)));
        } catch (Exception e) {
            log.error("Error checking TOTP status: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error checking TOTP status: " + e.getMessage()));
        }
    }
}
