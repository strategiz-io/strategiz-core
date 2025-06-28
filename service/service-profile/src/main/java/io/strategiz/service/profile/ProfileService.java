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
}
