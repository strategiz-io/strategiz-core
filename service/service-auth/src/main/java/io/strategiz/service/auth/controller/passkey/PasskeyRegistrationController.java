package io.strategiz.service.auth.controller.passkey;

import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.auth.exception.ServiceAuthErrorDetails;
import io.strategiz.service.auth.model.passkey.PasskeyRegistrationCompletionRequest;
import io.strategiz.service.auth.model.passkey.PasskeyRegistrationRequest;
import io.strategiz.service.auth.service.passkey.PasskeyRegistrationService;
import io.strategiz.service.auth.service.passkey.PasskeyRegistrationService.AuthTokens;
import io.strategiz.service.auth.service.passkey.PasskeyRegistrationService.RegistrationChallenge;
import io.strategiz.service.auth.service.passkey.PasskeyRegistrationService.RegistrationCompletion;
import io.strategiz.service.auth.service.passkey.PasskeyRegistrationService.RegistrationRequest;
import io.strategiz.service.auth.service.passkey.PasskeyRegistrationService.RegistrationResult;
import io.strategiz.service.auth.service.emailotp.EmailOtpAuthenticationService;
import io.strategiz.service.auth.util.CookieUtil;
import io.strategiz.business.tokenauth.SessionAuthBusiness;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.base.constants.ModuleConstants;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.data.user.entity.UserProfileEntity;
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
 * Controller for passkey registration using resource-based REST endpoints
 * 
 * This controller handles passkey (WebAuthn) registration operations following
 * REST best practices with plural resource naming and proper HTTP verbs.
 * 
 * Endpoints:
 * - POST /auth/passkeys/registrations - Begin registration (create challenge)
 * - PUT /auth/passkeys/registrations/{id} - Complete registration (submit credential)
 * 
 * Uses clean architecture - returns resources directly, no wrappers.
 */
@RestController
@RequestMapping("/v1/auth/passkeys")
public class PasskeyRegistrationController extends BaseController {
    
    @Override
    protected String getModuleName() {
        return ModuleConstants.AUTH_MODULE;
    }

    private static final Logger log = LoggerFactory.getLogger(PasskeyRegistrationController.class);
    
    @Autowired
    private PasskeyRegistrationService registrationService;
    
    @Autowired
    private EmailOtpAuthenticationService emailOtpAuthenticationService;
    
    @Autowired
    private SessionAuthBusiness sessionAuthBusiness;

    @Autowired
    private CookieUtil cookieUtil;

    @Autowired
    private UserRepository userRepository;

    /**
     * Validate temporary token from Step 1 and extract user ID
     *
     * @param temporaryToken The token from profile creation
     * @param expectedEmail The email that should match the token
     * @return The user ID if validation passes
     * @throws StrategizException if validation fails
     */
    private String validateTemporaryToken(String temporaryToken, String expectedEmail) {
        log.info("=== TOKEN VALIDATION START ===");
        log.info("validateTemporaryToken - email: {}", expectedEmail);
        log.info("validateTemporaryToken - token starts with: {}",
                temporaryToken != null ?
                (temporaryToken.length() > 20 ? temporaryToken.substring(0, 20) + "..." : temporaryToken) : "null");

        // Validate the token format and extract user ID
        Optional<String> userIdOpt = sessionAuthBusiness.validateSession(temporaryToken);
        if (userIdOpt.isEmpty()) {
            log.error("Token validation failed for email: {} - session not found or invalid", expectedEmail);
            throwModuleException(ServiceAuthErrorDetails.INVALID_TOKEN, expectedEmail);
        }

        String userId = userIdOpt.get();

        // CRITICAL: Log the exact userId value from token
        log.info("=== TOKEN VALIDATION RESULT ===");
        log.info("validateTemporaryToken - EXTRACTED userId: [{}]", userId);
        log.info("validateTemporaryToken - userId length: {}", userId != null ? userId.length() : 0);
        log.info("validateTemporaryToken - userId format check: starts with 'usr_pub_' = {}",
                userId != null && userId.startsWith("usr_pub_"));
        log.info("validateTemporaryToken - userId format check: UUID format = {}",
                userId != null && userId.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));

        log.info("Temporary token validated successfully for user: {} with email: {}", userId, expectedEmail);
        log.info("=== TOKEN VALIDATION END ===");
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
     * Begin passkey registration process - Create registration challenge
     * 
     * POST /auth/passkeys/registrations
     * 
     * @param request Registration request with user details and temporary token
     * @return Clean registration challenge response - no wrapper, let GlobalExceptionHandler handle errors
     */
    @PostMapping("/registrations")
    public ResponseEntity<RegistrationChallenge> beginRegistration(
            @RequestBody PasskeyRegistrationRequest request) {

        log.info("=== PASSKEY REGISTRATION: beginRegistration START ===");
        log.info("PasskeyRegistrationController.beginRegistration - email: {}", request.email());
        log.info("PasskeyRegistrationController.beginRegistration - identityToken present: {}", request.identityToken() != null);
        logRequest("beginRegistration", request.email());

        // Validate identity token if provided (Step 1 token), otherwise lookup existing user by email
        String userId = null;
        if (request.identityToken() != null) {
            log.info("PasskeyRegistrationController.beginRegistration - Using identity token to get userId");
            userId = validateTemporaryToken(request.identityToken(), request.email());
            log.info("PasskeyRegistrationController.beginRegistration - userId from token: [{}]", userId);
        } else {
            // Lookup existing user - user must have completed Step 1 (profile creation)
            log.info("PasskeyRegistrationController.beginRegistration - No identity token, looking up by email");
            userId = getExistingUserByEmail(request.email());
            log.info("PasskeyRegistrationController.beginRegistration - userId from email lookup: [{}]", userId);
        }

        log.info("PasskeyRegistrationController.beginRegistration - FINAL userId being used: [{}]", userId);

        // Create registration request
        RegistrationRequest registrationRequest = new RegistrationRequest(userId, request.email());

        // Begin registration - let exceptions bubble up
        RegistrationChallenge challenge = registrationService.beginRegistration(registrationRequest);

        log.info("PasskeyRegistrationController.beginRegistration - Challenge created with userId: [{}]", challenge.userId());
        log.info("=== PASSKEY REGISTRATION: beginRegistration END ===");
        logRequestSuccess("beginRegistration", userId, challenge);
        // Return clean response - headers added by StandardHeadersInterceptor
        return createCleanResponse(challenge);
    }

