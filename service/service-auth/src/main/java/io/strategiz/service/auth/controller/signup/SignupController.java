package io.strategiz.service.auth.controller.signup;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.strategiz.service.auth.service.signup.SignupService;
import io.strategiz.service.auth.model.signup.SignupRequest;
import io.strategiz.service.auth.model.signup.SignupResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.Valid;

/**
 * Controller for user signup.
 * Handles user registration with various authentication methods.
 * Uses clean architecture - returns resources directly, no wrappers.
 */
@RestController
@RequestMapping("/auth/signup")
public class SignupController {

    private static final Logger log = LoggerFactory.getLogger(SignupController.class);

    private final SignupService signupService;

    public SignupController(SignupService signupService) {
        this.signupService = signupService;
    }

    /**
     * User signup endpoint
     * 
     * @param request Signup request containing user details and auth preferences
     * @return Clean signup response with user details, tokens, and auth-specific data - no wrapper, let GlobalExceptionHandler handle errors
     */
    @PostMapping
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        log.info("Processing signup request for user: {}", request.getEmail());
        
        // Process signup - let exceptions bubble up
        SignupResponse response = signupService.processSignup(request);
        
        // Return clean response - headers added by StandardHeadersInterceptor
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Complete signup process after email verification or other steps
     * TODO: Implement when needed for multi-step signup flows
     * 
     * @param completionData Additional data for completing signup
     * @return Clean completion response - no wrapper, let GlobalExceptionHandler handle errors
     */
    @PostMapping("/complete")
    public ResponseEntity<String> completeSignup(@RequestBody Object completionData) {
        log.info("Completing signup process - placeholder implementation");
        
        // TODO: Implement complete signup logic when needed
        // For now, return a simple acknowledgment
        
        // Return clean response - headers added by StandardHeadersInterceptor
        return ResponseEntity.ok("Signup completion not yet implemented");
    }
}
