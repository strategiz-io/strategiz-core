package io.strategiz.service.profile.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.strategiz.service.profile.service.SignupProfileService;
import io.strategiz.service.profile.model.CreateProfileRequest;
import io.strategiz.service.profile.model.CreateProfileResponse;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.base.constants.ModuleConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.Valid;

/**
 * Controller for profile creation during signup flow.
 * Handles profile creation with initial user data.
 * Uses clean architecture - returns resources directly, no wrappers.
 */
@RestController
@RequestMapping("/v1/signup/profile")
public class SignupProfileController extends BaseController {

    @Override
    protected String getModuleName() {
        return ModuleConstants.PROFILE_MODULE;
    }

    private static final Logger log = LoggerFactory.getLogger(SignupProfileController.class);

    private final SignupProfileService signupProfileService;

    public SignupProfileController(SignupProfileService signupProfileService) {
        this.signupProfileService = signupProfileService;
    }

    /**
     * Create a profile during the signup process
     * 
     * @param request Profile creation request containing user details and temporary token
     * @return Clean profile creation response with user ID and token - no wrapper, let GlobalExceptionHandler handle errors
     */
    @PostMapping
    public ResponseEntity<CreateProfileResponse> createProfile(@Valid @RequestBody CreateProfileRequest request) {
        log.info("Creating signup profile for email: {}", request.getEmail());
        
        // Create profile during signup - let exceptions bubble up
        CreateProfileResponse response = signupProfileService.createUserProfile(request);
        
        // Return clean response - headers added by StandardHeadersInterceptor
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
} 