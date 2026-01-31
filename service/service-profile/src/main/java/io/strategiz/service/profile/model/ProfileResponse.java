package io.strategiz.service.profile.model;

import io.strategiz.service.profile.constants.ProfileConstants;

/**
 * Response DTO for user profile data - used as service return type
 */
public class ProfileResponse {

	private String userId;

	private String name;

	private String email;

	private String photoURL;

	private boolean isEmailVerified;

	private String subscriptionTier;

	private Boolean demoMode;

	private boolean isActive;

	private long createdAt;

	private long modifiedAt;

	// Default constructor
	public ProfileResponse() {
	}

	// Full constructor
	public ProfileResponse(String userId, String name, String email, String photoURL, boolean isEmailVerified,
			String subscriptionTier, Boolean demoMode, boolean isActive, long createdAt, long modifiedAt) {
		this.userId = userId;
		this.name = name;
		this.email = email;
		this.photoURL = photoURL;
		this.isEmailVerified = isEmailVerified;
		this.subscriptionTier = subscriptionTier;
		this.demoMode = demoMode;
		this.isActive = isActive;
		this.createdAt = createdAt;
		this.modifiedAt = modifiedAt;
	}

	// Getters and setters
	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhotoURL() {
		return photoURL;
	}

	public void setPhotoURL(String photoURL) {
		this.photoURL = photoURL;
	}

	public boolean isEmailVerified() {
		return isEmailVerified;
	}

	public void setEmailVerified(boolean isEmailVerified) {
		this.isEmailVerified = isEmailVerified;
	}

	public String getSubscriptionTier() {
		return subscriptionTier;
	}

	public void setSubscriptionTier(String subscriptionTier) {
		this.subscriptionTier = subscriptionTier;
	}

	public Boolean getDemoMode() {
		return demoMode;
	}

	public void setDemoMode(Boolean demoMode) {
		this.demoMode = demoMode;
	}

	public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean active) {
		isActive = active;
	}

	public long getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(long createdAt) {
		this.createdAt = createdAt;
	}

	public long getModifiedAt() {
		return modifiedAt;
	}

	public void setModifiedAt(long modifiedAt) {
		this.modifiedAt = modifiedAt;
	}

	@Override
	public String toString() {
		return "ProfileResponse{" + "userId='" + userId + '\'' + ", name='" + name + '\'' + ", email='" + email + '\''
				+ ", photoURL='" + photoURL + '\'' + ", isEmailVerified=" + isEmailVerified + ", subscriptionTier='"
				+ subscriptionTier + '\'' + ", demoMode=" + demoMode + ", isActive=" + isActive + ", createdAt="
				+ createdAt + ", modifiedAt=" + modifiedAt + '}';
	}

	/**
	 * Creates a ProfileResponse from a UserEntity
	 */
	public static ProfileResponse fromEntity(io.strategiz.data.user.entity.UserEntity user) {
		if (user == null || user.getProfile() == null) {
			return null;
		}

		io.strategiz.data.user.entity.UserProfileEntity profile = user.getProfile();

		return new ProfileResponse(user.getId(), profile.getName(), profile.getEmail(), profile.getPhotoURL(),
				profile.getIsEmailVerified() != null ? profile.getIsEmailVerified()
						: ProfileConstants.Defaults.EMAIL_VERIFIED,
				profile.getSubscriptionTier(), profile.getDemoMode(),
				user.getIsActive() != null ? user.getIsActive() : ProfileConstants.Defaults.IS_ACTIVE,
				System.currentTimeMillis(), // Default to current time
				System.currentTimeMillis() // Default to current time
		);
	}

}