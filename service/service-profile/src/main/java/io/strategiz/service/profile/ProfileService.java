package io.strategiz.service.profile;

import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.data.user.entity.UserProfileEntity;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.base.BaseService;
import io.strategiz.service.profile.exception.ProfileErrors;
import io.strategiz.service.profile.model.CreateProfileRequest;
import io.strategiz.service.profile.model.ProfileResponse;
import io.strategiz.service.profile.model.ReadProfileResponse;
import io.strategiz.service.profile.model.UpdateProfileRequest;
import io.strategiz.service.profile.model.UpdateProfileResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service for managing user profiles
 */
@Service
@Transactional
public class ProfileService extends BaseService {

    @Override
    protected String getModuleName() {
        return "service-profile";
    }

    private static final Logger log = LoggerFactory.getLogger(ProfileService.class);

    private final UserRepository userRepository;

    public ProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Creates a new user profile (used by regular ProfileController)
     */
    public ReadProfileResponse createProfile(UpdateProfileRequest request) {
        log.debug("Creating profile for user with email: {}", request.getEmail());
        
        // Check if user already exists
        Optional<UserEntity> existingUser = userRepository.findByEmail(request.getEmail());
        if (existingUser.isPresent()) {
            throw new StrategizException(ProfileErrors.PROFILE_ALREADY_EXISTS, "User already exists with email: " + request.getEmail());
        }
        
        // Create new user with profile
        UserEntity user = new UserEntity();
        
        UserProfileEntity profile = new UserProfileEntity();
        profile.setName(request.getName());
        profile.setEmail(request.getEmail());
        profile.setPhotoURL(request.getPhotoURL());
        profile.setSubscriptionTier(request.getSubscriptionTier() != null ? request.getSubscriptionTier() : "free");
        profile.setTradingMode(request.getTradingMode() != null ? request.getTradingMode() : "demo");
        profile.setVerifiedEmail(false);
        profile.setIsActive(true);
        
        user.setProfile(profile);
        
        // Save the user (which will cascade to profile)
        UserEntity savedUser = userRepository.save(user);
        
        log.info("Profile created successfully for user: {}", savedUser.getId());
        return ReadProfileResponse.fromEntity(savedUser);
    }

    /**
     * Gets a user profile by user ID
     */
    public ReadProfileResponse getProfile(String userId) {
        log.debug("Getting profile for user: {}", userId);
        
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new StrategizException(ProfileErrors.PROFILE_NOT_FOUND, "User not found: " + userId);
        }
        
        UserEntity user = userOpt.get();
        if (user.getProfile() == null) {
            throw new StrategizException(ProfileErrors.PROFILE_NOT_FOUND, "Profile not found for user: " + userId);
        }
        
