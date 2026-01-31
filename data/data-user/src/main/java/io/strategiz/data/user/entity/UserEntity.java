package io.strategiz.data.user.entity;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.base.entity.BaseEntity;
import io.strategiz.data.base.annotation.Collection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;

import java.util.Objects;

/**
 * User document entity - root document in users/{userId} collection Contains core user
 * identity and profile information
 *
 * Subcollections (handled by other modules): - auth-methods: Authentication methods
 * (data-auth module) - watchlist: User's market watchlist items (data-watchlist module) -
 * providers: Connected trading providers (data-provider module) - devices: User devices
 * (data-device module) - preferences: User preferences and settings (data-preferences
 * module)
 */
@Collection("users")
public class UserEntity extends BaseEntity {

	@DocumentId
	@PropertyName("userId")
	@JsonProperty("userId")
	@NotBlank(message = "User ID is required")
	private String userId;

	@PropertyName("profile")
	@JsonProperty("profile")
	@NotNull(message = "User profile is required")
	@Valid
	private UserProfileEntity profile;

	// Constructors
	public UserEntity() {
		super();
	}

	/**
	 * Creates a new user with the minimum required fields
	 */
	public UserEntity(String userId, String name, String email) {
		super();
		this.userId = userId;
		this.profile = new UserProfileEntity(name, email);
	}

	public UserEntity(String userId, UserProfileEntity profile) {
		super();
		this.userId = userId;
		this.profile = profile;
	}

	// Getters and Setters
	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public UserProfileEntity getProfile() {
		return profile;
	}

	public void setProfile(UserProfileEntity profile) {
		this.profile = profile;
	}

	@Override
	public String getId() {
		return userId;
	}

	@Override
	public void setId(String id) {
		this.userId = id;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		if (!super.equals(o))
			return false;
		UserEntity user = (UserEntity) o;
		return Objects.equals(userId, user.userId) && Objects.equals(profile, user.profile);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), userId, profile);
	}

	@Override
	public String toString() {
		return "UserEntity{" + "userId='" + userId + '\'' + ", profile=" + profile + ", isActive=" + getIsActive()
				+ '}';
	}

}
