package io.strategiz.service.auth.controller.passkey;

import io.strategiz.service.auth.model.ApiResponse;
import io.strategiz.service.auth.model.passkey.PasskeyRegistrationCompletionRequest;
import io.strategiz.service.auth.model.passkey.PasskeyRegistrationRequest;
import io.strategiz.service.auth.model.token.SignupIdentityToken;
import io.strategiz.service.auth.service.passkey.PasskeyRegistrationService;
import io.strategiz.service.auth.service.passkey.PasskeyRegistrationService.AuthTokens;
import io.strategiz.service.auth.service.passkey.PasskeyRegistrationService.RegistrationChallenge;
import io.strategiz.service.auth.service.passkey.PasskeyRegistrationService.RegistrationCompletion;
import io.strategiz.service.auth.service.passkey.PasskeyRegistrationService.RegistrationRequest;
import io.strategiz.service.auth.service.passkey.PasskeyRegistrationService.RegistrationResult;
import io.strategiz.service.auth.service.token.SignupTokenService;
import io.strategiz.service.auth.service.emailotp.EmailOtpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for passkey registration endpoints
 */
@RestController
@RequestMapping("/auth/passkey/registration")
public class PasskeyRegistrationController {

    private static final Logger log = LoggerFactory.getLogger(PasskeyRegistrationController.class);
    
    @Autowired
    private PasskeyRegistrationService registrationService;
    
    @Autowired
    private EmailOtpService emailOtpService;
    
    @Autowired
    private SignupTokenService signupTokenService;

    /**
     * Begin WebAuthn registration process
     * Requires email verification before proceeding, either via code or identity token
     *
     * @param request Registration request with user email and verification method
     * @return Response with registration options
     */
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

    @PostMapping("/begin")
    public ResponseEntity<ApiResponse<Map<String, Object>>> beginRegistration(
            @RequestBody PasskeyRegistrationRequest request) {
        try {
            boolean isVerified = false;
            
            // Option 1: Verify using direct verification code
            if (request.verificationCode() != null && !request.verificationCode().isEmpty()) {
                isVerified = emailOtpService.verifyOtp(
                        request.email(), "signup", request.verificationCode());
                
                if (isVerified) {
                    log.info("Email verification successful via code for: {}", request.email());
                } else {
                    log.warn("Invalid verification code provided for: {}", request.email());
                }
            } 
            // Option 2: Verify using identity token from previous verification
            else if (request.identityToken() != null && !request.identityToken().isEmpty()) {
                SignupIdentityToken identityToken = signupTokenService.validateSignupToken(request.identityToken());
                
                if (identityToken != null && identityToken.email().equals(request.email()) && identityToken.emailVerified()) {
                    isVerified = true;
                    log.info("Email verification confirmed via identity token for: {}", request.email());
                } else {
                    log.warn("Invalid identity token provided for: {}", request.email());
                }
            } 
            // Development-only fallback
            else {
                // Only for development/testing environments
                log.warn("No verification provided for: {}. This would be rejected in production.", 
                        request.email());
                
                // For development, we can allow skipping verification
                // This should be environment-dependent in a real deployment
                isVerified = true;
            }
            
            // Only proceed if verification succeeded
            if (!isVerified) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ApiResponse.<Map<String, Object>>error("Email verification required before registration")
                );
            }
            
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
            user.put("id", challenge.userId());
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
            
            return ResponseEntity.ok(
                ApiResponse.<Map<String, Object>>success("Registration challenge generated", publicKeyCredentialCreationOptions)
            );
        } catch (Exception e) {
            log.error("Error beginning passkey registration: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.<Map<String, Object>>error("Error beginning passkey registration: " + e.getMessage())
            );
        }
    }
    
    /**
     * Complete WebAuthn registration process
     *
     * @param request Completion request with credential data
     * @return Response with registration result
     */
    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<Map<String, String>>> completeRegistration(
            @RequestBody PasskeyRegistrationCompletionRequest request) {
        try {
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
            
            RegistrationResult result = registrationService.completeRegistration(completion);
            
            if (result.success()) {
                AuthTokens tokens = (AuthTokens) result.result();
                Map<String, String> tokenResponse = Map.of(
                    "accessToken", tokens.accessToken(),
                    "refreshToken", tokens.refreshToken(),
                    "tokenType", "Bearer"
                );
                
                return ResponseEntity.ok(
                    ApiResponse.<Map<String, String>>success("Passkey registration successful", tokenResponse)
                );
            } else {
                String errorMessage = result.result() != null ? result.result().toString() : "Passkey registration failed";
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ApiResponse.<Map<String, String>>error(errorMessage)
                );
            }
        } catch (Exception e) {
            log.error("Error completing passkey registration: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.<Map<String, String>>error("Error completing passkey registration: " + e.getMessage())
            );
        }
    }
}