        return ReadProfileResponse.fromEntity(user);
    }

    /**
     * Gets a user profile by email
     */
    public Optional<ProfileResponse> getProfileByEmail(String email) {
        log.debug("Getting profile by email: {}", email);
        
        Optional<UserEntity> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            log.warn("User not found by email: {}", email);
            return Optional.empty();
        }
        
        UserEntity user = userOpt.get();
        if (user.getProfile() == null) {
            log.warn("Profile not found for user with email: {}", email);
            return Optional.empty();
        }
        
        return Optional.of(ProfileResponse.fromEntity(user));
    }

    /**
     * Updates a user profile
     */
    public ReadProfileResponse updateProfile(String userId, UpdateProfileRequest request) {
        log.debug("Updating profile for user: {}", userId);
        
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new StrategizException(ProfileErrors.PROFILE_NOT_FOUND, "User not found: " + userId);
        }
        
        UserEntity user = userOpt.get();
        UserProfileEntity profile = user.getProfile();
        
        if (profile == null) {
            throw new StrategizException(ProfileErrors.PROFILE_NOT_FOUND, "Profile not found for user: " + userId);
        }
        
        // Update profile fields
        if (request.getName() != null) {
            profile.setName(request.getName());
        }
        if (request.getPhotoURL() != null) {
            profile.setPhotoURL(request.getPhotoURL());
        }
        if (request.getSubscriptionTier() != null) {
            profile.setSubscriptionTier(request.getSubscriptionTier());
        }
        if (request.getTradingMode() != null) {
            profile.setTradingMode(request.getTradingMode());
        }
        
        // Save the user
        UserEntity savedUser = userRepository.save(user);
        
        log.info("Profile updated successfully for user: {}", userId);
        return ReadProfileResponse.fromEntity(savedUser);
    }

    /**
     * Updates the subscription tier for a user
     */
    public UpdateProfileResponse updateSubscriptionTier(String userId, String subscriptionTier) {
        log.debug("Updating subscription tier for user: {} to: {}", userId, subscriptionTier);
        
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new StrategizException(ProfileErrors.PROFILE_NOT_FOUND, "User not found: " + userId);
        }
        
        UserEntity user = userOpt.get();
        UserProfileEntity profile = user.getProfile();
        
        if (profile == null) {
            throw new StrategizException(ProfileErrors.PROFILE_NOT_FOUND, "Profile not found for user: " + userId);
        }
        
        profile.setSubscriptionTier(subscriptionTier);
        
        // Save the user
        UserEntity savedUser = userRepository.save(user);
        
        log.info("Subscription tier updated successfully for user: {} to: {}", userId, subscriptionTier);
        return UpdateProfileResponse.success(userId, "Subscription tier updated successfully");
    }

    /**
     * Updates the trading mode for a user
     */
    public UpdateProfileResponse updateTradingMode(String userId, String tradingMode) {
        log.debug("Updating trading mode for user: {} to: {}", userId, tradingMode);
        
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new StrategizException(ProfileErrors.PROFILE_NOT_FOUND, "User not found: " + userId);
        }
        
        UserEntity user = userOpt.get();
        UserProfileEntity profile = user.getProfile();
        
        if (profile == null) {
            throw new StrategizException(ProfileErrors.PROFILE_NOT_FOUND, "Profile not found for user: " + userId);
        }
        
        profile.setTradingMode(tradingMode);
        
        // Save the user
        UserEntity savedUser = userRepository.save(user);
        
        log.info("Trading mode updated successfully for user: {} to: {}", userId, tradingMode);
        return UpdateProfileResponse.success(userId, "Trading mode updated successfully");
    }

    /**
     * Verifies user's email address
     */
    public ReadProfileResponse verifyEmail(String userId, String verificationCode) {
        log.debug("Verifying email for user: {} with code: {}", userId, verificationCode);
        
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new StrategizException(ProfileErrors.PROFILE_NOT_FOUND, "User not found: " + userId);
        }
        
        UserEntity user = userOpt.get();
        UserProfileEntity profile = user.getProfile();
        
        if (profile == null) {
            throw new StrategizException(ProfileErrors.PROFILE_NOT_FOUND, "Profile not found for user: " + userId);
        }
        
        // TODO: Implement actual verification logic
        // For now, just mark as verified
        profile.setVerifiedEmail(true);
        
        // Save the user
        UserEntity savedUser = userRepository.save(user);
        
        log.info("Email verified successfully for user: {}", userId);
        return ReadProfileResponse.fromEntity(savedUser);
    }

    /**
     * Deactivates a user profile
     */
    public void deactivateProfile(String userId) {
        log.debug("Deactivating profile for user: {}", userId);
        
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new StrategizException(ProfileErrors.PROFILE_NOT_FOUND, "User not found: " + userId);
        }
        
        UserEntity user = userOpt.get();
        UserProfileEntity profile = user.getProfile();
        
        if (profile == null) {
            throw new StrategizException(ProfileErrors.PROFILE_NOT_FOUND, "Profile not found for user: " + userId);
        }
        
        profile.setIsActive(false);
        userRepository.save(user);
        
        log.info("Profile deactivated successfully for user: {}", userId);
    }

    /**
     * Deletes a user profile
     */
    public void deleteProfile(String userId) {
        log.debug("Deleting profile for user: {}", userId);
        
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.warn("User not found: {}", userId);
            return;
        }
        
        UserEntity user = userOpt.get();
        if (user.getProfile() != null) {
            user.setProfile(null);
            userRepository.save(user);
            log.info("Profile deleted successfully for user: {}", userId);
        } else {
            log.warn("No profile found to delete for user: {}", userId);
        }
    }

    /**
     * Validates if a user exists
     */
    public boolean userExists(String userId) {
        return userRepository.existsById(userId);
    }

    /**
     * Validates if a user exists by email
     */
    public boolean userExistsByEmail(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    /**
     * Validates if a profile exists for a user
     */
    public boolean profileExists(String userId) {
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return false;
        }
        
        UserEntity user = userOpt.get();
        return user.getProfile() != null;
    }

    /**
     * Helper method to validate user existence and get the user
     */
    private UserEntity validateAndGetUser(String userId) {
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new StrategizException(ProfileErrors.PROFILE_NOT_FOUND, "User not found: " + userId);
        }
        return userOpt.get();
    }

    /**
     * Helper method to validate profile existence and get the profile
     */
    private UserProfileEntity validateAndGetProfile(UserEntity user) {
        UserProfileEntity profile = user.getProfile();
        if (profile == null) {
            throw new StrategizException(ProfileErrors.PROFILE_NOT_FOUND, "Profile not found for user: " + user.getId());
        }
        return profile;
    }
}
