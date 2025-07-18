package io.strategiz.service.auth.controller.totp;

import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.auth.exception.AuthErrors;
import io.strategiz.service.auth.service.totp.TotpAuthenticationService;
import io.strategiz.service.auth.model.totp.TotpAuthenticationRequest;
import io.strategiz.service.auth.model.ApiTokenResponse;
import io.strategiz.service.base.controller.BaseController;
import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

/**
 * Controller for TOTP authentication using resource-based REST endpoints
 * 
 * This controller handles TOTP (Time-based One-Time Password) authentication operations following
 * REST best practices with plural resource naming and proper HTTP verbs.
 * 
 * Endpoints:
 * - POST /auth/totp/authentications - Authenticate with TOTP code
 * - POST /auth/totp/authentications/verify - Verify TOTP code (no session creation)
 * 
 * Uses clean architecture - returns resources directly, no wrappers.
 */
@RestController
@RequestMapping("/auth/totp")
public class TotpAuthenticationController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(TotpAuthenticationController.class);
    
    @Autowired
    private TotpAuthenticationService totpAuthService;
    
    /**
     * Authenticate using TOTP code - Create full authentication session
     * 
     * POST /auth/totp/authentications
     * 
     * @param request TOTP authentication request containing user ID and code
     * @param servletRequest HTTP request to extract client IP
     * @return Clean authentication response with access tokens
     */
    @PostMapping("/authentications")
    public ResponseEntity<ApiTokenResponse> authenticate(
            @Valid @RequestBody TotpAuthenticationRequest request,
            HttpServletRequest servletRequest) {
        
        logRequest("totpAuthentication", request.userId());
        
        // Extract client IP address
        String ipAddress = servletRequest.getRemoteAddr();
        
        // Authenticate with TOTP and create session - let exceptions bubble up
        ApiTokenResponse tokens = totpAuthService.authenticateWithTotp(
            request.userId(), 
            request.code(), 
            ipAddress,
            request.deviceId()
        );
        
        if (tokens == null || tokens.accessToken() == null) {
            throw new StrategizException(AuthErrors.TOTP_VERIFICATION_FAILED, request.userId());
        }
        
        logRequestSuccess("totpAuthentication", request.userId(), tokens);
        return createCleanResponse(tokens);
    }
    
    /**
     * Verify TOTP code without creating session (for MFA scenarios)
     * 
     * POST /auth/totp/authentications/verify
     * 
     * @param request TOTP verification request
     * @return Clean verification result - no wrapper, let GlobalExceptionHandler handle errors
     */
    @PostMapping("/authentications/verify")
    public ResponseEntity<Map<String, Object>> verify(@Valid @RequestBody TotpAuthenticationRequest request) {
        logRequest("verifyTotpCode", request.userId());
        
        // Verify TOTP code - let exceptions bubble up
        boolean verified = totpAuthService.verifyCode(request.userId(), request.code());
        
        if (!verified) {
            throw new StrategizException(AuthErrors.TOTP_VERIFICATION_FAILED, request.userId());
        }
        
        Map<String, Object> result = Map.of(
            "verified", true,
            "userId", request.userId(),
            "message", "TOTP code verified successfully"
        );
        
        logRequestSuccess("verifyTotpCode", request.userId(), result);
        return createCleanResponse(result);
    }
}
