package io.strategiz.service.auth.controller.passkey;

import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.auth.exception.AuthErrors;
import io.strategiz.service.auth.model.passkey.PasskeyRegistrationCompletionRequest;
import io.strategiz.service.auth.model.passkey.PasskeyRegistrationRequest;
import io.strategiz.service.auth.service.passkey.PasskeyRegistrationService;
import io.strategiz.service.auth.service.passkey.PasskeyRegistrationService.AuthTokens;
import io.strategiz.service.auth.service.passkey.PasskeyRegistrationService.RegistrationChallenge;
import io.strategiz.service.auth.service.passkey.PasskeyRegistrationService.RegistrationCompletion;
import io.strategiz.service.auth.service.passkey.PasskeyRegistrationService.RegistrationRequest;
import io.strategiz.service.auth.service.passkey.PasskeyRegistrationService.RegistrationResult;
import io.strategiz.service.auth.service.emailotp.EmailOtpService;
import io.strategiz.business.tokenauth.SessionAuthBusiness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for Step 2 of the signup process: Passkey Registration
 * 
 * This controller handles passkey (WebAuthn) authentication method setup.
 * It expects a temporary token from Step 1 (profile creation) and validates
 * the user's identity before proceeding with passkey registration.
 * 
 * After successful passkey setup, users can proceed to Step 3 (provider integration)
 * or complete the signup process.
 * 
 * Uses clean architecture - returns resources directly, no wrappers.
 */
@RestController
@RequestMapping("/auth/signup/passkey")
public class PasskeyRegistrationController {

    private static final Logger log = LoggerFactory.getLogger(PasskeyRegistrationController.class);
    
    @Autowired
    private PasskeyRegistrationService registrationService;
    
    @Autowired
    private EmailOtpService emailOtpService;
    
    @Autowired
    private SessionAuthBusiness sessionAuthBusiness;

    /**
     * Validate temporary token from Step 1 and extract user ID
     * 
     * @param temporaryToken The token from profile creation
     * @param expectedEmail The email that should match the token
     * @return The user ID if validation passes
     * @throws StrategizException if validation fails
     */
    private String validateTemporaryToken(String temporaryToken, String expectedEmail) {
        // Validate the token format and extract user ID
        Optional<String> userIdOpt = sessionAuthBusiness.validateAccessToken(temporaryToken);
        if (userIdOpt.isEmpty()) {
            throw new StrategizException(AuthErrors.INVALID_TOKEN, expectedEmail);
        }
        
        String userId = userIdOpt.get();
        
        // TODO: Additional validation could be added here:
        // 1. Check that the token has ACR "1" (partial authentication)
        // 2. Verify the email matches the user profile
        // 3. Ensure user exists and has no auth methods set up yet
        // For now, basic token validation is sufficient
        
        log.debug("Temporary token validated for user: {} with email: {}", userId, expectedEmail);
        return userId;
    }

    /**
     * Get default public key credential parameters (algorithms)
     * @return List of supported algorithms
     */
    private List<Map<String, Object>> getDefaultPubKeyCredParams() {
        List<Map<String, Object>> pubKeyCredParams = new ArrayList<>();
        
        // ES256 algorithm (Recommended)
        Map<String, Object> es256 = new HashMap<>();
        es256.put("type", "public-key");
        es256.put("alg", -7);  // ES256 algorithm
        pubKeyCredParams.add(es256);
        
        // RS256 algorithm
        Map<String, Object> rs256 = new HashMap<>();
        rs256.put("type", "public-key");
        rs256.put("alg", -257);  // RS256 algorithm
        pubKeyCredParams.add(rs256);
        
        return pubKeyCredParams;
    }

    /**
     * Step 2: Begin WebAuthn passkey registration process
     * 
     * Expects a temporary token from Step 1 (profile creation). The token is validated
     * to ensure the user has completed profile creation and is authorized to set up
     * authentication methods.
     *
     * @param request Registration request with user email and temporary token from Step 1
     * @return Clean response with WebAuthn registration options - no wrapper, let GlobalExceptionHandler handle errors
     */
    @PostMapping("/begin")
    public ResponseEntity<Map<String, Object>> beginRegistration(
            @RequestBody PasskeyRegistrationRequest request,
            @RequestHeader(value = "Authorization", required = true) String authorizationHeader) {
        
        log.info("Beginning passkey registration for Step 2 for: {}", request.email());
        
        // Extract and validate temporary token from Step 1 (profile creation)
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new StrategizException(AuthErrors.INVALID_TOKEN, request.email());
        }
        
        String temporaryToken = authorizationHeader.substring(7); // Remove "Bearer " prefix
        
        // Validate temporary token - let exceptions bubble up
        validateTemporaryToken(temporaryToken, request.email());
        
        // Validation passed - user is authorized to set up authentication methods
        
