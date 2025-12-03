package io.strategiz.service.auth.controller.passkey;

import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.auth.exception.AuthErrors;
import io.strategiz.service.auth.model.ApiTokenResponse;
import io.strategiz.service.auth.model.AuthenticationResponse;
import io.strategiz.service.auth.util.CookieUtil;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.service.auth.model.passkey.*;
import io.strategiz.service.auth.service.passkey.PasskeyAuthenticationService;
import io.strategiz.service.auth.service.passkey.PasskeyChallengeService;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.base.constants.ModuleConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


/**
 * Controller for passkey authentication using resource-based REST endpoints
 * 
 * This controller handles passkey (WebAuthn) authentication operations following
 * REST best practices with plural resource naming and proper HTTP verbs.
 * 
 * Endpoints:
 * - POST /auth/passkeys/authentications - Begin authentication (create challenge)
 * - PUT /auth/passkeys/authentications/{id} - Complete authentication (submit assertion)
 * 
 * Uses clean architecture - returns resources directly, no wrappers.
 */
@RestController
@RequestMapping("/v1/auth/passkeys")
public class PasskeyAuthenticationController extends BaseController {
    
    private static final Logger log = LoggerFactory.getLogger(PasskeyAuthenticationController.class);

    @Override
    protected String getModuleName() {
        return ModuleConstants.AUTH_MODULE;
    }

    @Autowired
    private PasskeyChallengeService challengeService;

    @Autowired
    private PasskeyAuthenticationService authenticationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CookieUtil cookieUtil;
    
    // Constructor to log controller initialization
    public PasskeyAuthenticationController() {
        log.info("PasskeyAuthenticationController constructor called - endpoints will be registered at /v1/auth/passkeys/*");
    }
    
    @PostConstruct
    public void init() {
        log.info("PasskeyAuthenticationController @PostConstruct - Controller fully initialized");
        log.info("Registered endpoints:");
        log.info("  - POST /v1/auth/passkeys/authentications (beginAuthentication)");
        log.info("  - PUT /v1/auth/passkeys/authentications/{authenticationId} (completeAuthentication)");
    }

    /**
     * Begin WebAuthn authentication process - Create authentication challenge
     * 
     * POST /auth/passkeys/authentications
     * 
     * @return Clean authentication challenge response - no wrapper, let GlobalExceptionHandler handle errors
     */
    @PostMapping("/authentications")
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
     * Complete WebAuthn authentication process - Submit assertion data
     * 
     * PUT /auth/passkeys/authentications/{id}
     * 
     * @param authenticationId The authentication challenge ID
     * @param request Completion request with credential data
     * @param servletRequest HTTP request to extract client IP
     * @return Clean authentication result with tokens - no wrapper, let GlobalExceptionHandler handle errors
     */
    @PutMapping("/authentications/{authenticationId}")
    public ResponseEntity<AuthenticationResponse> completeAuthentication(
            @PathVariable String authenticationId,
            @RequestBody @Valid PasskeyAuthenticationCompletionRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {

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

        // Set HTTP-only cookies for session management
        cookieUtil.setAccessTokenCookie(servletResponse, result.accessToken());
        cookieUtil.setRefreshTokenCookie(servletResponse, result.refreshToken());
        log.info("Authentication cookies set for user: {}", result.userId());

        // Fetch user data to include in response
        AuthenticationResponse.UserInfo userInfo = null;
        if (result.userId() != null) {
            try {
                UserEntity user = userRepository.findById(result.userId()).orElse(null);
                if (user != null && user.getProfile() != null) {
                    userInfo = new AuthenticationResponse.UserInfo(
                        user.getId(),
                        user.getProfile().getEmail(),
                        user.getProfile().getName(),  // Just use the single name field
                        Boolean.TRUE.equals(user.getProfile().getIsEmailVerified())
                    );
                }
            } catch (Exception e) {
                log.warn("Failed to fetch user data for response: {}", e.getMessage());
                // Continue without user data - tokens are more important
            }
        }
        
        // Create response with tokens and user data
        AuthenticationResponse response = AuthenticationResponse.create(
            result.accessToken(),
            result.refreshToken(),
            userInfo
        );
        
        logRequestSuccess("completeAuthentication", result.userId(), response);
        // Return clean response - headers added by StandardHeadersInterceptor
        return createCleanResponse(response);
    }
    
}