    /**
     * Complete passkey registration process - Submit credential data
     *
     * PUT /auth/passkeys/registrations/{id}
     *
     * @param registrationId The registration challenge ID
     * @param request Completion request with attestation data
     * @param httpRequest HTTP request for session management
     * @param httpResponse HTTP response for setting cookies
     * @return Clean registration result with tokens - no wrapper, let GlobalExceptionHandler handle errors
     */
    @PutMapping("/registrations/{registrationId}")
    public ResponseEntity<AuthTokens> completeRegistration(
            @PathVariable String registrationId,
            @RequestBody PasskeyRegistrationCompletionRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest,
            jakarta.servlet.http.HttpServletResponse httpResponse) {

        log.info("=== PASSKEY REGISTRATION: completeRegistration START ===");
        log.info("PasskeyRegistrationController.completeRegistration - registrationId: {}", registrationId);
        log.info("PasskeyRegistrationController.completeRegistration - email: {}", request.email());
        log.info("PasskeyRegistrationController.completeRegistration - identityToken present: {}", request.identityToken() != null);
        logRequest("completeRegistration", request.email());

        // Validate identity token to get the same user ID used in beginRegistration
        String userId = null;
        if (request.identityToken() != null) {
            log.info("PasskeyRegistrationController.completeRegistration - Using identity token to get userId");
            userId = validateTemporaryToken(request.identityToken(), request.email());
            log.info("PasskeyRegistrationController.completeRegistration - userId from token: [{}]", userId);
        } else {
            // Lookup existing user - user must have completed Step 1 (profile creation)
            log.info("PasskeyRegistrationController.completeRegistration - No identity token, looking up by email");
            userId = getExistingUserByEmail(request.email());
            log.info("PasskeyRegistrationController.completeRegistration - userId from email lookup: [{}]", userId);
        }

        log.info("PasskeyRegistrationController.completeRegistration - FINAL userId for passkey: [{}]", userId);

        // Create registration completion data
        RegistrationCompletion completion = new RegistrationCompletion(
            userId,
            request.credentialId(),
            request.attestationObject(),
            request.clientDataJSON(),
            request.deviceId()
        );

        log.info("PasskeyRegistrationController.completeRegistration - Calling registrationService.completeRegistration with userId: [{}]", userId);

        // Complete registration - let exceptions bubble up
        RegistrationResult result = registrationService.completeRegistration(completion);

        if (!result.success()) {
            log.warn("Passkey registration failed for: {}", request.email());
            throwModuleException(ServiceAuthErrorDetails.PASSKEY_REGISTRATION_FAILED, request.email());
        }

        // Extract tokens from result
        AuthTokens tokens = (AuthTokens) result.result();

        // Set cookies for server-side session management using standardized CookieUtil
        cookieUtil.setAccessTokenCookie(httpResponse, tokens.accessToken());
        if (tokens.refreshToken() != null) {
            cookieUtil.setRefreshTokenCookie(httpResponse, tokens.refreshToken());
        }

        log.info("=== PASSKEY REGISTRATION: completeRegistration END ===");
        logRequestSuccess("completeRegistration", request.email(), tokens);
        // Return clean response - headers added by StandardHeadersInterceptor
        return createCleanResponse(tokens);
    }

    /**
     * Get registration options for manual WebAuthn configuration
     * 
     * GET /auth/passkeys/registrations/options
     * 
     * @param temporaryToken Token from Step 1
     * @param email User's email address
     * @return Clean registration options - no wrapper, let GlobalExceptionHandler handle errors
     */
    @GetMapping("/registrations/options")
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
            "name", email
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

    /**
     * Get existing user's UUID by email.
     * User MUST already exist from Step 1 (profile creation).
     * This prevents duplicate user creation during passkey registration.
     *
     * @param email User's email address
     * @return The user's UUID
     * @throws StrategizException if user not found (profile not created in Step 1)
     */
    private String getExistingUserByEmail(String email) {
        // User MUST already exist from Step 1 (profile creation)
        Optional<UserEntity> existingUser = userRepository.getUserByEmail(email);

        if (existingUser.isPresent()) {
            String existingUserId = existingUser.get().getUserId();
            log.info("Found existing user for email: {}, userId: {}", email, existingUserId);
            return existingUserId;
        }

        // User not found - they need to complete Step 1 first
        log.error("User not found for email: {}. Step 1 (profile creation) must be completed first.", email);
        throwModuleException(ServiceAuthErrorDetails.USER_NOT_FOUND, email);
        return null; // Never reached due to exception
    }
}
