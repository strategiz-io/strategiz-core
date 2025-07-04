package io.strategiz.service.auth.controller.totp;

import io.strategiz.service.auth.service.totp.TotpAuthenticationService;
import io.strategiz.service.auth.model.totp.TotpAuthenticationRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

/**
 * Controller for TOTP authentication operations.
 * Handles time-based one-time password authentication.
 * Uses clean architecture - returns resources directly, no wrappers.
 */
@RestController
@RequestMapping("/auth/totp")
public class TotpAuthenticationController {

    private static final Logger log = LoggerFactory.getLogger(TotpAuthenticationController.class);
    
    private final TotpAuthenticationService totpAuthService;
    
    public TotpAuthenticationController(TotpAuthenticationService totpAuthService) {
        this.totpAuthService = totpAuthService;
    }
    
    /**
     * Authenticate using TOTP code
     * 
     * @param request TOTP authentication request containing user ID and code
     * @return Clean authentication response - no wrapper, let GlobalExceptionHandler handle errors
     */
    @PostMapping("/authenticate")
    public ResponseEntity<Map<String, Object>> authenticate(@Valid @RequestBody TotpAuthenticationRequest request) {
        log.info("Processing TOTP authentication for user: {}", request.userId());
        
        // Authenticate with TOTP - let exceptions bubble up
        totpAuthService.authenticateWithTotp(request.userId(), request.code());
        
        // If we reach here, authentication was successful
        Map<String, Object> response = Map.of(
            "success", true,
            "message", "TOTP authentication successful",
            "userId", request.userId()
        );
        
        // Return clean response - headers added by StandardHeadersInterceptor
        return ResponseEntity.ok(response);
    }
    
    /**
     * Verify TOTP code without creating session (for MFA scenarios)
     * 
     * @param request TOTP verification request
     * @return Clean verification result - no wrapper, let GlobalExceptionHandler handle errors
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify(@Valid @RequestBody TotpAuthenticationRequest request) {
        log.info("Verifying TOTP code for user: {}", request.userId());
        
        // Verify TOTP code - let exceptions bubble up
        boolean verified = totpAuthService.verifyCode(request.userId(), request.code());
        
        Map<String, Object> result = Map.of(
            "verified", verified,
            "userId", request.userId()
        );
        
        // Return clean response - headers added by StandardHeadersInterceptor
        return ResponseEntity.ok(result);
    }
}
