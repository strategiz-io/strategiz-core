package io.strategiz.service.auth.controller.signup;

import io.strategiz.service.auth.model.signup.SignupRequest;
import io.strategiz.service.auth.model.signup.SignupResponse;
import io.strategiz.service.auth.service.signup.SignupService;
import io.strategiz.service.base.model.ApiResponseWrapper;

import java.time.ZonedDateTime;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller that handles unified signup process including profile creation
 * and authentication method setup in a single workflow
 */
@RestController
@RequestMapping("/auth/signup")
public class SignupController {

    private static final Logger logger = LoggerFactory.getLogger(SignupController.class);
    
    private final SignupService signupService;
    
    public SignupController(SignupService signupService) {
        this.signupService = signupService;
    }
    
    /**
     * Endpoint for complete signup process
     * 
     * @param request SignupRequest containing profile and authentication details
     * @return ApiResponseWrapper containing SignupResponse with user details, tokens, and auth-specific data
     */
    @PostMapping
    public ResponseEntity<ApiResponseWrapper<SignupResponse>> signup(@Valid @RequestBody SignupRequest request) {
        logger.info("Received signup request for email: {} with auth method: {}", 
            request.getEmail(), request.getAuthMethod());
        
        SignupResponse response = signupService.processSignup(request);
        
        ApiResponseWrapper<SignupResponse> apiResponse = new ApiResponseWrapper<SignupResponse>(
            true,
            200,
            "User successfully registered",
            ZonedDateTime.now(),
            response
        );
        
        return ResponseEntity.ok(apiResponse);
    }
    
    /**
     * Endpoint for completing authentication setup after initial signup
     * Used for methods requiring a second step like passkey registration completion
     */
    @PostMapping("/complete")
    public ResponseEntity<ApiResponseWrapper<SignupResponse>> completeSignup(@RequestBody Object completionData) {
        // Implementation would depend on the auth method
        // For passkey, this would handle the credentials from the client
        // For TOTP, this would verify the first code
        // etc.
        
        return ResponseEntity.ok(new ApiResponseWrapper<SignupResponse>(true, 200, "Authentication setup completed", ZonedDateTime.now(), null));
    }
}
