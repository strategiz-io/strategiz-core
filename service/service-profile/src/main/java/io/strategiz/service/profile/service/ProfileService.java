package io.strategiz.service.profile.service;

import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.data.user.entity.UserProfileEntity;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.base.BaseService;
import io.strategiz.service.profile.constants.ProfileConstants;
import io.strategiz.service.profile.exception.ProfileErrors;
import io.strategiz.service.profile.model.CreateProfileRequest;
import io.strategiz.service.profile.model.ProfileResponse;
import io.strategiz.service.profile.model.ReadProfileResponse;
import io.strategiz.service.profile.model.UpdateProfileRequest;
import io.strategiz.service.profile.model.UpdateProfileResponse;
import io.strategiz.service.profile.model.UpdateDemoModeResponse;
import io.strategiz.business.tokenauth.SessionAuthBusiness;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service for managing user profiles
 */
@Service
public class ProfileService extends BaseService {

	@Override
	protected String getModuleName() {
		return "service-profile";
	}

	private final UserRepository userRepository;

	private final SessionAuthBusiness sessionAuthBusiness;

	public ProfileService(UserRepository userRepository, SessionAuthBusiness sessionAuthBusiness) {
		this.userRepository = userRepository;
		this.sessionAuthBusiness = sessionAuthBusiness;
	}

	/**
	 * Creates a new user profile (used by regular ProfileController)
	 */
	public ReadProfileResponse createProfile(UpdateProfileRequest request) {
		log.debug("Creating profile for user with email: {}", request.getEmail());

		// Check if user already exists
		Optional<UserEntity> existingUser = userRepository.findByEmail(request.getEmail());
		if (existingUser.isPresent()) {
			throw new StrategizException(ProfileErrors.PROFILE_ALREADY_EXISTS,
					ProfileConstants.ErrorMessages.USER_ALREADY_EXISTS + request.getEmail());
		}

		// Create new user with profile
		UserEntity user = new UserEntity();

		UserProfileEntity profile = new UserProfileEntity();
		profile.setName(request.getName());
		profile.setEmail(request.getEmail());
		profile.setPhotoURL(request.getPhotoURL());
		profile.setSubscriptionTier(request.getSubscriptionTier() != null ? request.getSubscriptionTier()
				: ProfileConstants.Defaults.SUBSCRIPTION_TIER);
		profile
			.setDemoMode(request.getDemoMode() != null ? request.getDemoMode() : ProfileConstants.Defaults.DEMO_MODE);
		profile.setIsEmailVerified(ProfileConstants.Defaults.EMAIL_VERIFIED);
		profile.setBio(request.getBio());
		profile.setLocation(request.getLocation());
		profile.setOccupation(request.getOccupation());
		profile.setEducation(request.getEducation());

		user.setProfile(profile);

		// Save the user (which will cascade to profile)
		UserEntity savedUser = userRepository.save(user);

		log.info(ProfileConstants.LogMessages.PROFILE_CREATED + "{}", savedUser.getId());
		return ReadProfileResponse.fromEntity(savedUser);
	}

	/**
	 * Gets a user profile by user ID
	 */
	public ReadProfileResponse getProfile(String userId) {
		log.info("=== ProfileService.getProfile START ===");
		log.info("Getting profile for userId: [{}]", userId);

		Optional<UserEntity> userOpt = userRepository.findById(userId);
		if (userOpt.isEmpty()) {
			log.error("User not found for userId: [{}]", userId);
			throw new StrategizException(ProfileErrors.PROFILE_NOT_FOUND,
					ProfileConstants.ErrorMessages.USER_NOT_FOUND + userId);
		}

		UserEntity user = userOpt.get();
		log.info("Found user with ID: [{}]", user.getId());

		if (user.getProfile() == null) {
			log.error("Profile is NULL for userId: [{}]", userId);
			throw new StrategizException(ProfileErrors.PROFILE_NOT_FOUND,
					ProfileConstants.ErrorMessages.PROFILE_NOT_FOUND + userId);
		}

		// Debug: Log profile fields
		log.info("Profile name: [{}]", user.getProfile().getName());
		log.info("Profile email: [{}]", user.getProfile().getEmail());
		log.info("Profile photoURL: [{}]", user.getProfile().getPhotoURL());
		log.info("Profile demoMode: [{}]", user.getProfile().getDemoMode());

		ReadProfileResponse response = ReadProfileResponse.fromEntity(user);
		log.info("Response name: [{}]", response.getName());
		log.info("=== ProfileService.getProfile END ===");

		return response;
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
			throw new StrategizException(ProfileErrors.PROFILE_NOT_FOUND,
					ProfileConstants.ErrorMessages.USER_NOT_FOUND + userId);
		}

