package io.strategiz.service.profile.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import io.strategiz.service.profile.service.ProfileService;
import io.strategiz.service.profile.model.UpdateProfileRequest;
import io.strategiz.service.profile.model.ReadProfileResponse;
import io.strategiz.service.profile.model.UpdateProfileVerificationRequest;
import io.strategiz.service.profile.model.UpdateDemoModeRequest;
import io.strategiz.service.profile.model.UpdateDemoModeResponse;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.base.constants.ModuleConstants;
import io.strategiz.framework.authorization.annotation.RequireAuth;
import io.strategiz.framework.authorization.annotation.AuthUser;
import io.strategiz.framework.authorization.context.AuthenticatedUser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.Valid;

/**
 * Controller for user profile management.
 * Provides endpoints for creating, reading, updating profile information.
 * Uses clean architecture - returns resources directly, no wrappers.
 */
@RestController
@RequestMapping("/v1")
@Validated
public class ProfileController extends BaseController {

    @Override
    protected String getModuleName() {
        return ModuleConstants.PROFILE_MODULE;
    }

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
    @PostMapping("/users/profiles")
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
     * @param user The authenticated user from token
     * @return Clean profile response - no wrapper, let GlobalExceptionHandler handle errors
     */
    @GetMapping("/users/profiles/me")
    @RequireAuth(minAcr = "1")
    public ResponseEntity<ReadProfileResponse> getMyProfile(@AuthUser AuthenticatedUser user) {
        String userId = user.getUserId();
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
     * Get profile by user ID.
     * This endpoint requires authentication and users can only access their own profile.
     *
     * @param userId The user ID to get profile for (must match authenticated user)
     * @param user The authenticated user from token
     * @return Clean profile response - no wrapper, let GlobalExceptionHandler handle errors
     */
    @GetMapping("/users/profiles/{userId}")
    @RequireAuth(minAcr = "1")
    public ResponseEntity<ReadProfileResponse> getProfileById(
            @PathVariable String userId,
            @AuthUser AuthenticatedUser user) {

        // Users can only access their own profile
        if (!userId.equals(user.getUserId())) {
            throw new RuntimeException("Access denied: Cannot access another user's profile");
        }

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
     * @param user The authenticated user from token
     * @return Clean updated profile response - no wrapper, let GlobalExceptionHandler handle errors
     */
    @PutMapping("/users/profiles/me")
    @RequireAuth(minAcr = "1")
    public ResponseEntity<ReadProfileResponse> updateMyProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthUser AuthenticatedUser user) {

        String userId = user.getUserId();
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
     * @param user The authenticated user from token
     * @return Clean verified profile response - no wrapper, let GlobalExceptionHandler handle errors
     */
    @PostMapping("/users/profiles/verify-email")
    @RequireAuth(minAcr = "1")
    public ResponseEntity<ReadProfileResponse> verifyEmail(
            @Valid @RequestBody UpdateProfileVerificationRequest request,
            @AuthUser AuthenticatedUser user) {

        String userId = user.getUserId();
        log.info("Verifying email for user: {}", userId);

        // Verify email - let exceptions bubble up
        ReadProfileResponse verifiedProfile = profileService.verifyEmail(userId, request.getVerificationCode());

        // Return clean response - headers added by StandardHeadersInterceptor
        return ResponseEntity.ok(verifiedProfile);
    }

    /**
     * Update the current user's demo mode
     *
     * @param request Demo mode update request
     * @param user The authenticated user from token
     * @return Response with new JWT tokens containing updated demo mode
     */
    @PutMapping("/profile/demo-mode")
    @RequireAuth(minAcr = "1")
    public ResponseEntity<UpdateDemoModeResponse> updateDemoMode(
            @Valid @RequestBody UpdateDemoModeRequest request,
            @AuthUser AuthenticatedUser user) {

        String userId = user.getUserId();
        log.info("Updating demo mode for user: {} to: {}", userId, request.isDemoMode());

        // Update demo mode and get new tokens
        UpdateDemoModeResponse response = profileService.updateDemoMode(userId, request.isDemoMode());

        // Return response with new tokens
        return ResponseEntity.ok(response);
    }

    /**
     * Deactivate the current user's profile
     *
     * @param user The authenticated user from token
     * @return Empty response indicating successful deactivation
     */
    @DeleteMapping("/users/profiles/me")
    @RequireAuth(minAcr = "2")  // Require MFA for account deletion
    public ResponseEntity<Void> deactivateProfile(@AuthUser AuthenticatedUser user) {
        String userId = user.getUserId();
        log.info("Deactivating profile for user: {}", userId);

        // Deactivate profile - let exceptions bubble up
        profileService.deactivateProfile(userId);

        // Return clean response - headers added by StandardHeadersInterceptor
        return ResponseEntity.noContent().build();
    }
}
