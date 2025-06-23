package io.strategiz.service.auth.controller.passkey;

import io.strategiz.service.auth.model.ApiTokenResponse;
import io.strategiz.service.auth.model.passkey.*;
import io.strategiz.service.auth.service.passkey.PasskeyAuthenticationService;
import io.strategiz.service.auth.service.passkey.PasskeyChallengeService;
import io.strategiz.service.auth.model.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;

/**
 * Controller for passkey authentication endpoints
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
     * @return Response with authentication options
     */
    @PostMapping("/begin")
    public ResponseEntity<ApiResponse<PasskeyAuthenticationService.AuthenticationChallenge>> beginAuthentication() {
        try {
            // Use the challenge service to validate the request
            challengeService.cleanExpiredChallenges();
            
            PasskeyAuthenticationService.AuthenticationChallenge response = authenticationService.beginAuthentication();
            return ResponseEntity.ok(
                ApiResponse.<PasskeyAuthenticationService.AuthenticationChallenge>success("Authentication challenge generated", response)
            );
        } catch (Exception e) {
            logger.error("Error beginning passkey authentication: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.<PasskeyAuthenticationService.AuthenticationChallenge>error("Error beginning passkey authentication: " + e.getMessage())
            );
        }
    }
    
    /**
     * Complete WebAuthn authentication process
     * 
     * @param request Completion request with credential data
     * @param httpRequest HTTP request to extract client IP
     * @return Authentication result with tokens
     */
    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<ApiTokenResponse>> completeAuthentication(
            @Valid @RequestBody PasskeyAuthenticationCompletionRequest request,
            HttpServletRequest httpRequest) {
        try {
            // Extract client IP address
            String ipAddress = httpRequest.getRemoteAddr();
            
            // Convert from API model to our service model
            PasskeyAuthenticationService.AuthenticationCompletion completion = new PasskeyAuthenticationService.AuthenticationCompletion(
                request.credentialId(),
                request.authenticatorData(),
                request.clientDataJSON(),
                request.signature(),
                request.userHandle(),
                ipAddress,
                request.deviceId() // Use as device ID
            );
            
            PasskeyAuthenticationService.AuthenticationResult result = authenticationService.completeAuthentication(completion);
            
            if (result.success()) {
                logger.info("Passkey authentication successful for credential ID: {}", request.credentialId());
                
                // Check token generation success
                if (result.accessToken() == null || result.refreshToken() == null) {
                    logger.error("Token generation failed - missing tokens in successful result");
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        ApiResponse.<ApiTokenResponse>error("Authentication succeeded but token generation failed")
                    );
                }
                
                ApiTokenResponse tokenResponse = new ApiTokenResponse(
                    result.accessToken(), 
                    result.refreshToken(), 
                    "Bearer"
                );
                
                return ResponseEntity.ok(
                    ApiResponse.<ApiTokenResponse>success("Authentication successful", tokenResponse)
                );
            } else {
                logger.warn("Passkey authentication failed: {}", result.errorMessage());
                
                // Map different error types to appropriate HTTP status codes
                HttpStatus statusCode = HttpStatus.UNAUTHORIZED;
                String message = result.errorMessage();
                
                if (message != null) {
                    if (message.contains("not found")) {
                        statusCode = HttpStatus.NOT_FOUND;
                    } else if (message.contains("expired") || message.contains("timed out")) {
                        statusCode = HttpStatus.GONE;
                    } else if (message.contains("invalid format") || message.contains("malformed")) {
                        statusCode = HttpStatus.BAD_REQUEST;
                    }
                }
                
                return ResponseEntity.status(statusCode).body(
                    ApiResponse.<ApiTokenResponse>error("Authentication failed: " + message)
                );
            }
        } catch (IllegalArgumentException e) {
            // Handle validation errors
            logger.warn("Validation error during passkey authentication: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                ApiResponse.<ApiTokenResponse>error("Invalid request: " + e.getMessage())
            );
        } catch (Exception e) {
            // Handle unexpected errors
            logger.error("Error during passkey authentication: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.<ApiTokenResponse>error("Authentication error: " + e.getMessage())
            );
        }
    }
    
    /**
     * Finish passkey sign-in process
     *
     * @param request Sign-in request with credential data
     * @return Authentication result
     */
    @PostMapping("/signin/finish")
    public ResponseEntity<ApiResponse<Map<String, String>>> finishSignIn(
            @Valid @RequestBody PasskeyAuthenticationCompletionRequest request,
            HttpServletRequest httpRequest) {
        try {
            // Extract client IP address
            String ipAddress = httpRequest.getRemoteAddr();
            
            // Convert from API model to our service model
            PasskeyAuthenticationService.AuthenticationCompletion completion = new PasskeyAuthenticationService.AuthenticationCompletion(
                request.credentialId(),
                request.authenticatorData(),
                request.clientDataJSON(),
                request.signature(),
                request.userHandle(),
                ipAddress,
                request.deviceId() // Use as device ID
            );
            
            PasskeyAuthenticationService.AuthenticationResult result = authenticationService.completeAuthentication(completion);
            if (!result.success() || result.accessToken() == null || result.refreshToken() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ApiResponse.<Map<String, String>>error("Authentication failed")
                );
            }

            Map<String, String> responseBody = Map.of(
                "userId", request.userHandle(),
                "accessToken", result.accessToken(),
                "refreshToken", result.refreshToken()
            );
            
            return ResponseEntity.ok(
                ApiResponse.<Map<String, String>>success("Authentication successful", responseBody)
            );
        } catch (Exception e) {
            logger.error("Error during passkey sign-in: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.<Map<String, String>>error("Authentication error: " + e.getMessage())
            );
        }
    }
}