		UserEntity user = userOpt.get();
		UserProfileEntity profile = user.getProfile();

		if (profile == null) {
			throw new StrategizException(ProfileErrors.PROFILE_NOT_FOUND,
					ProfileConstants.ErrorMessages.PROFILE_NOT_FOUND + userId);
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
		if (request.getDemoMode() != null) {
			profile.setDemoMode(request.getDemoMode());
		}
		if (request.getBio() != null) {
			profile.setBio(request.getBio());
		}
		if (request.getLocation() != null) {
			profile.setLocation(request.getLocation());
		}
		if (request.getOccupation() != null) {
			profile.setOccupation(request.getOccupation());
		}
		if (request.getEducation() != null) {
			profile.setEducation(request.getEducation());
		}

		// Save the user
		UserEntity savedUser = userRepository.save(user);

		log.info(ProfileConstants.LogMessages.PROFILE_UPDATED + "{}", userId);
		return ReadProfileResponse.fromEntity(savedUser);
	}

	/**
	 * Updates the subscription tier for a user
	 */
	public UpdateProfileResponse updateSubscriptionTier(String userId, String subscriptionTier) {
		log.debug("Updating subscription tier for user: {} to: {}", userId, subscriptionTier);

		Optional<UserEntity> userOpt = userRepository.findById(userId);
		if (userOpt.isEmpty()) {
			throw new StrategizException(ProfileErrors.PROFILE_NOT_FOUND,
					ProfileConstants.ErrorMessages.USER_NOT_FOUND + userId);
		}

		UserEntity user = userOpt.get();
		UserProfileEntity profile = user.getProfile();

		if (profile == null) {
			throw new StrategizException(ProfileErrors.PROFILE_NOT_FOUND,
					ProfileConstants.ErrorMessages.PROFILE_NOT_FOUND + userId);
		}

		profile.setSubscriptionTier(subscriptionTier);

		// Save the user
		UserEntity savedUser = userRepository.save(user);

		log.info(ProfileConstants.LogMessages.SUBSCRIPTION_UPDATED, userId, subscriptionTier);
		return UpdateProfileResponse.success(userId, "Subscription tier updated successfully");
	}

	/**
	 * Sets demo mode for a user without generating new tokens. Used by provider callbacks
	 * when connecting real data providers.
	 * @param userId the user ID
	 * @param demoMode the new demo mode value
	 */
	public void setDemoMode(String userId, boolean demoMode) {
		log.debug("Setting demo mode for user: {} to: {}", userId, demoMode);

		Optional<UserEntity> userOpt = userRepository.findById(userId);
		if (userOpt.isEmpty()) {
			log.warn("User not found when setting demo mode: {}", userId);
			return; // Don't throw - provider callback should not fail due to this
		}

		UserEntity user = userOpt.get();
		UserProfileEntity profile = user.getProfile();

		if (profile == null) {
			log.warn("Profile not found when setting demo mode for user: {}", userId);
			return;
		}

		profile.setDemoMode(demoMode);
		userRepository.save(user);

		log.info("Demo mode set to {} for user: {}", demoMode, userId);
	}

