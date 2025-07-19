package io.strategiz.service.profile.service;

import io.strategiz.business.tokenauth.SessionAuthBusiness;
import io.strategiz.business.tokenauth.SessionAuthBusiness.AuthRequest;
import io.strategiz.business.tokenauth.SessionAuthBusiness.AuthResult;
import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.data.user.entity.UserProfileEntity;
import io.strategiz.data.user.repository.UserRepository;
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
        log.info("Creating signup profile for email: {}", request.getEmail());
        
        // Check if user already exists
        Optional<UserEntity> existingUser = userRepository.findByEmail(request.getEmail());
        
        if (existingUser.isPresent()) {
            UserEntity user = existingUser.get();
            
            // User already exists, return existing profile for signup continuation
            log.info("User already exists, returning existing profile for signup continuation: {}", user.getId());
            
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
        log.info("Creating new user profile for email: {}", email);
        
        UserEntity user = new UserEntity();
        
        UserProfileEntity profile = new UserProfileEntity();
        profile.setName(name);
        profile.setEmail(email);
        profile.setVerifiedEmail(false);
        profile.setSubscriptionTier("free"); // Default tier
        profile.setTradingMode("demo"); // Default mode
        
        user.setProfile(profile);
        
        // For signup, we need to handle the audit issue properly
        // The user entity needs to be created as a NEW entity (without ID) so BaseRepository 
        // treats it as a create operation, not an update operation
        
        // Don't set any ID - let BaseRepository auto-generate it
        // The BaseRepository will call prepareForCreate() which will:
        // 1. Auto-generate an ID if none exists
        // 2. Initialize audit fields
        
        // Use "SIGNUP" as the audit user ID (who created this user)
        String auditUserId = "SIGNUP";
        
        log.info("Creating new user profile for email: {} with audit user: {}", email, auditUserId);
        
        // Use the UserRepositoryImpl's save method directly, but we need to cast to access BaseRepository's save
        // Actually, let's create a new approach - use the existing save method but fix the underlying issue
        
        // The real issue is in UserRepositoryImpl.save() - it uses user.getUserId() as audit userId
        // For signup, we need to generate the ID and set it so it's not null
        String newUserId = java.util.UUID.randomUUID().toString();
        user.setUserId(newUserId);
        
        log.info("Generated new user ID: {} for email: {}", newUserId, email);
        
        // Initialize audit fields manually before saving to avoid the "without audit fields" error
        user._initAudit(auditUserId);
        
        // Now save - this will be treated as an update since ID is set, but audit fields are already initialized
        return userRepository.save(user);
    }
    
    /**
     * Generate a partial authentication token for signup flow
     * This token has ACR=1 (partial auth) and is used to verify identity
     * during the multi-step signup process
     */
    private String generatePartialAuthToken(String userId, String email) {
        log.info("Generating partial auth token for user: {} during signup", userId);
        
        // Create auth request for partial authentication
        // No auth methods yet - this is just identity verification
        AuthRequest authRequest = new AuthRequest(
            userId,
            email,
            Collections.emptyList(), // No auth methods yet
            true, // isPartialAuth = true for signup flow
            "signup-device", // Temporary device ID for signup
            "signup-fingerprint", // Temporary fingerprint
            "0.0.0.0", // IP will be set by the controller
            "SignupFlow/1.0" // User agent identifier
        );
        
        // Generate the authentication tokens
        AuthResult authResult = sessionAuthBusiness.createAuthentication(authRequest);
        
        // Return only the access token for use as identity token
        return authResult.accessToken();
    }
} 