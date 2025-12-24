package io.strategiz.service.profile.service;

import io.strategiz.business.tokenauth.SessionAuthBusiness;
import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.data.user.entity.UserProfileEntity;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.service.base.BaseService;
import io.strategiz.service.profile.constants.ProfileConstants;
import io.strategiz.service.profile.model.CreateProfileRequest;
import io.strategiz.service.profile.model.CreateProfileResponse;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service for handling user profile creation during signup
 */
@Service
public class SignupProfileService extends BaseService {

    @Override
    protected String getModuleName() {
        return "service-profile";
    }

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
            response.setDemoMode(user.getProfile().getDemoMode());
            response.setIdentityToken(identityToken);

            return response;
        }

        // User doesn't exist, create new profile
        UserEntity user = createProfile(request.getName(), request.getEmail(), request.getDemoMode());

        // Generate a partial authentication token (ACR=1) for signup flow
        String identityToken = generatePartialAuthToken(user.getId(), user.getProfile().getEmail());

        CreateProfileResponse response = new CreateProfileResponse();
        response.setUserId(user.getId());
        response.setName(user.getProfile().getName());
        response.setEmail(user.getProfile().getEmail());
        response.setDemoMode(user.getProfile().getDemoMode());
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
    private UserEntity createProfile(String name, String email, Boolean demoMode) {
        log.info("=== SIGNUP PROFILE SERVICE: createProfile START ===");
        log.info("SignupProfileService.createProfile - Creating profile for email: {}, demoMode: {}", email, demoMode);

        UserEntity user = new UserEntity();
        log.info("SignupProfileService.createProfile - UserEntity created, ID before save: {}", user.getId());

        UserProfileEntity profile = new UserProfileEntity();
        profile.setName(name);
        profile.setEmail(email);
        profile.setIsEmailVerified(ProfileConstants.Defaults.EMAIL_VERIFIED);
        profile.setSubscriptionTier(ProfileConstants.Defaults.SUBSCRIPTION_TIER);
        // Use provided demoMode, or default to true if not specified
        profile.setDemoMode(demoMode != null ? demoMode : ProfileConstants.Defaults.DEMO_MODE);

        user.setProfile(profile);
        log.info("SignupProfileService.createProfile - Profile set, userId still: {}", user.getId());

        // Don't set any ID - let Firestore auto-generate the document ID on save
        // The repository will handle:
        // 1. Auto-generating a UUID document ID
        // 2. Initializing audit fields

        // Use createUser() to ensure proper user creation with UUID
        log.info("SignupProfileService.createProfile - Calling userRepository.createUser()");
        UserEntity savedUser = userRepository.createUser(user);

        log.info("=== SIGNUP PROFILE SERVICE: createProfile END ===");
        log.info("SignupProfileService.createProfile - User saved with ID: {} for email: {}", savedUser.getId(), email);
        log.info("SignupProfileService.createProfile - Full userId value: [{}]", savedUser.getId());

        return savedUser;
    }
    
    /**
     * Generate an identity token for signup flow
     * This token uses the identity-key (not session-key) and has limited scope
     * It's used to verify identity during the multi-step signup process
     *
     * Two-Phase Token Flow:
     * Phase 1 (Signup): identity token with scope="profile:create", acr="0"
     * Phase 2 (After Auth): session token with full scopes, acr="1"+"
     */
    private String generatePartialAuthToken(String userId, String email) {
        log.info(ProfileConstants.LogMessages.GENERATING_TOKEN, userId);
        log.info("SignupProfileService.generatePartialAuthToken - userId: [{}]", userId);
        
        // ENHANCED LOGGING: Verify UUID format before creating token
        boolean isValidUUID = userId != null && userId.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        log.info("SignupProfileService.generatePartialAuthToken - userId is valid UUID: {}", isValidUUID);
        if (!isValidUUID) {
            log.error("CRITICAL: Attempting to create identity token with non-UUID userId: [{}]", userId);
            log.error("This will cause incorrect Firestore paths in Step 2/3 when creating subcollections!");
        }

        // Use createIdentityTokenPair to create a proper identity token
        // This uses the identity-key (not session-key) for proper security isolation
        SessionAuthBusiness.TokenPair tokenPair = sessionAuthBusiness.createIdentityTokenPair(userId);

        log.info("SignupProfileService.generatePartialAuthToken - Created identity token for userId: [{}]", userId);

        // Return the identity token (access token from the pair)
        return tokenPair.accessToken();
    }
} 