	/**
	 * Updates the demo mode for a user and returns new JWT tokens
	 */
	public UpdateDemoModeResponse updateDemoMode(String userId, Boolean demoMode) {
		log.debug("Updating demo mode for user: {} to: {}", userId, demoMode);

		Optional<UserEntity> userOpt = userRepository.findById(userId);
		if (userOpt.isEmpty()) {
			throw new StrategizException(ProfileErrors.PROFILE_NOT_FOUND,
					ProfileConstants.ErrorMessages.USER_NOT_FOUND + userId);
		}

		UserEntity user = userOpt.get();
		UserProfileEntity profile = user.getProfile();

		if (profile == null) {
			throw new StrategizException(ProfileErrors.PROFILE_NOT_FOUND,
					ProfileConstants.ErrorMessages.PROFILE_NOT_FOUND + userId);
		}

		// Update and save the demo mode
		profile.setDemoMode(demoMode);
		UserEntity savedUser = userRepository.save(user);

		// Generate new tokens with updated demo mode
		SessionAuthBusiness.AuthRequest authRequest = new SessionAuthBusiness.AuthRequest(userId, profile.getEmail(),
				java.util.List.of("profile_update"), // Pseudo auth method
				false, // Not partial auth
				null, // No device ID for profile update
				null, // No fingerprint
				null, // No IP address
				"Profile Service", demoMode // New demo mode
		);

		SessionAuthBusiness.AuthResult authResult = sessionAuthBusiness.createAuthentication(authRequest);

		log.info(ProfileConstants.LogMessages.DEMO_MODE_UPDATED, userId, demoMode);

		return new UpdateDemoModeResponse(demoMode, authResult.accessToken(), authResult.refreshToken());
	}

	/**
	 * Verifies user's email address
	 */
	public ReadProfileResponse verifyEmail(String userId, String verificationCode) {
		log.debug("Verifying email for user: {} with code: {}", userId, verificationCode);

		Optional<UserEntity> userOpt = userRepository.findById(userId);
		if (userOpt.isEmpty()) {
			throw new StrategizException(ProfileErrors.PROFILE_NOT_FOUND,
					ProfileConstants.ErrorMessages.USER_NOT_FOUND + userId);
		}

		UserEntity user = userOpt.get();
		UserProfileEntity profile = user.getProfile();

		if (profile == null) {
			throw new StrategizException(ProfileErrors.PROFILE_NOT_FOUND,
					ProfileConstants.ErrorMessages.PROFILE_NOT_FOUND + userId);
		}

		// TODO: Implement actual verification logic
		// For now, just mark as verified
		profile.setIsEmailVerified(!ProfileConstants.Defaults.EMAIL_VERIFIED);

		// Save the user
		UserEntity savedUser = userRepository.save(user);

		log.info(ProfileConstants.LogMessages.EMAIL_VERIFIED + "{}", userId);
		return ReadProfileResponse.fromEntity(savedUser);
	}

	/**
	 * Deactivates a user profile
	 */
	public void deactivateProfile(String userId) {
		log.debug("Deactivating profile for user: {}", userId);

		Optional<UserEntity> userOpt = userRepository.findById(userId);
		if (userOpt.isEmpty()) {
			throw new StrategizException(ProfileErrors.PROFILE_NOT_FOUND,
					ProfileConstants.ErrorMessages.USER_NOT_FOUND + userId);
		}

		UserEntity user = userOpt.get();
		UserProfileEntity profile = user.getProfile();

		if (profile == null) {
			throw new StrategizException(ProfileErrors.PROFILE_NOT_FOUND,
					ProfileConstants.ErrorMessages.PROFILE_NOT_FOUND + userId);
		}

		// Use soft delete on the user entity (BaseEntity)
		userRepository.deleteUser(userId);

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
			log.info(ProfileConstants.LogMessages.PROFILE_DELETED + "{}", userId);
		}
		else {
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
			throw new StrategizException(ProfileErrors.PROFILE_NOT_FOUND,
					ProfileConstants.ErrorMessages.USER_NOT_FOUND + userId);
		}
		return userOpt.get();
	}

	/**
	 * Helper method to validate profile existence and get the profile
	 */
	private UserProfileEntity validateAndGetProfile(UserEntity user) {
		UserProfileEntity profile = user.getProfile();
		if (profile == null) {
			throw new StrategizException(ProfileErrors.PROFILE_NOT_FOUND,
					"Profile not found for user: " + user.getId());
		}
		return profile;
	}

}
