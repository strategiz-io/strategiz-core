package io.strategiz.service.profile.service;

import io.strategiz.business.tokenauth.SessionAuthBusiness;
import io.strategiz.business.tokenauth.SessionAuthBusiness.AuthRequest;
import io.strategiz.business.tokenauth.SessionAuthBusiness.AuthResult;
import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.data.user.entity.UserProfileEntity;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.service.profile.constants.ProfileConstants;
import io.strategiz.service.profile.model.CreateProfileRequest;
import io.strategiz.service.profile.model.CreateProfileResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Optional;

/**
 * Service for handling user profile creation during signup
 */
@Service
@Transactional
public class SignupProfileService {
    
    private static final Logger log = LoggerFactory.getLogger(SignupProfileService.class);
    
    private final UserRepository userRepository;
    private final SessionAuthBusiness sessionAuthBusiness;
    
    public SignupProfileService(UserRepository userRepository, SessionAuthBusiness sessionAuthBusiness) {
        this.userRepository = userRepository;
        this.sessionAuthBusiness = sessionAuthBusiness;
    }
    
    /**
     * Create signup profile
     */
    public CreateProfileResponse createSignupProfile(CreateProfileRequest request) {
        log.info(ProfileConstants.LogMessages.CREATING_SIGNUP_PROFILE + "{}", request.getEmail());
        
        // Check if user already exists
        Optional<UserEntity> existingUser = userRepository.findByEmail(request.getEmail());
        
        if (existingUser.isPresent()) {
            UserEntity user = existingUser.get();
            
            // User already exists, return existing profile for signup continuation
            log.info(ProfileConstants.LogMessages.USER_EXISTS_CONTINUE + "{}", user.getId());
            
            // Update the profile with new information if provided
            UserProfileEntity profile = user.getProfile();
            if (request.getName() != null && !request.getName().equals(profile.getName())) {
                profile.setName(request.getName());
                user.setProfile(profile);
                user = userRepository.save(user);
            }
            
            // Generate a partial authentication token (ACR=1) for signup flow
            String identityToken = generatePartialAuthToken(user.getId(), user.getProfile().getEmail());
            
            CreateProfileResponse response = new CreateProfileResponse();
            response.setUserId(user.getId());
            response.setName(user.getProfile().getName());
            response.setEmail(user.getProfile().getEmail());
            response.setIdentityToken(identityToken);
            
            return response;
        }
        
        // User doesn't exist, create new profile
        UserEntity user = createProfile(request.getName(), request.getEmail());
        
        // Generate a partial authentication token (ACR=1) for signup flow
        String identityToken = generatePartialAuthToken(user.getId(), user.getProfile().getEmail());
        
        CreateProfileResponse response = new CreateProfileResponse();
        response.setUserId(user.getId());
        response.setName(user.getProfile().getName());
        response.setEmail(user.getProfile().getEmail());
        response.setIdentityToken(identityToken);
        
        return response;
    }

    /**
     * Create user profile (used by SignupProfileController)
     */
    public CreateProfileResponse createUserProfile(CreateProfileRequest request) {
        return createSignupProfile(request);
    }

    /**
     * Helper method to create a new user profile
     */
    private UserEntity createProfile(String name, String email) {
        log.info(ProfileConstants.LogMessages.CREATING_PROFILE + "{}", email);
        
        UserEntity user = new UserEntity();
        
        UserProfileEntity profile = new UserProfileEntity();
        profile.setName(name);
        profile.setEmail(email);
        profile.setIsEmailVerified(ProfileConstants.Defaults.EMAIL_VERIFIED);
        profile.setSubscriptionTier(ProfileConstants.Defaults.SUBSCRIPTION_TIER);
        profile.setDemoMode(ProfileConstants.Defaults.DEMO_MODE);
        
        user.setProfile(profile);

        // Don't set any ID - let Firestore auto-generate the document ID on save
        // The repository will handle:
        // 1. Auto-generating a UUID document ID
        // 2. Initializing audit fields

        // Save the user - this will be a CREATE operation since id is null
        UserEntity savedUser = userRepository.save(user);

        log.info("Created user profile with ID: {} for email: {}", savedUser.getId(), email);

        return savedUser;
    }
    
    /**
     * Generate a partial authentication token for signup flow
     * This token has ACR=1 (partial auth) and is used to verify identity
     * during the multi-step signup process
     */
    private String generatePartialAuthToken(String userId, String email) {
        log.info(ProfileConstants.LogMessages.GENERATING_TOKEN, userId);
        
        // Create auth request for partial authentication
        // No auth methods yet - this is just identity verification
        AuthRequest authRequest = new AuthRequest(
            userId,
            email,
            Collections.emptyList(), // No auth methods yet
            ProfileConstants.Defaults.IS_PARTIAL_AUTH, // isPartialAuth = true for signup flow
            ProfileConstants.Auth.SIGNUP_DEVICE_ID, // Temporary device ID for signup
            "signup-fingerprint", // Temporary fingerprint
            "0.0.0.0", // IP will be set by the controller
            "SignupFlow/1.0", // User agent identifier
            true // demoMode - default to demo for new signups
        );
        
        // Generate the authentication tokens
        AuthResult authResult = sessionAuthBusiness.createAuthentication(authRequest);
        
        // Return only the access token for use as identity token
        return authResult.accessToken();
    }
} 