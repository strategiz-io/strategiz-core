package io.strategiz.data.social.entity;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.base.entity.BaseEntity;
import io.strategiz.data.base.annotation.Collection;
import jakarta.validation.constraints.NotBlank;

/**
 * User follow relationship entity - represents one user following another.
 *
 * Collection: user_follows (top-level) Document ID: compound key
 * {followerId}_{followingId}
 *
 * This is a one-way follow (like Twitter), not mutual friendship. - followerId: the user
 * who is doing the following - followingId: the user being followed
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Collection("user_follows")
public class UserFollowEntity extends BaseEntity {

	@DocumentId
	@PropertyName("id")
	@JsonProperty("id")
	private String id;

	@PropertyName("followerId")
	@JsonProperty("followerId")
	@NotBlank(message = "Follower ID is required")
	private String followerId;

	@PropertyName("followingId")
	@JsonProperty("followingId")
	@NotBlank(message = "Following ID is required")
	private String followingId;

	@PropertyName("followedAt")
	@JsonProperty("followedAt")
	private Timestamp followedAt;

	// Denormalized fields for efficient display (avoid extra lookups)
	@PropertyName("followerName")
	@JsonProperty("followerName")
	private String followerName;

	@PropertyName("followerPhotoURL")
	@JsonProperty("followerPhotoURL")
	private String followerPhotoURL;

	@PropertyName("followingName")
	@JsonProperty("followingName")
	private String followingName;

	@PropertyName("followingPhotoURL")
	@JsonProperty("followingPhotoURL")
	private String followingPhotoURL;

	// Constructors
	public UserFollowEntity() {
		super();
	}

	public UserFollowEntity(String followerId, String followingId) {
		super();
		this.followerId = followerId;
		this.followingId = followingId;
		this.id = generateId(followerId, followingId);
		this.followedAt = Timestamp.now();
	}

	/**
	 * Generates compound document ID from follower and following IDs. Format:
	 * {followerId}_{followingId}
	 */
	public static String generateId(String followerId, String followingId) {
		return followerId + "_" + followingId;
	}

	// Getters and Setters
	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	public String getFollowerId() {
		return followerId;
	}

	public void setFollowerId(String followerId) {
		this.followerId = followerId;
	}

	public String getFollowingId() {
		return followingId;
	}

	public void setFollowingId(String followingId) {
		this.followingId = followingId;
	}

	public Timestamp getFollowedAt() {
		return followedAt;
	}

	public void setFollowedAt(Timestamp followedAt) {
		this.followedAt = followedAt;
	}

	public String getFollowerName() {
		return followerName;
	}

	public void setFollowerName(String followerName) {
		this.followerName = followerName;
	}

	public String getFollowerPhotoURL() {
		return followerPhotoURL;
	}

	public void setFollowerPhotoURL(String followerPhotoURL) {
		this.followerPhotoURL = followerPhotoURL;
	}

	public String getFollowingName() {
		return followingName;
	}

	public void setFollowingName(String followingName) {
		this.followingName = followingName;
	}

	public String getFollowingPhotoURL() {
		return followingPhotoURL;
	}

	public void setFollowingPhotoURL(String followingPhotoURL) {
		this.followingPhotoURL = followingPhotoURL;
	}

	@Override
	public String toString() {
		return "UserFollowEntity{" + "id='" + id + '\'' + ", followerId='" + followerId + '\'' + ", followingId='"
				+ followingId + '\'' + ", followedAt=" + followedAt + ", isActive=" + getIsActive() + '}';
	}

}