        // Convert from API model to service model
        RegistrationRequest serviceRequest = 
            new RegistrationRequest(request.email(), request.displayName());
            
        RegistrationChallenge challenge = registrationService.beginRegistration(serviceRequest);
        
        // Format the response as expected by WebAuthn clients
        Map<String, Object> publicKeyCredentialCreationOptions = new HashMap<>();
        
        // Basic registration parameters
        publicKeyCredentialCreationOptions.put("challenge", challenge.challenge());
        publicKeyCredentialCreationOptions.put("timeout", challenge.timeout());
        
        // Relying Party information
        Map<String, Object> rp = new HashMap<>();
        rp.put("id", challenge.rpId());
        rp.put("name", challenge.rpName());
        publicKeyCredentialCreationOptions.put("rp", rp);
        
        // User information
        Map<String, Object> user = new HashMap<>();
        // WebAuthn requires user.id to be base64url encoded
        String userIdBase64 = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(challenge.userId().getBytes());
        user.put("id", userIdBase64);
        user.put("name", challenge.userId()); // Using email as both id and name
        user.put("displayName", challenge.username());
        publicKeyCredentialCreationOptions.put("user", user);
        
        // Authenticator selection criteria - critical for cross-platform support
        Map<String, Object> authenticatorSelection = new HashMap<>();
        if (challenge.authenticatorSelection().authenticatorAttachment() != null) {
            authenticatorSelection.put("authenticatorAttachment", challenge.authenticatorSelection().authenticatorAttachment());
        }
        authenticatorSelection.put("residentKey", challenge.authenticatorSelection().residentKey());
        authenticatorSelection.put("requireResidentKey", challenge.authenticatorSelection().requireResidentKey());
        authenticatorSelection.put("userVerification", challenge.authenticatorSelection().userVerification());
        publicKeyCredentialCreationOptions.put("authenticatorSelection", authenticatorSelection);
        
        // Attestation and other options
        publicKeyCredentialCreationOptions.put("attestation", challenge.attestation());
        
        // Empty public key credential parameters - will be filled by WebAuthn API
        publicKeyCredentialCreationOptions.put("pubKeyCredParams", getDefaultPubKeyCredParams());
        
        // No excluded credentials for new registration
        publicKeyCredentialCreationOptions.put("excludeCredentials", new ArrayList<>());
        
        // Return clean response - headers added by StandardHeadersInterceptor
        return ResponseEntity.ok(publicKeyCredentialCreationOptions);
    }
    
    /**
     * Step 2: Complete WebAuthn passkey registration process
     * 
     * Completes the passkey registration and returns full authentication tokens.
     * The user can now proceed to Step 3 (provider integration) or complete signup.
     *
     * @param request Completion request with credential data
     * @param authorizationHeader Authorization header with temporary token from Step 1
     * @return Clean response with full authentication tokens for Step 3 - no wrapper, let GlobalExceptionHandler handle errors
     */
    @PostMapping("/complete")
    public ResponseEntity<Map<String, String>> completeRegistration(
            @RequestBody PasskeyRegistrationCompletionRequest request,
            @RequestHeader(value = "Authorization", required = true) String authorizationHeader) {
        
        log.info("Completing passkey registration for Step 2 for: {}", request.email());
        
        // Validate temporary token from Step 1 (same validation as begin endpoint)
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new StrategizException(AuthErrors.INVALID_TOKEN, request.email());
        }
        
        String temporaryToken = authorizationHeader.substring(7); // Remove "Bearer " prefix
        
        // Validate temporary token - let exceptions bubble up
        validateTemporaryToken(temporaryToken, request.email());
        
        // Convert from API model to service model
        RegistrationCompletion completion = new RegistrationCompletion(
            request.email(),
            request.credentialId(), 
            "", // Public key will be extracted from attestation
            request.attestationObject(),
            request.clientDataJSON(),
            "", // User agent - can be added if needed
            request.deviceId() // Using deviceId as device name
        );
        
        // Complete registration - let exceptions bubble up
        RegistrationResult result = registrationService.completeRegistration(completion);
        
        if (!result.success()) {
            // Extract error information from result object if available, or use a default message
            String errorMessage = (result.result() != null) ? 
                result.result().toString() : "Unknown registration error";
            log.warn("Passkey registration failed: {}", errorMessage);
            throw new StrategizException(AuthErrors.VERIFICATION_FAILED, request.email());
        }
        
        AuthTokens tokens = (AuthTokens) result.result();
        
        // Format tokens into a response map
        Map<String, String> tokenResponse = new HashMap<>();
        tokenResponse.put("accessToken", tokens.accessToken());
        tokenResponse.put("refreshToken", tokens.refreshToken());
        tokenResponse.put("tokenType", "Bearer");
        
        log.info("Passkey registration successful for email: {}", request.email());
        
        // Return clean response - headers added by StandardHeadersInterceptor
        return ResponseEntity.ok(tokenResponse);
    }
}
