package io.strategiz.service.auth.controller.passkey;

import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.auth.exception.AuthErrors;
import io.strategiz.service.auth.model.ApiTokenResponse;
import io.strategiz.service.auth.model.passkey.*;
import io.strategiz.service.auth.service.passkey.PasskeyAuthenticationService;
import io.strategiz.service.auth.service.passkey.PasskeyChallengeService;
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
public class PasskeyAuthenticationController {

    private static final Logger logger = LoggerFactory.getLogger(PasskeyAuthenticationController.class);

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
        logger.info("Beginning passkey authentication");
        
        // Clean expired challenges
        challengeService.cleanExpiredChallenges();
        
        // Generate challenge - let exceptions bubble up
        PasskeyAuthenticationService.AuthenticationChallenge response = authenticationService.beginAuthentication();
        
        // Return clean response - headers added by StandardHeadersInterceptor
        return ResponseEntity.ok(response);
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
        
        logger.info("Completing passkey authentication for credential: {}", request.credentialId());
        
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
            logger.warn("Passkey authentication failed: {}", result.errorMessage());
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
            logger.error("Token generation failed - missing tokens in successful result");
            throw new StrategizException(AuthErrors.VERIFICATION_FAILED, request.credentialId());
        }
        
        logger.info("Passkey authentication successful for credential ID: {}", request.credentialId());
        
        ApiTokenResponse tokenResponse = new ApiTokenResponse(
            result.accessToken(), 
            result.refreshToken(), 
            "Bearer"
        );
        
        // Return clean response - headers added by StandardHeadersInterceptor
        return ResponseEntity.ok(tokenResponse);
    }
    
    /**
     * Finish passkey sign-in process
     *
     * @param request Sign-in request with credential data
     * @return Clean authentication result - no wrapper, let GlobalExceptionHandler handle errors
     */
    @PostMapping("/signin/finish")
    public ResponseEntity<Map<String, String>> finishSignIn(
            @Valid @RequestBody PasskeyAuthenticationCompletionRequest request,
            HttpServletRequest httpRequest) {
        
        logger.info("Finishing passkey sign-in for credential: {}", request.credentialId());
        
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
        
        // Return clean response - headers added by StandardHeadersInterceptor
        return ResponseEntity.ok(responseBody);
    }
}
