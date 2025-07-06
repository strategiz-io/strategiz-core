package io.strategiz.service.auth.controller.passkey;

import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.auth.exception.AuthErrors;
import io.strategiz.service.auth.model.ApiTokenResponse;
import io.strategiz.service.auth.model.passkey.*;
import io.strategiz.service.auth.service.passkey.PasskeyAuthenticationService;
import io.strategiz.service.auth.service.passkey.PasskeyChallengeService;
import io.strategiz.service.base.controller.BaseController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for passkey authentication endpoints.
 * Uses clean architecture - returns resources directly, no wrappers.
 */
@RestController
@RequestMapping("/auth/passkey/authentication")
public class PasskeyAuthenticationController extends BaseController {

    @Autowired
    private PasskeyChallengeService challengeService;
    
    @Autowired
    private PasskeyAuthenticationService authenticationService;

    /**
     * Begin WebAuthn authentication process
     * 
     * @return Clean authentication challenge response - no wrapper, let GlobalExceptionHandler handle errors
     */
    @PostMapping("/begin")
    public ResponseEntity<PasskeyAuthenticationService.AuthenticationChallenge> beginAuthentication() {
        logRequest("beginAuthentication", "anonymous");
        
        // Clean expired challenges
        challengeService.cleanExpiredChallenges();
        
        // Generate challenge - let exceptions bubble up
        PasskeyAuthenticationService.AuthenticationChallenge response = authenticationService.beginAuthentication();
        
        logRequestSuccess("beginAuthentication", "anonymous", response);
        // Return clean response - headers added by StandardHeadersInterceptor
        return createCleanResponse(response);
    }
    
    /**
     * Complete WebAuthn authentication process
     * 
     * @param request Completion request with credential data
     * @param servletRequest HTTP request to extract client IP
     * @return Clean authentication result with tokens - no wrapper, let GlobalExceptionHandler handle errors
     */
    @PostMapping("/complete")
    public ResponseEntity<ApiTokenResponse> completeAuthentication(
            @RequestBody @Valid PasskeyAuthenticationCompletionRequest request,
            HttpServletRequest servletRequest) {
        
        logRequest("completeAuthentication", request.credentialId());
        
        // Extract client IP address
        String ipAddress = servletRequest.getRemoteAddr();
        
        // Convert from API model to service model
        PasskeyAuthenticationService.AuthenticationCompletion completion = new PasskeyAuthenticationService.AuthenticationCompletion(
            request.credentialId(),
            request.authenticatorData(),
            request.clientDataJSON(),
            request.signature(),
            request.userHandle(),
            ipAddress,
            request.deviceId()
        );
        
        // Complete authentication - let exceptions bubble up
        PasskeyAuthenticationService.AuthenticationResult result = authenticationService.completeAuthentication(completion);
        
        if (!result.success()) {
            log.warn("Passkey authentication failed: {}", result.errorMessage());
            String message = result.errorMessage();
            
            if (message != null && message.contains("not found")) {
                throw new StrategizException(AuthErrors.PASSKEY_CHALLENGE_NOT_FOUND, message);
            } else if (message != null && (message.contains("expired") || message.contains("timed out"))) {
                throw new StrategizException(AuthErrors.VERIFICATION_EXPIRED, message);
            } else {
                throw new StrategizException(AuthErrors.VERIFICATION_FAILED, request.credentialId());
            }
        }
        
        // Check token generation success
        if (result.accessToken() == null || result.refreshToken() == null) {
            throw new StrategizException(AuthErrors.VERIFICATION_FAILED, request.credentialId());
        }
        
        // Create response with tokens
        ApiTokenResponse tokenResponse = new ApiTokenResponse(
            result.accessToken(),
            result.refreshToken(),
            request.userHandle() // userId from request
        );
        
        logRequestSuccess("completeAuthentication", request.userHandle(), tokenResponse);
        // Return clean response - headers added by StandardHeadersInterceptor
        return createCleanResponse(tokenResponse);
    }
    
    /**
     * Alternative endpoint for backwards compatibility
     * 
     * @param request Completion request with credential data
     * @param httpRequest HTTP request to extract client IP
     * @return Clean authentication result - no wrapper, let GlobalExceptionHandler handle errors
     */
    @PostMapping("/signin/finish")
    public ResponseEntity<Map<String, String>> finishSignIn(
            @Valid @RequestBody PasskeyAuthenticationCompletionRequest request,
            HttpServletRequest httpRequest) {
        
        logRequest("finishSignIn", request.credentialId());
        
        // Extract client IP address
        String ipAddress = httpRequest.getRemoteAddr();
        
        // Convert from API model to service model
        PasskeyAuthenticationService.AuthenticationCompletion completion = new PasskeyAuthenticationService.AuthenticationCompletion(
            request.credentialId(),
            request.authenticatorData(),
            request.clientDataJSON(),
            request.signature(),
            request.userHandle(),
            ipAddress,
            request.deviceId()
        );
        
        // Complete authentication - let exceptions bubble up
        PasskeyAuthenticationService.AuthenticationResult result = authenticationService.completeAuthentication(completion);
        
        if (!result.success() || result.accessToken() == null || result.refreshToken() == null) {
            throw new StrategizException(AuthErrors.VERIFICATION_FAILED, request.credentialId());
        }

        Map<String, String> responseBody = Map.of(
            "userId", request.userHandle(),
            "accessToken", result.accessToken(),
            "refreshToken", result.refreshToken()
        );
        
        logRequestSuccess("finishSignIn", request.userHandle(), responseBody);
        // Return clean response - headers added by StandardHeadersInterceptor
        return createCleanResponse(responseBody);
    }
}
