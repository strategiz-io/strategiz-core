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
import io.strategiz.service.base.controller.BaseController;
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
public class PasskeyRegistrationController extends BaseController {

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
        Optional<String> userIdOpt = sessionAuthBusiness.validateSession(temporaryToken);
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
        List<Map<String, Object>> credParams = new ArrayList<>();
        
        // ES256 (Elliptic Curve Digital Signature Algorithm using P-256 and SHA-256)
        credParams.add(Map.of("type", "public-key", "alg", -7));
        
        // RS256 (RSA Signature with SHA-256)
        credParams.add(Map.of("type", "public-key", "alg", -257));
        
        // EdDSA (Ed25519)
        credParams.add(Map.of("type", "public-key", "alg", -8));
        
        return credParams;
    }

    /**
     * Begin passkey registration process
     * 
     * @param request Registration request with user details and temporary token
     * @return Clean registration challenge response - no wrapper, let GlobalExceptionHandler handle errors
     */
    @PostMapping("/begin")
    public ResponseEntity<RegistrationChallenge> beginRegistration(
            @RequestBody PasskeyRegistrationRequest request) {
        
        logRequest("beginRegistration", request.email());
        
        // Validate identity token if provided (Step 1 token)
        String userId = null;
        if (request.identityToken() != null) {
            userId = validateTemporaryToken(request.identityToken(), request.email());
        } else {
            // For now, create a user ID based on email - this should be improved
            userId = "user-" + request.email().hashCode();
        }
        
        // Create registration request
        RegistrationRequest registrationRequest = new RegistrationRequest(userId, request.email());
        
        // Begin registration - let exceptions bubble up
        RegistrationChallenge challenge = registrationService.beginRegistration(registrationRequest);
        
        logRequestSuccess("beginRegistration", userId, challenge);
        // Return clean response - headers added by StandardHeadersInterceptor
        return createCleanResponse(challenge);
    }

    /**
     * Complete passkey registration process
     * 
     * @param request Completion request with attestation data
     * @return Clean registration result with tokens - no wrapper, let GlobalExceptionHandler handle errors
     */
    @PostMapping("/complete")
    public ResponseEntity<AuthTokens> completeRegistration(
            @RequestBody PasskeyRegistrationCompletionRequest request) {
        
        logRequest("completeRegistration", request.email());
        
        // Create user ID from email for now - this should be improved
        String userId = "user-" + request.email().hashCode();
        
        // Create registration completion data
        RegistrationCompletion completion = new RegistrationCompletion(
            userId,
            request.credentialId(),
            request.attestationObject(),
            request.clientDataJSON(),
            request.deviceId(),
            "", // Device name not provided in request
            ""  // User agent not provided in request
        );
        
        // Complete registration - let exceptions bubble up
        RegistrationResult result = registrationService.completeRegistration(completion);
        
        if (!result.success()) {
            log.warn("Passkey registration failed for: {}", request.email());
            throw new StrategizException(AuthErrors.PASSKEY_REGISTRATION_FAILED, request.email());
        }
        
        // Extract tokens from result
        AuthTokens tokens = (AuthTokens) result.result();
        
        logRequestSuccess("completeRegistration", request.email(), tokens);
        // Return clean response - headers added by StandardHeadersInterceptor
        return createCleanResponse(tokens);
    }

    /**
     * Get registration options for manual WebAuthn configuration
     * 
     * @param temporaryToken Token from Step 1
     * @param email User's email address
     * @return Clean registration options - no wrapper, let GlobalExceptionHandler handle errors
     */
    @GetMapping("/options")
    public ResponseEntity<Map<String, Object>> getRegistrationOptions(
            @RequestParam String temporaryToken,
            @RequestParam String email) {
        
        logRequest("getRegistrationOptions", email);
        
        // Validate temporary token
        String userId = validateTemporaryToken(temporaryToken, email);
        
        // Create registration options
        Map<String, Object> options = new HashMap<>();
        options.put("challenge", Base64.getEncoder().encodeToString(
            ("challenge-" + System.currentTimeMillis()).getBytes()));
        options.put("rp", Map.of("name", "Strategiz", "id", "strategiz.io"));
        options.put("user", Map.of(
            "id", Base64.getEncoder().encodeToString(userId.getBytes()),
            "name", email,
            "displayName", email
        ));
        options.put("pubKeyCredParams", getDefaultPubKeyCredParams());
        options.put("timeout", 60000);
        options.put("attestation", "direct");
        options.put("authenticatorSelection", Map.of(
            "authenticatorAttachment", "cross-platform",
            "userVerification", "preferred"
        ));
        
        logRequestSuccess("getRegistrationOptions", userId, options);
        // Return clean response - headers added by StandardHeadersInterceptor
        return createCleanResponse(options);
    }
}
