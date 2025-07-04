package io.strategiz.service.profile;

import io.strategiz.data.user.model.User;
import io.strategiz.data.user.model.UserProfile;
import io.strategiz.data.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import io.strategiz.service.profile.model.UpdateProfileRequest;
import io.strategiz.service.profile.model.ReadProfileResponse;
import io.strategiz.service.profile.model.CreateProfileRequest;
import io.strategiz.service.profile.model.CreateProfileResponse;

/**
 * Service for managing user profile information
 */
@Service
public class ProfileService {
    
    private static final Logger logger = LoggerFactory.getLogger(ProfileService.class);
    
    private final UserRepository userRepository;
    
    @Autowired
    public ProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    /**
     * Create a new user profile
     * 
     * @param name User's display name
     * @param email User's email
     * @return The created user with profile information
     */
    public User createProfile(String name, String email) {
        logger.info("Creating profile for user with email: {}", email);
        
        // Check if user with this email already exists
        Optional<User> existingUser = userRepository.getUserByEmail(email);
        if (existingUser.isPresent()) {
            throw new IllegalArgumentException("User with email " + email + " already exists");
        }
        
        String userId = UUID.randomUUID().toString();
        User user = new User(userId, name, email, "system");
        return userRepository.createUser(user);
    }
    
    /**
     * Get a user's profile by ID
     * 
     * @param userId User ID
     * @return Optional containing the user if found
     */
    public Optional<User> getProfileById(String userId) {
        logger.debug("Getting profile for user ID: {}", userId);
        return userRepository.getUserById(userId);
    }
    
    /**
     * Find a user's profile by email address
     * 
     * @param email User email
     * @return Optional containing the user if found
     */
    public Optional<User> getProfileByEmail(String email) {
        logger.debug("Getting profile by email: {}", email);
        return userRepository.getUserByEmail(email);
    }
    
    /**
     * Update a user's profile information
     * 
     * @param userId User ID
     * @param name Updated name (null to keep current)
     * @param email Updated email (null to keep current)
     * @param photoURL Updated photo URL (null to keep current)
     * @param subscriptionTier Updated subscription tier (null to keep current)
     * @param tradingMode Updated trading mode (null to keep current)
     * @return Updated user
     */
    public User updateProfile(String userId, String name, String email, 
                             String photoURL, String subscriptionTier, String tradingMode) {
        logger.info("Updating profile for user: {}", userId);
        
        Optional<User> existingUserOpt = userRepository.getUserById(userId);
        if (existingUserOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }
        
        User user = existingUserOpt.get();
        UserProfile profile = user.getProfile();
        
        // Update only the fields that are not null
        if (name != null) {
            profile.setName(name);
        }
        
        if (email != null) {
            // If email is changing, check if the new email is already in use
            if (!email.equals(profile.getEmail())) {
                Optional<User> userWithEmail = userRepository.getUserByEmail(email);
                if (userWithEmail.isPresent() && !userWithEmail.get().getUserId().equals(userId)) {
                    throw new IllegalArgumentException("Email " + email + " is already in use");
                }
                profile.setEmail(email);
                profile.setVerifiedEmail(false); // Reset verification when email changes
            }
        }
        
        if (photoURL != null) {
            profile.setPhotoURL(photoURL);
        }
        
        if (subscriptionTier != null) {
            profile.setSubscriptionTier(subscriptionTier);
        }
        
        if (tradingMode != null) {
            profile.setTradingMode(tradingMode);
        }
        
        user.setProfile(profile);
        user.setModifiedAt(new Date());
        user.setModifiedBy(userId);
        user.setVersion(user.getVersion() + 1);
        
        return userRepository.updateUser(user);
    }
    
    /**
     * Verify a user's email address
     * 
     * @param userId User ID
     * @return Updated user
     */
    public User verifyEmail(String userId) {
        logger.info("Verifying email for user: {}", userId);
        
        Optional<User> existingUser = userRepository.getUserById(userId);
        if (existingUser.isEmpty()) {
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }
        
        User user = existingUser.get();
        UserProfile profile = user.getProfile();
        profile.setVerifiedEmail(true);
        user.setProfile(profile);
        user.setModifiedAt(new Date());
        user.setModifiedBy(userId);
        user.setVersion(user.getVersion() + 1);
        
        return userRepository.updateUser(user);
    }
    
    /**
     * Deactivate a user's profile
     * 
     * @param userId User ID
     * @return Updated user
     */
    public User deactivateProfile(String userId) {
        logger.info("Deactivating profile for user: {}", userId);
        
        Optional<User> existingUser = userRepository.getUserById(userId);
        if (existingUser.isEmpty()) {
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }
        
        User user = existingUser.get();
        UserProfile profile = user.getProfile();
        profile.setIsActive(false);
        user.setProfile(profile);
        user.setIsActive(false); // Also deactivate the user
        user.setModifiedAt(new Date());
        user.setModifiedBy("system");
        user.setVersion(user.getVersion() + 1);
        
        return userRepository.updateUser(user);
    }
    
