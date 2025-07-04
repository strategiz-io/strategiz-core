package io.strategiz.service.provider.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import io.strategiz.service.provider.service.SignupProviderService;
import io.strategiz.service.provider.model.SignupCompleteRequest;
import io.strategiz.service.provider.model.SignupCompleteResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Map;

/**
 * Controller for provider connections during signup flow.
 * Handles optional provider setup and signup completion.
 * Uses clean architecture - returns resources directly, no wrappers.
 */
@RestController
@RequestMapping("/api/signup/provider")
public class SignupProviderController {

    private static final Logger log = LoggerFactory.getLogger(SignupProviderController.class);

    private final SignupProviderService signupProviderService;

    public SignupProviderController(SignupProviderService signupProviderService) {
        this.signupProviderService = signupProviderService;
    }

    /**
     * Initiate provider connection during signup
     * 
     * @param providerId The provider to connect
     * @param principal The authenticated user
     * @return Clean provider connection response - no wrapper, let GlobalExceptionHandler handle errors
     */
    @PostMapping("/{providerId}/connect")
    public ResponseEntity<Map<String, String>> initiateProviderConnection(
            @PathVariable String providerId,
            Principal principal) {
        
        if (principal == null) {
            throw new RuntimeException("Authentication required");
        }
        
        String userId = principal.getName();
        log.info("Initiating provider connection during signup for provider: {} and user: {}", providerId, userId);
        
        // Initiate provider connection - let exceptions bubble up
        Map<String, String> connectionData = signupProviderService.initiateProviderConnection(providerId, userId);
        
        // Return clean response - headers added by StandardHeadersInterceptor
        return ResponseEntity.ok(connectionData);
    }

    /**
     * Complete signup without connecting any providers
     * 
     * @param request Signup completion request
     * @param principal The authenticated user
     * @return Clean signup completion response - no wrapper, let GlobalExceptionHandler handle errors
     */
    @PostMapping("/complete")
    public ResponseEntity<SignupCompleteResponse> completeSignupWithoutProvider(
            @Valid @RequestBody SignupCompleteRequest request,
            Principal principal) {
        
        if (principal == null) {
            throw new RuntimeException("Authentication required");
        }
        
        String userId = principal.getName();
        log.info("Completing signup without provider connection for user: {}", userId);
        
        // Complete signup - let exceptions bubble up
        SignupCompleteResponse response = signupProviderService.completeSignup(userId, request);
        
        // Return clean response - headers added by StandardHeadersInterceptor
        return ResponseEntity.ok(response);
    }

    /**
     * Get available providers for signup
     * 
     * @return Clean list of available providers - no wrapper, let GlobalExceptionHandler handle errors
     */
    @GetMapping("/available")
    public ResponseEntity<Map<String, Object>> getAvailableProviders() {
        log.info("Getting available providers for signup");
        
        // Get available providers - let exceptions bubble up
        Map<String, Object> availableProviders = signupProviderService.getAvailableProviders();
        
        // Return clean response - headers added by StandardHeadersInterceptor
        return ResponseEntity.ok(availableProviders);
    }
} 