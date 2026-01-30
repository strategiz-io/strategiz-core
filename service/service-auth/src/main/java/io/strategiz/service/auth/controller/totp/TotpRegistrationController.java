package io.strategiz.service.auth.controller.totp;

import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.auth.exception.AuthErrors;
import io.strategiz.service.auth.service.totp.TotpRegistrationService;
import io.strategiz.service.auth.model.totp.TotpRegistrationRequest;
import io.strategiz.service.auth.model.totp.TotpRegistrationCompletionRequest;
import io.strategiz.service.base.controller.BaseController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for TOTP registration using resource-based REST endpoints
 * 
 * This controller handles TOTP (Time-based One-Time Password) registration operations following
 * REST best practices with plural resource naming and proper HTTP verbs.
 * 
 * Endpoints:
 * - POST /auth/totp/registrations - Begin TOTP setup (generate secret)
 * - PUT /auth/totp/registrations/{registrationId} - Complete TOTP setup (verify code)
 * - GET /auth/totp/registrations/{userId} - Get TOTP status for user
 * - DELETE /auth/totp/registrations/{userId} - Disable TOTP for user
 * 
 * Uses clean architecture - returns resources directly, no wrappers.
 */
@RestController
@RequestMapping("/v1/auth/totp")
public class TotpRegistrationController extends BaseController {
    
    @Override
    protected String getModuleName() {
        return "service-auth";
    }

    private static final Logger log = LoggerFactory.getLogger(TotpRegistrationController.class);
    
    @Autowired
    private TotpRegistrationService totpRegistrationService;
    
    /**
     * Begin TOTP registration process - Generate TOTP secret and QR code
     * 
     * POST /auth/totp/registrations
     * 
     * @param request Registration request with user details
     * @return Clean registration challenge response with QR code and secret
     */
    @PostMapping("/registrations")
    public ResponseEntity<Map<String, Object>> beginRegistration(
            @RequestBody @Valid TotpRegistrationRequest request) {
        
        logRequest("beginTotpRegistration", request.userId());
        
        // Generate TOTP secret and QR code with details - let exceptions bubble up
        var totpResult = totpRegistrationService.generateTotpSecretWithDetails(request.userId());
        
        Map<String, Object> response = Map.of(
            "success", true,
            "secret", totpResult.getSecret(),
            "qrCodeUri", totpResult.getQrCodeUri(),
            "userId", request.userId(),
            "registrationId", "totp-" + System.currentTimeMillis(),
            "message", "TOTP setup initialized successfully"
        );
        
        logRequestSuccess("beginTotpRegistration", request.userId(), response);
        return createCleanResponse(response);
    }
    
    /**
     * Complete TOTP registration process - Verify TOTP code and finalize setup
     * 
     * PUT /auth/totp/registrations/{id}
     * 
     * @param registrationId The registration challenge ID
     * @param request Completion request with TOTP verification code
     * @return Clean registration result with status
     */
    @PutMapping("/registrations/{registrationId}")
    public ResponseEntity<Map<String, Object>> completeRegistration(
            @PathVariable String registrationId,
            @RequestBody @Valid TotpRegistrationCompletionRequest request,
            HttpServletRequest httpRequest) {

        logRequest("completeTotpRegistration", request.userId());

        // Resolve access token: prefer request body, fall back to cookie
        String accessToken = request.accessToken();
        if (accessToken == null || accessToken.isBlank()) {
            accessToken = extractCookieValue(httpRequest, "strategiz-access-token");
        }

        // Complete TOTP registration and get updated tokens
        Map<String, Object> authResult = totpRegistrationService.enableTotpWithTokenUpdate(
            request.userId(),
            accessToken,
            request.totpCode()
        );
        
        if (authResult == null) {
            log.warn("TOTP registration failed for user: {}", request.userId());
            throw new StrategizException(AuthErrors.TOTP_VERIFICATION_FAILED, request.userId());
        }
        
        // Create success response with updated tokens
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("registrationId", registrationId);
        response.put("userId", request.userId());
        response.put("message", "TOTP registration completed successfully");
        
        // Add the authentication tokens from the business layer
        if (authResult.containsKey("accessToken")) {
            response.put("accessToken", authResult.get("accessToken"));
        }
        if (authResult.containsKey("refreshToken")) {
            response.put("refreshToken", authResult.get("refreshToken"));
        }
        if (authResult.containsKey("identityToken")) {
            response.put("identityToken", authResult.get("identityToken"));
        }
        
        logRequestSuccess("completeTotpRegistration", request.userId(), response);
        return createCleanResponse(response);
    }
    

    /**
     * Get TOTP status for a specific user
     * 
     * GET /auth/totp/registrations/{userId}
     * 
     * @param userId The user ID to check
     * @return Clean status response - no wrapper, let GlobalExceptionHandler handle errors
     */
    @GetMapping("/registrations/{userId}")
    public ResponseEntity<Map<String, Object>> getRegistrationStatus(@PathVariable String userId) {
        logRequest("getTotpRegistrationStatus", userId);
        
        // Check TOTP status - let exceptions bubble up
        boolean enabled = totpRegistrationService.isTotpSetUp(userId);
        
        Map<String, Object> result = Map.of(
            "enabled", enabled,
            "userId", userId,
            "registrationType", "totp"
        );
        
        logRequestSuccess("getTotpRegistrationStatus", userId, result);
        return createCleanResponse(result);
    }
    
    /**
     * Disable TOTP for a specific user
     * 
     * DELETE /auth/totp/registrations/{userId}
     * 
     * @param userId The user ID to disable TOTP for
     * @return Clean disable response - no wrapper, let GlobalExceptionHandler handle errors
     */
    @DeleteMapping("/registrations/{userId}")
    public ResponseEntity<Map<String, Object>> disableRegistration(@PathVariable String userId) {
        logRequest("disableTotpRegistration", userId);
        
        // Disable TOTP - let exceptions bubble up
        totpRegistrationService.disableTotp(userId);
        
        Map<String, Object> result = Map.of(
            "disabled", true,
            "userId", userId,
            "message", "TOTP registration disabled successfully"
        );
        
        logRequestSuccess("disableTotpRegistration", userId, result);
        return createCleanResponse(result);
    }

    private String extractCookieValue(HttpServletRequest request, String cookieName) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
