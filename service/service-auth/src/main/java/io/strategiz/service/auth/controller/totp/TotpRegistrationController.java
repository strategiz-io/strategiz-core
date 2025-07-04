package io.strategiz.service.auth.controller.totp;

import io.strategiz.service.auth.service.totp.TotpRegistrationService;
import io.strategiz.service.auth.model.totp.TotpRegistrationRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

/**
 * Controller for TOTP registration operations.
 * Handles setup and configuration of time-based one-time passwords.
 * Uses clean architecture - returns resources directly, no wrappers.
 */
@RestController
@RequestMapping("/auth/totp/setup")
public class TotpRegistrationController {

    private static final Logger log = LoggerFactory.getLogger(TotpRegistrationController.class);
    
    private final TotpRegistrationService totpRegistrationService;
    
    public TotpRegistrationController(TotpRegistrationService totpRegistrationService) {
        this.totpRegistrationService = totpRegistrationService;
    }
    
    /**
     * Initialize TOTP setup for a user
     * 
     * @param request TOTP setup request containing user information
     * @return Clean setup response with QR code - no wrapper, let GlobalExceptionHandler handle errors
     */
    @PostMapping("/initialize")
    public ResponseEntity<Map<String, Object>> initializeSetup(@Valid @RequestBody TotpRegistrationRequest request) {
        log.info("Initializing TOTP setup for user: {}", request.userId());
        
        // Generate TOTP secret and QR code - let exceptions bubble up
        String qrCodeUri = totpRegistrationService.generateTotpSecret(request.userId());
        
        Map<String, Object> response = Map.of(
            "success", true,
            "qrCodeUri", qrCodeUri,
            "userId", request.userId(),
            "message", "TOTP setup initialized successfully"
        );
        
        // Return clean response - headers added by StandardHeadersInterceptor
        return ResponseEntity.ok(response);
    }
    
    /**
     * Complete TOTP registration by verifying the first code
     * 
     * @param request Request containing userId and verification code
     * @return Clean registration response - no wrapper, let GlobalExceptionHandler handle errors
     */
    @PostMapping("/complete")
    public ResponseEntity<Map<String, Object>> completeRegistration(@Valid @RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        String code = request.get("code");
        String sessionToken = request.get("sessionToken");
        
        log.info("Completing TOTP registration for user: {}", userId);
        
        // Complete TOTP registration - let exceptions bubble up
        boolean completed = totpRegistrationService.enableTotp(userId, sessionToken, code);
        
        Map<String, Object> result = Map.of(
            "completed", completed,
            "userId", userId,
            "message", completed ? "TOTP enabled successfully" : "TOTP verification failed"
        );
        
        // Return clean response - headers added by StandardHeadersInterceptor
        return ResponseEntity.ok(result);
    }
    
    /**
     * Check TOTP status for a user
     * 
     * @param userId The user ID to check
     * @return Clean status response - no wrapper, let GlobalExceptionHandler handle errors
     */
    @GetMapping("/status/{userId}")
    public ResponseEntity<Map<String, Object>> checkStatus(@PathVariable String userId) {
        log.info("Checking TOTP status for user: {}", userId);
        
        // Check TOTP status - let exceptions bubble up
        boolean enabled = totpRegistrationService.isTotpSetUp(userId);
        
        Map<String, Object> result = Map.of(
            "enabled", enabled,
            "userId", userId
        );
        
        // Return clean response - headers added by StandardHeadersInterceptor
        return ResponseEntity.ok(result);
    }
    
    /**
     * Disable TOTP for a user
     * 
     * @param userId The user ID to disable TOTP for
     * @return Clean disable response - no wrapper, let GlobalExceptionHandler handle errors
     */
    @PostMapping("/disable/{userId}")
    public ResponseEntity<Map<String, Object>> disableTotp(@PathVariable String userId) {
        log.info("Disabling TOTP for user: {}", userId);
        
        // Disable TOTP - let exceptions bubble up
        totpRegistrationService.disableTotp(userId);
        
        Map<String, Object> result = Map.of(
            "disabled", true,
            "userId", userId,
            "message", "TOTP disabled successfully"
        );
        
        // Return clean response - headers added by StandardHeadersInterceptor
        return ResponseEntity.ok(result);
    }
}
