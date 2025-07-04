package io.strategiz.service.profile.service;

import io.strategiz.business.tokenauth.SessionAuthBusiness;
import io.strategiz.business.tokenauth.SessionAuthBusiness.TokenPair;
import io.strategiz.data.user.model.User;
import io.strategiz.data.user.model.UserProfile;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.profile.exception.ProfileErrors;
import io.strategiz.service.profile.model.CreateProfileRequest;
import io.strategiz.service.profile.model.CreateProfileResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.Collections;

/**
 * Service for Step 1 of signup process: Profile Creation
 * 
 * Creates user profiles with basic information and issues temporary tokens
 * that allow proceeding to Step 2 (authentication method setup).
 * 
 * This service does NOT handle authentication method setup - that's handled
 * by authentication-specific services in Step 2.
 */
@Service
public class SignupProfileService {

    private static final Logger logger = LoggerFactory.getLogger(SignupProfileService.class);
    
    private final UserRepository userRepository;
    private final SessionAuthBusiness sessionAuthBusiness;
    
    public SignupProfileService(
        UserRepository userRepository,
        SessionAuthBusiness sessionAuthBusiness
    ) {
        this.userRepository = userRepository;
        this.sessionAuthBusiness = sessionAuthBusiness;
    }
    
    /**
     * Create a user profile for Step 1 of signup
     * 
     * @param request Profile creation request with basic user information
     * @return CreateProfileResponse with user ID and identity token
     */
    @Transactional
    public CreateProfileResponse createUserProfile(CreateProfileRequest request) {
        logger.info("Creating user profile for email: {}", request.getEmail());
        
        try {
            // Check if user already exists
            if (userRepository.getUserByEmail(request.getEmail()).isPresent()) {
                throw new StrategizException(ProfileErrors.PROFILE_ALREADY_EXISTS, request.getEmail());
            }
            
            // Create user entity
            User user = createUserEntity(request);
            
            // Save user to repository
            User createdUser = userRepository.createUser(user);
            if (createdUser == null) {
                throw new StrategizException(ProfileErrors.PROFILE_CREATION_FAILED, request.getEmail());
            }
            
            logger.info("Successfully created user profile with ID: {}", createdUser.getUserId());
            
            // Create temporary token for authentication setup (Step 2)
            // ACR "1" = Partial authentication (profile created, no auth methods yet)
            // This grants limited permissions: read/write profile + auth method setup
            TokenPair tokenPair = sessionAuthBusiness.createAuthenticationTokenPair(
                createdUser.getUserId(),
                Collections.emptyList(), // No authentication methods completed yet
                "1", // ACR "1" - Partial authentication
                null, // No device ID for profile creation
                "signup-flow" // IP address placeholder for signup flow
            );
            
            // Build response
            return new CreateProfileResponse(
                createdUser.getUserId(),
                createdUser.getProfile().getName(),
                createdUser.getProfile().getEmail(),
                tokenPair.accessToken(),
                3600L // 1 hour expiration for auth setup
            );
            
        } catch (StrategizException e) {
            // Re-throw known exceptions
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during profile creation: {}", e.getMessage(), e);
            throw new StrategizException(ProfileErrors.PROFILE_CREATION_FAILED, 
                "Failed to create user profile", e);
        }
    }
    
    /**
     * Create a User entity from the profile creation request
     * 
     * @param request The profile creation request
     * @return A populated User entity (not yet persisted)
     */
    private User createUserEntity(CreateProfileRequest request) {
        // Generate unique user ID
        String userId = UUID.randomUUID().toString();
        
        // Create user entity
        User user = new User();
        user.setUserId(userId);
        
        // Create user profile
        UserProfile profile = new UserProfile();
        profile.setName(request.getName());
        profile.setEmail(request.getEmail());
        profile.setPhotoURL(request.getPhotoURL());
        profile.setVerifiedEmail(false); // Will be verified during auth setup
        profile.setSubscriptionTier("free"); // Default tier
        profile.setTradingMode("demo"); // Default mode
        profile.setIsActive(true);
        
        // Set profile on user
        user.setProfile(profile);
        
        // Set audit fields
        Date now = new Date();
        user.setCreatedBy("signup-profile-service");
        user.setCreatedAt(now);
        user.setModifiedBy("signup-profile-service");
        user.setModifiedAt(now);
        user.setIsActive(true);
        user.setVersion(1);
        
        return user;
    }
} 