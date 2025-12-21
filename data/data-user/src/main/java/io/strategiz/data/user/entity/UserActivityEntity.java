package io.strategiz.data.user.entity;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.base.entity.BaseEntity;
import io.strategiz.data.base.annotation.Collection;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

/**
 * User activity entity - represents an activity performed by a user.
 *
 * Collection: user_activities (top-level)
 *
 * Used for building activity feeds - followers can see activities from users they follow.
 * Activities are created when users perform notable actions like publishing strategies,
 * executing trades, reaching milestones, etc.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Collection("user_activities")
public class UserActivityEntity extends BaseEntity {

    @DocumentId
    @PropertyName("id")
    @JsonProperty("id")
    private String id;

    @PropertyName("userId")
    @JsonProperty("userId")
    @NotBlank(message = "User ID is required")
    private String userId;

    @PropertyName("activityType")
    @JsonProperty("activityType")
    @NotBlank(message = "Activity type is required")
    private String activityType;

    @PropertyName("title")
    @JsonProperty("title")
    private String title;

    @PropertyName("description")
    @JsonProperty("description")
    private String description;

    @PropertyName("resourceType")
    @JsonProperty("resourceType")
    private String resourceType;

    @PropertyName("resourceId")
    @JsonProperty("resourceId")
    private String resourceId;

    @PropertyName("metadata")
    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    @PropertyName("activityDate")
    @JsonProperty("activityDate")
    private Timestamp activityDate;

    @PropertyName("visibility")
    @JsonProperty("visibility")
    private String visibility = "PUBLIC";

    // Denormalized user info for display
    @PropertyName("userName")
    @JsonProperty("userName")
    private String userName;

    @PropertyName("userPhotoURL")
    @JsonProperty("userPhotoURL")
    private String userPhotoURL;

    // Constructors
    public UserActivityEntity() {
        super();
    }

    public UserActivityEntity(String userId, String activityType, String title) {
        super();
        this.userId = userId;
        this.activityType = activityType;
        this.title = title;
        this.activityDate = Timestamp.now();
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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public Timestamp getActivityDate() {
        return activityDate;
    }

    public void setActivityDate(Timestamp activityDate) {
        this.activityDate = activityDate;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserPhotoURL() {
        return userPhotoURL;
    }

    public void setUserPhotoURL(String userPhotoURL) {
        this.userPhotoURL = userPhotoURL;
    }

    @Override
    public String toString() {
        return "UserActivityEntity{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", activityType='" + activityType + '\'' +
                ", title='" + title + '\'' +
                ", activityDate=" + activityDate +
                ", visibility='" + visibility + '\'' +
                ", isActive=" + getIsActive() +
                '}';
    }
}
