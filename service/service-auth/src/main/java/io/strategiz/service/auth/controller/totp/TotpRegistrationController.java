package io.strategiz.service.auth.controller.totp;

import io.strategiz.data.user.model.User;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.framework.exception.ApiResponse;
import io.strategiz.framework.exception.DomainService;
import io.strategiz.framework.exception.ErrorFactory;
import io.strategiz.service.auth.service.totp.TotpRegistrationService;
import io.strategiz.service.auth.model.totp.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for TOTP registration operations
 * Handles setup, verification, and management of TOTP for users
 */
@RestController
@RequestMapping("/auth/totp/registration")
@DomainService(domain = "auth")
public class TotpRegistrationController {
    private static final Logger log = LoggerFactory.getLogger(TotpRegistrationController.class);

    private final TotpRegistrationService totpRegistrationService;
    private final UserRepository userRepository;
    private final ErrorFactory errorFactory;
    
    public TotpRegistrationController(TotpRegistrationService totpRegistrationService,
                                     UserRepository userRepository,
                                     ErrorFactory errorFactory) {
        this.totpRegistrationService = totpRegistrationService;
        this.userRepository = userRepository;
        this.errorFactory = errorFactory;
    }

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
        
        // Check if TOTP is already set up - throw exception instead of returning error response
        if (totpRegistrationService.isTotpSetUp(request.userId())) {
            log.warn("TOTP already registered for user: {}", request.userId());
            throw errorFactory.validationFailed("TOTP is already registered for this user")
                .withContext("userId", request.userId());
        }
        
        // Get user and validate - throws exception if not found
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> errorFactory.userNotFound(request.userId()));
        
        // Get user's email which will be used as the TOTP account name
        String email = user.getProfile() != null ? user.getProfile().getEmail() : null;
        if (email == null || email.isBlank()) {
            log.warn("User email is null or empty: {}", request.userId());
            throw errorFactory.validationFailed("User email is required for TOTP registration")
                .withContext("userId", request.userId());
        }
        
        // Generate new TOTP secret and QR code
        String qrCodeImageUri = totpRegistrationService.generateTotpSecret(request.userId());
        
        // Note: Secret is now managed internally by the service and not returned directly
        String secretPlaceholder = "SECRET_MANAGED_BY_SERVICE";
        
        TotpRegistrationResponse response = TotpRegistrationResponse.success(secretPlaceholder, qrCodeImageUri);
        return ResponseEntity.ok(ApiResponse.success(response, "TOTP registration initiated"));
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
        
        String userId = request.get("userId");
        String sessionToken = request.get("sessionToken");
        String code = request.get("code");
        
        // Validate required fields
        if (userId == null || userId.isBlank()) {
            throw errorFactory.validationFailed("User ID is required");
        }
        
        if (code == null || code.isBlank()) {
            throw errorFactory.validationFailed("Verification code is required");
        }
        
        // enableTotp now throws exceptions for failures instead of returning boolean
        totpRegistrationService.enableTotp(userId, sessionToken, code);
        
        // If we got here, activation was successful
        log.info("TOTP activated successfully for user: {}", userId);
        Map<String, Object> response = Map.of("activated", true);
        return ResponseEntity.ok(ApiResponse.success(response, "TOTP activation successful"));
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
        
        // Look up user first - throws exception if not found
        userRepository.findById(userId)
            .orElseThrow(() -> errorFactory.userNotFound(userId));
            
        // Check if TOTP is already registered
        if (!totpRegistrationService.isTotpSetUp(userId)) {
            log.warn("TOTP not registered for user: {}", userId);
            throw errorFactory.validationFailed("TOTP is not registered for this user")
                .withContext("userId", userId);
        }
        
        // Disable TOTP - throws exception if it fails
        totpRegistrationService.disableTotp(userId);
        
        log.info("TOTP disabled successfully for user: {}", userId);
        return ResponseEntity.ok(ApiResponse.success(null, "TOTP disabled successfully"));
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
        
        // Look up user first - throws exception if not found
        userRepository.findById(userId)
            .orElseThrow(() -> errorFactory.userNotFound(userId));
            
        boolean enabled = totpRegistrationService.isTotpSetUp(userId);
        Map<String, Boolean> response = Map.of("enabled", enabled);
        return ResponseEntity.ok(ApiResponse.success(response, "TOTP status retrieved"));
    }
}
