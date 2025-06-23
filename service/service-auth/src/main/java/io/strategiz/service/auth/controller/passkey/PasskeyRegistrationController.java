package io.strategiz.service.auth.controller.passkey;

import io.strategiz.service.auth.model.ApiTokenResponse;
import io.strategiz.service.auth.model.passkey.*;
import io.strategiz.service.auth.service.passkey.PasskeyChallengeService;
import io.strategiz.service.auth.service.passkey.PasskeyRegistrationService;
import io.strategiz.service.auth.service.passkey.PasskeyRegistrationService.AuthTokens;
import io.strategiz.service.auth.service.passkey.PasskeyRegistrationService.RegistrationChallenge;
import io.strategiz.service.auth.service.passkey.PasskeyRegistrationService.RegistrationCompletion;
import io.strategiz.service.auth.service.passkey.PasskeyRegistrationService.RegistrationRequest;
import io.strategiz.service.auth.service.passkey.PasskeyRegistrationService.RegistrationResult;
import io.strategiz.service.auth.model.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * Controller for passkey registration endpoints
 */
@RestController
@RequestMapping("/auth/passkey/registration")
public class PasskeyRegistrationController {

    private static final Logger logger = LoggerFactory.getLogger(PasskeyRegistrationController.class);

    @Autowired
    private PasskeyChallengeService challengeService;
    
    @Autowired
    private PasskeyRegistrationService registrationService;

    /**
     * Begin WebAuthn registration process
     *
     * @param request Registration request with user email
     * @return Response with registration options
     */
    @PostMapping("/begin")
    public ResponseEntity<ApiResponse<RegistrationChallenge>> beginRegistration(
            @RequestBody PasskeyRegistrationRequest request) {
        try {
            // Convert from API model to service model
            RegistrationRequest serviceRequest = 
                new RegistrationRequest(request.email(), request.displayName());
                
            RegistrationChallenge response = registrationService.beginRegistration(serviceRequest);
            return ResponseEntity.ok(
                ApiResponse.<RegistrationChallenge>success("Registration challenge generated", response)
            );
        } catch (Exception e) {
            logger.error("Error beginning passkey registration: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.<RegistrationChallenge>error("Error beginning passkey registration: " + e.getMessage())
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
    public ResponseEntity<ApiResponse<ApiTokenResponse>> completeRegistration(
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
                ApiTokenResponse tokenResponse = new ApiTokenResponse(
                    tokens.accessToken(), 
                    tokens.refreshToken(), 
                    "Bearer"
                );
                
                return ResponseEntity.ok(
                    ApiResponse.<ApiTokenResponse>success("Passkey registration successful", tokenResponse)
                );
            } else {
                String errorMessage = result.result() != null ? result.result().toString() : "Passkey registration failed";
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ApiResponse.<ApiTokenResponse>error(errorMessage)
                );
            }
        } catch (Exception e) {
            logger.error("Error completing passkey registration: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.<ApiTokenResponse>error("Error completing passkey registration: " + e.getMessage())
            );
        }
    }
    
    /**
     * Finish passkey registration process
     *
     * @param request Registration request with credential data
     * @return Registration result
     */
    @PostMapping("/finish")
    public ResponseEntity<ApiResponse<Map<String, String>>> finishRegistration(
            @RequestBody PasskeyRegistrationCompletionRequest request) {
        try {
            RegistrationCompletion completion = new RegistrationCompletion(
                request.email(),
                request.credentialId(), 
                "", // Public key will be extracted from attestation
                request.attestationObject(),
                request.clientDataJSON(),
                "", // User agent
                request.deviceId() // Using deviceId as device name
            );
            
            RegistrationResult result = registrationService.completeRegistration(completion);
            
            if (!result.success()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ApiResponse.<Map<String, String>>error("Registration failed")
                );
            }
            
            AuthTokens tokens = (AuthTokens)result.result();
            Map<String, String> responseData = Map.of(
                "userId", request.email(),
                "accessToken", tokens.accessToken(),
                "refreshToken", tokens.refreshToken()
            );
            
            return ResponseEntity.ok(
                ApiResponse.<Map<String, String>>success("Registration successful", responseData)
            );
        } catch (Exception e) {
            logger.error("Error during passkey registration: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.<Map<String, String>>error("Error during registration: " + e.getMessage())
            );
        }
    }
}
