package io.strategiz.service.profile.controller;

import io.strategiz.data.user.model.User;
import io.strategiz.service.base.controller.BaseApiController;
import io.strategiz.service.base.model.ApiResponseWrapper;
import io.strategiz.service.profile.ProfileService;
import io.strategiz.service.profile.model.ProfileRequest;
import io.strategiz.service.profile.model.ProfileResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * REST controller for user profile management
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController extends BaseApiController {

    private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);
    private final ProfileService profileService;
    
    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }
    
    /**
     * Create a new user profile
     * 
     * @param request Profile creation request with name, email, etc.
     * @return Created profile data
     */
    @PostMapping
    public ResponseEntity<ApiResponseWrapper<ProfileResponse>> createProfile(@Valid @RequestBody ProfileRequest request) {
        try {
            User user = profileService.createProfile(request.getName(), request.getEmail());
            ProfileResponse response = mapUserToProfileResponse(user);
            
            return success(response, "Profile created successfully");
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid profile creation request: {}", e.getMessage());
            return badRequest(e.getMessage());
        } catch (Exception e) {
            logger.error("Error creating profile", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "PROFILE_CREATE_ERROR", "Failed to create profile: " + e.getMessage());
        }
    }
    
    /**
     * Get current user's profile
     * 
     * @param userDetails Current authenticated user
     * @return User's profile data
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponseWrapper<ProfileResponse>> getMyProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String userId = userDetails.getUsername(); // Assuming UserDetails username is the user ID
            Optional<User> userOpt = profileService.getProfileById(userId);
            
            if (userOpt.isPresent()) {
                ProfileResponse response = mapUserToProfileResponse(userOpt.get());
                return success(response);
            } else {
                return notFound("User profile", userId);
            }
        } catch (Exception e) {
            logger.error("Error retrieving profile", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "PROFILE_RETRIEVE_ERROR", "Failed to retrieve profile: " + e.getMessage());
        }
    }
    
    /**
     * Get a user profile by ID (admin only)
     * 
     * @param userId ID of the user to retrieve
     * @return User's profile data
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseWrapper<ProfileResponse>> getProfileById(@PathVariable String userId) {
        try {
            Optional<User> userOpt = profileService.getProfileById(userId);
            
            if (userOpt.isPresent()) {
                ProfileResponse response = mapUserToProfileResponse(userOpt.get());
                return success(response);
            } else {
                return notFound("User profile", userId);
            }
        } catch (Exception e) {
            logger.error("Error retrieving profile", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "PROFILE_RETRIEVE_ERROR", "Failed to retrieve profile: " + e.getMessage());
        }
    }
    
    /**
     * Update current user's profile
     * 
     * @param request Profile update request
     * @param userDetails Current authenticated user
     * @return Updated profile data
     */
    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponseWrapper<ProfileResponse>> updateMyProfile(
            @Valid @RequestBody ProfileRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String userId = userDetails.getUsername(); // Assuming UserDetails username is the user ID
            User user = profileService.updateProfile(userId, 
                    request.getName(), 
                    request.getEmail(),
                    request.getPhotoURL(), 
                    request.getSubscriptionTier(), 
                    request.getTradingMode());
            
            ProfileResponse response = mapUserToProfileResponse(user);
            return success(response, "Profile updated successfully");
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid profile update request: {}", e.getMessage());
            return badRequest(e.getMessage());
        } catch (Exception e) {
            logger.error("Error updating profile", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "PROFILE_UPDATE_ERROR", "Failed to update profile: " + e.getMessage());
        }
    }
    
    /**
     * Verify email for current user
     * 
     * @param userDetails Current authenticated user
     * @return Updated profile data
     */
    @PostMapping("/me/verify-email")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponseWrapper<ProfileResponse>> verifyEmail(
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String userId = userDetails.getUsername(); // Assuming UserDetails username is the user ID
            User user = profileService.verifyEmail(userId);
            
            ProfileResponse response = mapUserToProfileResponse(user);
            return success(response, "Email verified successfully");
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid email verification request: {}", e.getMessage());
            return badRequest(e.getMessage());
        } catch (Exception e) {
            logger.error("Error verifying email", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "EMAIL_VERIFY_ERROR", "Failed to verify email: " + e.getMessage());
        }
    }
    
    /**
     * Deactivate current user's profile
     * 
     * @param userDetails Current authenticated user
     * @return Response with status
     */
    @PostMapping("/me/deactivate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponseWrapper<Void>> deactivateProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String userId = userDetails.getUsername(); // Assuming UserDetails username is the user ID
            profileService.deactivateProfile(userId);
            
            return success(null, "Profile deactivated successfully");
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid profile deactivation request: {}", e.getMessage());
            return badRequest(e.getMessage());
        } catch (Exception e) {
            logger.error("Error deactivating profile", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "PROFILE_DEACTIVATE_ERROR", "Failed to deactivate profile: " + e.getMessage());
        }
    }
    
    /**
     * Helper method to map User entity to ProfileResponse DTO
     */
    private ProfileResponse mapUserToProfileResponse(User user) {
        return new ProfileResponse(
            user.getId(),
            user.getProfile().getName(),
            user.getProfile().getEmail(),
            user.getProfile().getPhotoURL(),
            user.getProfile().getVerifiedEmail(),
            user.getProfile().getSubscriptionTier(),
            user.getProfile().getTradingMode(),
            user.getProfile().getIsActive(),
            user.getCreatedAt() != null ? user.getCreatedAt().getTime() : 0,
            user.getModifiedAt() != null ? user.getModifiedAt().getTime() : 0
        );
    }
}