    /**
     * Reactivate a user's profile
     * 
     * @param userId User ID
     * @return Updated user
     */
    public User reactivateProfile(String userId) {
        logger.info("Reactivating profile for user: {}", userId);
        
        Optional<User> existingUser = userRepository.getUserById(userId);
        if (existingUser.isEmpty()) {
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }
        
        User user = existingUser.get();
        UserProfile profile = user.getProfile();
        profile.setIsActive(true);
        user.setProfile(profile);
        user.setIsActive(true); // Also reactivate the user
        user.setModifiedAt(new Date());
        user.setModifiedBy("system");
        user.setVersion(user.getVersion() + 1);
        
        return userRepository.updateUser(user);
    }
    
    /**
     * Create profile from UpdateProfileRequest (adapter for controller)
     */
    public ReadProfileResponse createProfile(UpdateProfileRequest request) {
        User user = createProfile(request.getName(), request.getEmail());
        return mapToReadProfileResponse(user);
    }
    
    /**
     * Get profile and return as ReadProfileResponse (adapter for controller)
     */
    public ReadProfileResponse getProfile(String userId) {
        Optional<User> userOpt = getProfileById(userId);
        if (userOpt.isEmpty()) {
            return null;
        }
        return mapToReadProfileResponse(userOpt.get());
    }
    
    /**
     * Update profile from UpdateProfileRequest (adapter for controller)
     */
    public ReadProfileResponse updateProfile(String userId, UpdateProfileRequest request) {
        User user = updateProfile(userId, request.getName(), request.getEmail(), 
                                request.getPhotoURL(), request.getSubscriptionTier(), 
                                request.getTradingMode());
        return mapToReadProfileResponse(user);
    }
    
    /**
     * Verify email with verification code (adapter for controller)
     */
    public ReadProfileResponse verifyEmail(String userId, String verificationCode) {
        // TODO: Implement verification code validation
        // For now, just verify the email
        User user = verifyEmail(userId);
        return mapToReadProfileResponse(user);
    }
    
    /**
     * Create signup profile (for signup flow)
     * 
     * Handles existing users more gracefully - if user exists but hasn't completed signup,
     * returns their profile for continuation. If user exists and is fully registered,
     * throws appropriate error.
     */
    public CreateProfileResponse createSignupProfile(CreateProfileRequest request) {
        logger.info("Creating signup profile for email: {}", request.getEmail());
        
        // Check if user already exists
        Optional<User> existingUser = userRepository.getUserByEmail(request.getEmail());
        
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            
            // TODO: Check if user has completed signup (has authentication methods)
            // For now, we'll assume if they exist, they can continue the signup flow
            logger.info("User already exists, returning existing profile for signup continuation: {}", user.getUserId());
            
            // Update the profile with new information if provided
            UserProfile profile = user.getProfile();
            if (request.getName() != null && !request.getName().equals(profile.getName())) {
                profile.setName(request.getName());
                user.setProfile(profile);
                user.setModifiedAt(new Date());
                user.setModifiedBy("signup-profile-service");
                user = userRepository.updateUser(user);
            }
            
            CreateProfileResponse response = new CreateProfileResponse();
            response.setUserId(user.getUserId());
            response.setName(user.getProfile().getName());
            response.setEmail(user.getProfile().getEmail());
            response.setIdentityToken("identity-token-" + user.getUserId()); // TODO: Generate real token
            
            return response;
        }
        
        // User doesn't exist, create new profile
        User user = createProfile(request.getName(), request.getEmail());
        
        CreateProfileResponse response = new CreateProfileResponse();
        response.setUserId(user.getUserId());
        response.setName(user.getProfile().getName());
        response.setEmail(user.getProfile().getEmail());
        response.setIdentityToken("identity-token-" + user.getUserId()); // TODO: Generate real token
        
        return response;
    }
    
    /**
     * Map User to ReadProfileResponse
     */
    private ReadProfileResponse mapToReadProfileResponse(User user) {
        ReadProfileResponse response = new ReadProfileResponse();
        UserProfile profile = user.getProfile();
        
        response.setUserId(user.getUserId());
        response.setName(profile.getName());
        response.setEmail(profile.getEmail());
        response.setPhotoURL(profile.getPhotoURL());
        response.setSubscriptionTier(profile.getSubscriptionTier());
        response.setTradingMode(profile.getTradingMode());
        response.setVerifiedEmail(profile.getVerifiedEmail() != null ? profile.getVerifiedEmail() : false);
        response.setActive(profile.getIsActive() != null ? profile.getIsActive() : true);
        response.setCreatedAt(user.getCreatedAt() != null ? user.getCreatedAt().getTime() : 0L);
        response.setModifiedAt(user.getModifiedAt() != null ? user.getModifiedAt().getTime() : 0L);
        
        return response;
    }
}
