package io.strategiz.service.profile.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import io.strategiz.service.profile.ProfileService;
import io.strategiz.service.profile.model.UpdateProfileRequest;
import io.strategiz.service.profile.model.ReadProfileResponse;
import io.strategiz.service.profile.model.UpdateProfileVerificationRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;

import jakarta.validation.Valid;

/**
 * Controller for user profile management.
 * Provides endpoints for creating, reading, updating profile information.
 * Uses clean architecture - returns resources directly, no wrappers.
 */
@RestController
@RequestMapping("/v1/profile")
@Validated
public class ProfileController {

    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    /**
     * Create a new user profile
     * 
     * @param request Profile creation request
     * @return Clean profile response - no wrapper, let GlobalExceptionHandler handle errors
     */
    @PostMapping
    public ResponseEntity<ReadProfileResponse> createProfile(@Valid @RequestBody UpdateProfileRequest request) {
        log.info("Creating profile for user: {}", request.getEmail());
        
        // Create profile - let exceptions bubble up
        ReadProfileResponse profile = profileService.createProfile(request);
        
        // Return clean response - headers added by StandardHeadersInterceptor
        return ResponseEntity.status(HttpStatus.CREATED).body(profile);
    }

    /**
     * Get the current user's profile
     * 
     * @param principal The authenticated user principal
     * @return Clean profile response - no wrapper, let GlobalExceptionHandler handle errors
     */
    @GetMapping("/me")
    public ResponseEntity<ReadProfileResponse> getMyProfile(Principal principal) {
        if (principal == null) {
            throw new RuntimeException("Authentication required");
        }
        
        String userId = principal.getName();
        log.info("Retrieving profile for user: {}", userId);
        
        // Get profile - let exceptions bubble up
        ReadProfileResponse profile = profileService.getProfile(userId);
        
        if (profile == null) {
            throw new RuntimeException("Profile not found for user: " + userId);
        }
        
        // Return clean response - headers added by StandardHeadersInterceptor
        return ResponseEntity.ok(profile);
    }

    /**
     * Get profile by user ID
     * 
     * @param userId The user ID to get profile for
     * @return Clean profile response - no wrapper, let GlobalExceptionHandler handle errors
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ReadProfileResponse> getProfileById(@PathVariable String userId) {
        log.info("Retrieving profile for user ID: {}", userId);
        
        // Get profile - let exceptions bubble up
        ReadProfileResponse profile = profileService.getProfile(userId);
        
        if (profile == null) {
            throw new RuntimeException("Profile not found for user: " + userId);
        }
        
        // Return clean response - headers added by StandardHeadersInterceptor
        return ResponseEntity.ok(profile);
    }

    /**
     * Update the current user's profile
     * 
     * @param request Profile update request
     * @param principal The authenticated user principal
     * @return Clean updated profile response - no wrapper, let GlobalExceptionHandler handle errors
     */
    @PutMapping("/me")
    public ResponseEntity<ReadProfileResponse> updateMyProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Principal principal) {
        
        if (principal == null) {
            throw new RuntimeException("Authentication required");
        }
        
        String userId = principal.getName();
        log.info("Updating profile for user: {}", userId);
        
        // Update profile - let exceptions bubble up
        ReadProfileResponse updatedProfile = profileService.updateProfile(userId, request);
        
        // Return clean response - headers added by StandardHeadersInterceptor
        return ResponseEntity.ok(updatedProfile);
    }

    /**
     * Verify user's email address
     * 
     * @param request Verification request containing verification code
     * @param principal The authenticated user principal
     * @return Clean verified profile response - no wrapper, let GlobalExceptionHandler handle errors
     */
    @PostMapping("/verify-email")
    public ResponseEntity<ReadProfileResponse> verifyEmail(
            @Valid @RequestBody UpdateProfileVerificationRequest request,
            Principal principal) {
        
        if (principal == null) {
            throw new RuntimeException("Authentication required");
        }
        
        String userId = principal.getName();
        log.info("Verifying email for user: {}", userId);
        
        // Verify email - let exceptions bubble up
        ReadProfileResponse verifiedProfile = profileService.verifyEmail(userId, request.getVerificationCode());
        
        // Return clean response - headers added by StandardHeadersInterceptor
        return ResponseEntity.ok(verifiedProfile);
    }

    /**
     * Deactivate the current user's profile
     * 
     * @param principal The authenticated user principal
     * @return Empty response indicating successful deactivation
     */
    @DeleteMapping("/me")
    public ResponseEntity<Void> deactivateProfile(Principal principal) {
        if (principal == null) {
            throw new RuntimeException("Authentication required");
        }
        
        String userId = principal.getName();
        log.info("Deactivating profile for user: {}", userId);
        
        // Deactivate profile - let exceptions bubble up
        profileService.deactivateProfile(userId);
        
        // Return clean response - headers added by StandardHeadersInterceptor
        return ResponseEntity.noContent().build();
    }
}
